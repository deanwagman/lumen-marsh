import { Link, useNavigate, useParams } from 'react-router-dom';
import { useState, type FormEvent } from 'react';

import { useAuth } from '@/auth/AuthContext';
import { useAttractionCommand } from '@/features/attractions/hooks/useAttractionCommand';
import { useAttractionDetail } from '@/features/attractions/hooks/useAttractionDetail';
import { useAttractions } from '@/features/attractions/hooks/useAttractions';
import { formatLabel } from '@/features/attractions/domain/formatters';
import { AttractionHandoffCard } from '@/features/maintenance/components/AttractionHandoffCard';
import { MaintenancePriorityBadge } from '@/features/maintenance/components/MaintenancePriorityBadge';
import { WorkOrderActionPanel } from '@/features/maintenance/components/WorkOrderActionPanel';
import { WorkOrderChecklist } from '@/features/maintenance/components/WorkOrderChecklist';
import { WorkOrderStatusBadge } from '@/features/maintenance/components/WorkOrderStatusBadge';
import { WorkOrderTimeline } from '@/features/maintenance/components/WorkOrderTimeline';
import {
  canCommandMaintenance,
  canReadMaintenance,
  type MaintenanceWorkOrderCommand,
} from '@/features/maintenance/domain/maintenance';
import { nextActionLabel, primaryMaintenanceAction, availableMaintenanceActions } from '@/features/maintenance/domain/maintenanceActions';
import {
  assignmentLabel,
  datetimeLocalValue,
  formatEstimatedRestore,
  formatMaintenanceTime,
  fromDatetimeLocal,
  isSafeExternalUri,
} from '@/features/maintenance/domain/maintenancePresentation';
import { useMaintenanceWorkOrderCommand } from '@/features/maintenance/hooks/useMaintenanceCommand';
import {
  useMaintenanceActivity,
  useMaintenanceWorkOrder,
} from '@/features/maintenance/hooks/useMaintenanceWorkOrder';
import {
  DuplicateCommandError,
  ForbiddenError,
  MaintenancePrerequisiteError,
  NotFoundError,
  VersionConflictError,
} from '@/shared/api/errors';
import { Button } from '@/shared/ui/Button';

import styles from './MaintenanceWorkOrderPage.module.css';

export default function MaintenanceWorkOrderPage() {
  const { workOrderId } = useParams();
  const { session } = useAuth();
  const navigate = useNavigate();
  const detail = useMaintenanceWorkOrder(workOrderId);
  const activity = useMaintenanceActivity(workOrderId);
  const command = useMaintenanceWorkOrderCommand();
  const attractions = useAttractions();
  const attractionDetail = useAttractionDetail(detail.data?.attractionId);
  const attractionCommand = useAttractionCommand(detail.data?.attractionId ?? '');
  const [note, setNote] = useState('');
  const [evidence, setEvidence] = useState({ label: '', contentType: 'text/uri-list', uri: '' });
  const [restoreAt, setRestoreAt] = useState('');
  const [incidentId, setIncidentId] = useState('');
  const workOrder = detail.data;

  const attraction =
    attractionDetail.data ??
    attractions.data?.find((item) => item.id === workOrder?.attractionId) ??
    null;

  if (!canReadMaintenance(session)) {
    return (
      <section className={styles.page}>
        <h1>Work order</h1>
        <p>This workspace requires the venueops/maintenance.read scope.</p>
      </section>
    );
  }

  function runCommand(input: {
    type: MaintenanceWorkOrderCommand;
    expectedVersion?: number;
    reason?: string;
    data?: Record<string, unknown>;
  }) {
    if (!workOrder) {
      return Promise.reject(new Error('Work order is not loaded.'));
    }
    const request = command
      .mutateAsync({
        workOrderId: workOrder.id,
        type: input.type,
        expectedVersion: input.expectedVersion ?? workOrder.version,
        reason: input.reason,
        data: input.data,
      })
      .then(() => {
        if (input.type === 'ADD_NOTE') {
          setNote('');
        }
        if (input.type === 'ADD_EVIDENCE') {
          setEvidence({ label: '', contentType: 'text/uri-list', uri: '' });
        }
        if (input.type === 'LINK_INCIDENT') {
          setIncidentId('');
        }
      });
    void request.catch(() => {
      // Command errors are rendered from the mutation state.
    });
    return request;
  }

  if (detail.isPending) {
    return (
      <section className={styles.page}>
        <p className={styles.muted} role="status">
          Loading work order
        </p>
      </section>
    );
  }

  if (detail.isError || !workOrder) {
    return (
      <section className={styles.page}>
        <h1>{detail.error instanceof NotFoundError ? 'Work order not found' : 'Unable to load work order'}</h1>
        <p className={styles.muted}>{detail.error?.message}</p>
        {detail.error instanceof NotFoundError ? (
          <Link to="/maintenance">Return to maintenance</Link>
        ) : (
          <Button onClick={() => void detail.refetch()}>Retry</Button>
        )}
      </section>
    );
  }

  const canCommand = canCommandMaintenance(session);
  const actions = availableMaintenanceActions({ workOrder, session, attraction });
  const primary = primaryMaintenanceAction(actions);

  return (
    <section className={styles.page}>
      <button type="button" className={styles.back} onClick={() => navigate(-1)}>
        ← Maintenance
      </button>
      <header className={styles.header}>
        <div>
          <p className={styles.kicker}>Work-order workspace</p>
          <h1 className={styles.mono}>{workOrder.workOrderNumber}</h1>
          <p>{workOrder.summary}</p>
          <p className={styles.muted}>{nextActionLabel(workOrder, attraction)}</p>
          {primary ? (
            <p>
              <Button
                onClick={() => document.getElementById('primary-maintenance-action')?.click()}
                disabled={command.isPending || Boolean(primary.disabledReason)}
              >
                {primary.label}
              </Button>
            </p>
          ) : null}
        </div>
        <div className={styles.badges}>
          <MaintenancePriorityBadge compact priority={workOrder.priority} />
          <WorkOrderStatusBadge status={workOrder.status} />
        </div>
      </header>
      <dl className={styles.facts}>
        <div>
          <dt>Classification</dt>
          <dd>{formatLabel(workOrder.classification)}</dd>
        </div>
        <div>
          <dt>Version</dt>
          <dd>v{workOrder.version}</dd>
        </div>
        <div>
          <dt>Updated</dt>
          <dd>{formatMaintenanceTime(workOrder.updatedAt)}</dd>
        </div>
        <div>
          <dt>Source</dt>
          <dd>
            {formatLabel(workOrder.sourceType)}
            {workOrder.sourceReferenceId ? ` · ${workOrder.sourceReferenceId}` : ''}
          </dd>
        </div>
      </dl>
      <p className={styles.context}>
        <Link to={`/attractions/${workOrder.attractionId}`}>
          {attraction?.name ?? workOrder.attractionId}
        </Link>
        {' · '}
        <Link to={`/maintenance/assets/${workOrder.asset.id}`}>{workOrder.asset.assetCode}</Link>
        {workOrder.incident ? (
          <>
            {' · '}
            <Link to={`/incidents/${workOrder.incident.id}`}>{workOrder.incident.title}</Link>
          </>
        ) : null}
      </p>

      {command.error instanceof VersionConflictError ? (
        <div className={styles.error} role="alert">
          <p>
            This work order changed while you were reviewing it. The latest state has been loaded.
          </p>
          <Button
            onClick={() => {
              command.reset();
              command.beginNewIntent();
              void detail.refetch();
              void activity.refetch();
            }}
            disabled={detail.isFetching}
          >
            {detail.isFetching ? 'Reloading…' : 'Reload current state'}
          </Button>
        </div>
      ) : (
        <p className={styles.srOnly} aria-live="polite">
          {command.isSuccess ? `${workOrder.workOrderNumber} updated to version ${workOrder.version}.` : ''}
        </p>
      )}
      {command.error instanceof ForbiddenError ? (
        <p role="alert">That action needs a supervisor approval or a missing maintenance scope. Form entries were kept.</p>
      ) : null}
      {command.error instanceof MaintenancePrerequisiteError ? (
        <p role="alert">{command.error.message}</p>
      ) : null}
      {command.error instanceof DuplicateCommandError ? (
        <p role="alert">
          That command identifier belongs to another resource. Retry as a new action if you still want
          the change.
        </p>
      ) : null}
      {attractionCommand.error instanceof VersionConflictError ? (
        <div className={styles.error} role="alert">
          <p>The attraction changed while you were reviewing the handoff. Reload current state before submitting.</p>
          <Button
            onClick={() => {
              attractionCommand.reset();
              void attractionDetail.refetch();
            }}
            disabled={attractionDetail.isFetching}
          >
            {attractionDetail.isFetching ? 'Reloading…' : 'Reload current state'}
          </Button>
        </div>
      ) : null}
      {attractionCommand.error instanceof ForbiddenError ? (
        <p role="alert">That attraction command needs attractions.command. Form entries were kept.</p>
      ) : null}

      <AttractionHandoffCard
        workOrder={workOrder}
        session={session}
        attraction={attraction}
        pending={attractionCommand.isPending}
        error={attractionCommand.error}
        onAttractionCommand={(input) => {
          const request = attractionCommand.mutateAsync(input).then(() => {
            void detail.refetch();
            void attractionDetail.refetch();
          });
          void request.catch(() => {
            // Attraction command errors are rendered from the mutation state.
          });
          return request;
        }}
      />

      <div className={styles.workspace}>
        <div className={styles.stack}>
          <WorkOrderActionPanel
            workOrder={workOrder}
            session={session}
            attraction={attraction}
            pending={command.isPending}
            staleSinceOpen={false}
            error={command.error}
            onCommand={runCommand}
          />
          <section className={styles.panel} aria-labelledby="assignment-heading">
            <h2 id="assignment-heading">Assignment</h2>
            <p>{assignmentLabel(workOrder)}</p>
            <p>Estimated restoration {formatEstimatedRestore(workOrder.estimatedRestoreAt)}</p>
            {canCommand ? (
              <form
                className={styles.form}
                onSubmit={(event: FormEvent) => {
                  event.preventDefault();
                  const iso = fromDatetimeLocal(restoreAt || datetimeLocalValue(workOrder.estimatedRestoreAt));
                  runCommand({
                    type: 'SET_ESTIMATED_RESTORE',
                    data: { estimatedRestoreAt: iso },
                  });
                }}
              >
                <label>
                  Estimated restoration time
                  <input
                    type="datetime-local"
                    value={restoreAt || datetimeLocalValue(workOrder.estimatedRestoreAt)}
                    onChange={(event) => setRestoreAt(event.target.value)}
                  />
                </label>
                <Button type="submit" disabled={command.isPending}>
                  Update estimate
                </Button>
              </form>
            ) : null}
          </section>
          <section className={styles.panel} aria-labelledby="description-heading">
            <h2 id="description-heading">Description and evidence</h2>
            <p>{workOrder.description ?? 'No description recorded.'}</p>
            {workOrder.evidence.length === 0 ? <p className={styles.muted}>No evidence recorded.</p> : null}
            <ul className={styles.evidence}>
              {workOrder.evidence.map((entry) => (
                <li key={entry.id}>
                  <p>
                    <strong>{entry.label}</strong> · {entry.contentType}
                  </p>
                  {isSafeExternalUri(entry.uri) ? (
                    <a href={entry.uri} target="_blank" rel="noreferrer noopener">
                      {entry.uri}
                    </a>
                  ) : (
                    <p className={styles.mono}>{entry.uri}</p>
                  )}
                  <p className={styles.muted}>
                    {entry.addedBySubject} · {formatMaintenanceTime(entry.addedAt)}
                  </p>
                </li>
              ))}
            </ul>
            {canCommand ? (
              <form
                className={styles.form}
                onSubmit={(event: FormEvent) => {
                  event.preventDefault();
                  runCommand({
                    type: 'ADD_EVIDENCE',
                    data: evidence,
                  });
                }}
              >
                <label>
                  Evidence label
                  <input
                    required
                    value={evidence.label}
                    onChange={(event) => setEvidence((current) => ({ ...current, label: event.target.value }))}
                  />
                </label>
                <label>
                  Content type
                  <input
                    required
                    value={evidence.contentType}
                    onChange={(event) =>
                      setEvidence((current) => ({ ...current, contentType: event.target.value }))
                    }
                  />
                </label>
                <label>
                  URI
                  <input
                    required
                    value={evidence.uri}
                    onChange={(event) => setEvidence((current) => ({ ...current, uri: event.target.value }))}
                  />
                </label>
                <Button type="submit" disabled={command.isPending}>
                  Add evidence
                </Button>
              </form>
            ) : null}
          </section>
          <section className={styles.panel} aria-labelledby="notes-heading">
            <h2 id="notes-heading">Notes</h2>
            {canCommand ? (
              <form
                className={styles.form}
                onSubmit={(event: FormEvent) => {
                  event.preventDefault();
                  runCommand({
                    type: 'ADD_NOTE',
                    reason: note.trim(),
                    data: { note: note.trim() },
                  });
                }}
              >
                <label>
                  Add a timestamped note
                  <textarea required value={note} onChange={(event) => setNote(event.target.value)} />
                </label>
                <Button type="submit" disabled={command.isPending || note.trim().length === 0}>
                  Add note
                </Button>
              </form>
            ) : (
              <p className={styles.muted}>Notes require maintenance command scope.</p>
            )}
          </section>
          {canCommand && !workOrder.incident ? (
            <section className={styles.panel} aria-labelledby="incident-heading">
              <h2 id="incident-heading">Incident link</h2>
              <form
                className={styles.form}
                onSubmit={(event: FormEvent) => {
                  event.preventDefault();
                  runCommand({ type: 'LINK_INCIDENT', data: { incidentId: incidentId.trim() } });
                }}
              >
                <label>
                  Incident ID
                  <input required value={incidentId} onChange={(event) => setIncidentId(event.target.value)} />
                </label>
                <Button type="submit" disabled={command.isPending}>
                  Link incident
                </Button>
              </form>
            </section>
          ) : null}
          <WorkOrderChecklist
            workOrder={workOrder}
            session={session}
            pending={command.isPending}
            onRecord={(input) =>
              runCommand({
                type: 'RECORD_CHECKLIST_RESULT',
                data: input,
              })
            }
          />
        </div>
        <section aria-labelledby="activity-heading">
          <h2 id="activity-heading">Activity</h2>
          <WorkOrderTimeline
            activity={activity.data}
            isPending={activity.isPending}
            isError={activity.isError}
            error={activity.error ?? null}
            retrying={activity.isFetching}
            onRetry={() => void activity.refetch()}
          />
        </section>
      </div>
    </section>
  );
}
