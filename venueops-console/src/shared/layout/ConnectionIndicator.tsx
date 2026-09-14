import { reconnectAttractionStream } from '@/features/attractions/api/AttractionEventSource';
import { useAttractionStreamHealth } from '@/features/attractions/hooks/useAttractionStream';
import { streamStatusLabel } from '@/features/attractions/domain/streamHealth';
import { formatUpdatedAt } from '@/features/attractions/domain/formatters';
import { Button } from '@/shared/ui/Button';

import styles from './ConnectionIndicator.module.css';

export function ConnectionIndicator() {
  const { data } = useAttractionStreamHealth();
  const health = data;
  const label = streamStatusLabel(health.status);
  const lastSnapshot = health.lastEventAt
    ? formatUpdatedAt(new Date(health.lastEventAt).toISOString())
    : null;
  const showReconnect = health.status !== 'live';

  return (
    <div className={styles.cluster}>
      <p className={styles.indicator} aria-live="polite">
        <span className={`${styles.dot} ${styles[health.status]}`} aria-hidden="true" />
        {label}
      </p>
      {lastSnapshot ? (
        <p className={styles.meta}>Last snapshot {lastSnapshot}</p>
      ) : (
        <p className={styles.meta}>Waiting for snapshot</p>
      )}
      {showReconnect ? (
        <Button variant="ghost" onClick={() => reconnectAttractionStream()}>
          Reconnect
        </Button>
      ) : null}
    </div>
  );
}
