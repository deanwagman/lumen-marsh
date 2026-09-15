import { formatLabel } from '@/features/attractions/domain/formatters';
import type { MaintenanceActivity } from '@/features/maintenance/domain/maintenance';
import {
  formatEventType,
  formatMaintenanceTime,
  formatWorkOrderStatus,
} from '@/features/maintenance/domain/maintenancePresentation';
import { Button } from '@/shared/ui/Button';

import styles from './WorkOrderTimeline.module.css';

export function WorkOrderTimeline({
  activity,
  isPending,
  isError,
  error,
  onRetry,
  retrying = false,
}: {
  activity: MaintenanceActivity[] | undefined;
  isPending: boolean;
  isError: boolean;
  error: Error | null;
  onRetry?: () => void;
  retrying?: boolean;
}) {
  if (isPending) {
    return (
      <p className={styles.muted} role="status">
        Loading activity
      </p>
    );
  }

  if (isError) {
    return (
      <div role="alert">
        <p>Unable to load work-order activity</p>
        {onRetry ? (
          <Button onClick={onRetry} disabled={retrying}>
            {retrying ? 'Retrying…' : 'Retry'}
          </Button>
        ) : null}
        {error?.message ? <p className={styles.muted}>{error.message}</p> : null}
      </div>
    );
  }

  if (!activity || activity.length === 0) {
    return <p className={styles.muted}>No activity recorded yet.</p>;
  }

  const items = [...activity].sort((left, right) => right.sequence - left.sequence);

  return (
    <ol className={styles.list} id="activity">
      {items.map((item) => (
        <li key={item.id} id={`activity-${item.id}`}>
          <p>
            <strong>{formatEventType(item.eventType)}</strong>
          </p>
          <p className={styles.muted}>{item.actorDisplayName}</p>
          <p className={styles.muted}>{formatMaintenanceTime(item.occurredAt)}</p>
          {item.fromStatus || item.toStatus ? (
            <p>
              {item.fromStatus ? formatWorkOrderStatus(item.fromStatus) : '—'} →{' '}
              {item.toStatus ? formatWorkOrderStatus(item.toStatus) : '—'}
            </p>
          ) : null}
          {item.reason ? <p>Reason {item.reason}</p> : null}
          <p className={styles.muted}>Version {item.resultingVersion}</p>
          <details className={styles.tech}>
            <summary>Technical details</summary>
            <p className={styles.mono}>Event {item.eventType}</p>
            <p className={styles.mono}>Correlation {item.correlationId}</p>
            {item.commandId ? <p className={styles.mono}>Command {item.commandId}</p> : null}
            {Object.keys(item.details).length > 0 ? (
              <p className={styles.mono}>{JSON.stringify(item.details)}</p>
            ) : null}
            <p className={styles.muted}>Original event value {formatLabel(item.eventType)}</p>
          </details>
        </li>
      ))}
    </ol>
  );
}
