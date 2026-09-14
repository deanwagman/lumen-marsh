import type { IncidentActivity } from '@/features/incidents/domain/incident';
import {
  formatActivityDetails,
  formatActivityType,
  formatIncidentTime,
} from '@/features/incidents/domain/formatters';
import { Button } from '@/shared/ui/Button';

import styles from './IncidentActivityTimeline.module.css';

export function IncidentActivityTimeline({
  activity,
  isPending,
  isError,
  error,
  attractionNames = new Map(),
  onRetry,
  retrying = false,
}: {
  activity: IncidentActivity[] | undefined;
  isPending: boolean;
  isError: boolean;
  error: Error | null;
  attractionNames?: Map<string, string>;
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
      <div className={styles.error} role="alert">
        <p>Unable to load incident activity</p>
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

  const items = [...activity].reverse();

  return (
    <ol className={styles.list} id="activity">
      {items.map((item) => {
        const details = formatActivityDetails(item, attractionNames);
        return (
          <li key={item.id} id={`activity-${item.id}`}>
            <p>
              <strong>{formatActivityType(item.type)}</strong>
            </p>
            <p className={styles.muted}>{item.actor}</p>
            <p className={styles.muted}>{formatIncidentTime(item.occurredAt)}</p>
            {item.reason ? <p>Reason {item.reason}</p> : null}
            <p className={styles.muted}>
              Version {item.previousVersion} → {item.resultingVersion}
            </p>
            {details.map((detail) => (
              <p key={detail}>{detail}</p>
            ))}
          </li>
        );
      })}
    </ol>
  );
}
