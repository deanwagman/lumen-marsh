import {
  formatAverageWait,
  shiftSummary,
} from '@/features/attractions/domain/overview';
import type { AttractionSummary } from '@/features/attractions/domain/attraction';
import { formatUpdatedAt } from '@/features/attractions/domain/formatters';

import styles from './ShiftSummary.module.css';

export function ShiftSummary({
  attractions,
  syncedAt,
}: {
  attractions: AttractionSummary[];
  syncedAt: number | null;
}) {
  const summary = shiftSummary(attractions);

  return (
    <dl className={styles.facts}>
      <div>
        <dt>Operating</dt>
        <dd>{summary.operatingCount}</dd>
      </div>
      <div>
        <dt>Needs attention</dt>
        <dd>{summary.attentionCount}</dd>
      </div>
      <div>
        <dt>Reduced capacity</dt>
        <dd>{summary.reducedCount}</dd>
      </div>
      <div>
        <dt>Average posted wait</dt>
        <dd>{formatAverageWait(summary.averageWaitMinutes)}</dd>
      </div>
      <div>
        <dt>Last synchronized</dt>
        <dd>{syncedAt ? formatUpdatedAt(new Date(syncedAt).toISOString()) : '—'}</dd>
      </div>
    </dl>
  );
}
