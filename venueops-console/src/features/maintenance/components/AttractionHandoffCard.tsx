import { useState } from 'react';
import { Link } from 'react-router-dom';

import type { AttractionSummary, OperatorAttraction } from '@/features/attractions/domain/attraction';
import type { AuthSession } from '@/auth/session';
import type { AttractionCommandType } from '@/features/attractions/domain/commands';
import {
  attractionHandoffOwnershipMessage,
  canReportTechnicalFault,
  canStartAttractionTesting,
} from '@/features/maintenance/domain/maintenanceActions';
import type { MaintenanceWorkOrder } from '@/features/maintenance/domain/maintenance';
import { HelpHeading } from '@/shared/ui/HelpLink';
import { Button } from '@/shared/ui/Button';
import { MaintenanceCommandDialog } from '@/features/maintenance/components/MaintenanceCommandDialog';
import { ForbiddenError, VersionConflictError } from '@/shared/api/errors';

import styles from './AttractionHandoffCard.module.css';

export function AttractionHandoffCard({
  workOrder,
  session,
  attraction,
  pending,
  stale,
  error = null,
  onAttractionCommand,
}: {
  workOrder: MaintenanceWorkOrder;
  session: AuthSession | null;
  attraction?: AttractionSummary | OperatorAttraction | null;
  pending: boolean;
  stale?: boolean;
  error?: Error | null;
  onAttractionCommand: (input: {
    type: AttractionCommandType;
    expectedVersion: number;
    reason: string;
  }) => void | Promise<unknown>;
}) {
  const recommended = workOrder.recommendedAttractionAction;
  const [confirm, setConfirm] = useState(false);
  const [openedVersion, setOpenedVersion] = useState<number | null>(null);
  if (!recommended || workOrder.status === 'COMPLETED' || workOrder.status === 'CANCELED') {
    return null;
  }

  const startTesting = canStartAttractionTesting({ workOrder, session, attraction });
  const reportFault = canReportTechnicalFault({ workOrder, session, attraction });
  const ready = recommended.command === 'START_TESTING';
  const command: AttractionCommandType = ready ? 'START_TESTING' : 'REPORT_TECHNICAL_FAULT';
  const canIssue = ready ? startTesting : reportFault;
  const remoteStale =
    Boolean(stale) ||
    (openedVersion !== null && attraction != null && attraction.version !== openedVersion);

  function closeDialog() {
    setConfirm(false);
    setOpenedVersion(null);
  }

  return (
    <section className={`${styles.card} ${ready ? styles.testing : styles.fault}`} aria-labelledby="handoff-heading">
      <HelpHeading
        id="handoff-heading"
        article="maintenance"
        section="handoff"
        label="Help: attraction testing handoff"
      >
        {ready ? 'Ready for operational testing' : 'Attraction operational handoff'}
      </HelpHeading>
      <p>{recommended.reason}</p>
      <p className={styles.muted}>
        Recommended attraction command {recommended.command}. Maintenance does not change attraction
        state automatically.
      </p>
      {attraction ? (
        <p>
          <Link to={`/attractions/${workOrder.attractionId}`}>{attraction.name} workspace</Link>
        </p>
      ) : null}
      {ready && !canIssue ? <p>{attractionHandoffOwnershipMessage(attraction)}</p> : null}
      {!ready && !canIssue ? (
        <p className={styles.muted}>
          {attraction?.status === 'CLOSED'
            ? 'The attraction is already closed. Keep the work order in progress until inspection is approved.'
            : 'Report technical fault from the attraction workspace if Operations needs a technical delay.'}
        </p>
      ) : null}
      {canIssue ? (
        <Button
          onClick={() => {
            if (!attraction) {
              return;
            }
            setOpenedVersion(attraction.version);
            setConfirm(true);
          }}
          disabled={pending}
        >
          {ready ? 'Start attraction testing' : 'Report technical fault'}
        </Button>
      ) : null}
      {confirm && attraction ? (
        <MaintenanceCommandDialog
          title={ready ? 'Start attraction testing' : 'Report technical fault'}
          description="This is an attraction command, not a maintenance command. It does not complete the work order or reopen the ride."
          currentState={attraction.status.replaceAll('_', ' ')}
          resultingState={ready ? 'TESTING' : 'TECHNICAL DELAY'}
          attractionName={attraction.name}
          warning="Maintenance never reopens an attraction."
          stale={remoteStale}
          pending={pending}
          error={attractionCommandErrorCopy(error)}
          submitLabel={ready ? 'Start testing' : 'Report fault'}
          onClose={closeDialog}
          onSubmit={() => {
            if (remoteStale || openedVersion === null) {
              return;
            }
            void Promise.resolve(
              onAttractionCommand({
                type: command,
                expectedVersion: openedVersion,
                reason: workOrder.workOrderNumber,
              }),
            )
              .then(() => {
                closeDialog();
              })
              .catch(() => {
                // Keep the reviewed version and reason until the operator closes.
              });
          }}
        />
      ) : null}
    </section>
  );
}

function attractionCommandErrorCopy(error: Error | null): string | null {
  if (!error) {
    return null;
  }
  if (error instanceof ForbiddenError) {
    return 'That attraction command needs attractions.command. Form entries were kept.';
  }
  if (error instanceof VersionConflictError) {
    return 'The attraction changed while you were reviewing the handoff. Close this dialog and reload current state before submitting.';
  }
  return error.message;
}
