import { useState } from 'react';

import type { AttractionSummary, OperatorAttraction } from '@/features/attractions/domain/attraction';
import type { AuthSession } from '@/auth/session';
import {
  availableMaintenanceActions,
  completionAuthorizationHint,
  inspectionAuthorizationHint,
  primaryMaintenanceAction,
  resultingStatus,
  type MaintenanceAction,
} from '@/features/maintenance/domain/maintenanceActions';
import {
  formatWorkOrderStatus,
} from '@/features/maintenance/domain/maintenancePresentation';
import type { MaintenanceWorkOrder, MaintenanceWorkOrderCommand } from '@/features/maintenance/domain/maintenance';
import { HelpHeading } from '@/shared/ui/HelpLink';
import { Button } from '@/shared/ui/Button';
import { MaintenanceCommandDialog } from '@/features/maintenance/components/MaintenanceCommandDialog';
import {
  DuplicateCommandError,
  ForbiddenError,
  MaintenancePrerequisiteError,
  VersionConflictError,
} from '@/shared/api/errors';

import styles from './WorkOrderActionPanel.module.css';

function nextStatusLabel(status: MaintenanceWorkOrder['status'], type: MaintenanceWorkOrderCommand) {
  const next = resultingStatus(status, type);
  return next ? formatWorkOrderStatus(next) : null;
}

export type WorkOrderCommandDraft = {
  type: MaintenanceWorkOrderCommand;
  expectedVersion: number;
  reason?: string;
  data?: Record<string, unknown>;
};

export function WorkOrderActionPanel({
  workOrder,
  session,
  attraction,
  pending,
  staleSinceOpen,
  error = null,
  onCommand,
}: {
  workOrder: MaintenanceWorkOrder;
  session: AuthSession | null;
  attraction?: AttractionSummary | OperatorAttraction | null;
  pending: boolean;
  staleSinceOpen?: boolean;
  error?: Error | null;
  onCommand: (input: WorkOrderCommandDraft) => void | Promise<unknown>;
}) {
  const actions = availableMaintenanceActions({ workOrder, session, attraction });
  const primary = primaryMaintenanceAction(actions);
  const inspectHint = inspectionAuthorizationHint(workOrder, session);
  const completeHint = completionAuthorizationHint(workOrder, session);
  const [active, setActive] = useState<MaintenanceAction | null>(null);
  const [reason, setReason] = useState('');
  const [teamId, setTeamId] = useState(workOrder.assignedTeam ?? 'Ride Systems');
  const [actorSubject, setActorSubject] = useState(workOrder.assignedActorSubject ?? '');
  const [openedVersion, setOpenedVersion] = useState<number | null>(null);
  const stale = Boolean(active) && (Boolean(staleSinceOpen) || (openedVersion !== null && workOrder.version !== openedVersion));

  const lifecycle = actions.filter(
    (action) => action.kind !== 'supporting' && action.type !== 'RECORD_CHECKLIST_RESULT',
  );

  async function submitActive() {
    if (!active || active.disabledReason) {
      return;
    }
    const data: Record<string, unknown> = {};
    if (active.type === 'ASSIGN' || active.type === 'REASSIGN') {
      data.teamId = teamId.trim();
      data.actorSubject = actorSubject.trim();
    }
    try {
      await onCommand({
        type: active.type,
        expectedVersion: openedVersion ?? workOrder.version,
        reason: reason.trim() || undefined,
        data: Object.keys(data).length ? data : undefined,
      });
    } catch {
      return;
    }
    setActive(null);
    setOpenedVersion(null);
    setReason('');
  }

  return (
    <section className={styles.panel} aria-labelledby="work-order-actions-heading">
      <HelpHeading
        id="work-order-actions-heading"
        article="maintenance"
        section={workOrder.status === 'AWAITING_INSPECTION' ? 'inspection' : 'lifecycle'}
        label="Help: maintenance commands"
      >
        Commands
      </HelpHeading>
      {inspectHint ? <p>{inspectHint}</p> : null}
      {completeHint ? <p>{completeHint}</p> : null}
      {lifecycle.length === 0 ? (
        <p className={styles.muted}>No lifecycle commands are available for this work order and role.</p>
      ) : null}
      <div className={styles.actions}>
        {lifecycle.map((action) => (
          <Button
            key={action.type}
            id={action.type === primary?.type ? 'primary-maintenance-action' : undefined}
            variant={action.kind === 'destructive' ? 'danger' : 'default'}
            disabled={pending || Boolean(action.disabledReason)}
            aria-pressed={active?.type === action.type}
            title={action.disabledReason}
            onClick={() => {
              setActive(action);
              setOpenedVersion(workOrder.version);
              setReason('');
            }}
          >
            {action.label}
          </Button>
        ))}
      </div>
      {lifecycle
        .filter((action) => action.disabledReason)
        .map((action) => (
          <p key={`${action.type}-blocked`} className={styles.muted}>
            {action.disabledReason}
          </p>
        ))}
      {active ? (
        <MaintenanceCommandDialog
          title={active.label}
          description={`${active.label} for ${workOrder.workOrderNumber}.`}
          currentState={formatWorkOrderStatus(workOrder.status)}
          resultingState={nextStatusLabel(workOrder.status, active.type)}
          attractionName={attraction?.name}
          warning={
            active.type === 'COMPLETE'
              ? 'Maintenance never reopens an attraction. Operations owns testing and return-to-service.'
              : active.kind === 'destructive'
                ? 'This action is recorded against the work-order audit trail.'
                : null
          }
          stale={stale}
          pending={pending}
          error={commandErrorCopy(error)}
          submitLabel={primary?.type === active.type ? active.label : `Confirm ${active.label.toLowerCase()}`}
          onClose={() => {
            setActive(null);
            setOpenedVersion(null);
            setReason('');
          }}
          onSubmit={() => {
            void submitActive();
          }}
        >
          {active.type === 'ASSIGN' || active.type === 'REASSIGN' ? (
            <>
              <label>
                Assigned team
                <input value={teamId} onChange={(event) => setTeamId(event.target.value)} required />
              </label>
              <label>
                Technician or operator subject
                <input
                  value={actorSubject}
                  onChange={(event) => setActorSubject(event.target.value)}
                />
              </label>
            </>
          ) : null}
          {active.requiresReason ? (
            <label>
              Reason
              <textarea required value={reason} onChange={(event) => setReason(event.target.value)} />
            </label>
          ) : (
            <label>
              Reason (optional)
              <textarea value={reason} onChange={(event) => setReason(event.target.value)} />
            </label>
          )}
        </MaintenanceCommandDialog>
      ) : null}
    </section>
  );
}

function commandErrorCopy(error: Error | null): string | null {
  if (!error) {
    return null;
  }
  if (error instanceof ForbiddenError) {
    return 'That action needs a supervisor approval or a missing maintenance scope. Form entries were kept.';
  }
  if (error instanceof VersionConflictError) {
    return 'This work order changed while you were reviewing it. Close this dialog and reload current state before submitting.';
  }
  if (error instanceof DuplicateCommandError) {
    return 'That command identifier belongs to another resource. Retry as a new action if you still want the change.';
  }
  if (error instanceof MaintenancePrerequisiteError) {
    return error.message;
  }
  return error.message;
}
