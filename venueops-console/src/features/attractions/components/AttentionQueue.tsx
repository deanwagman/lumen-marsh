import { Link } from 'react-router-dom';

import type { AttractionSummary } from '@/features/attractions/domain/attraction';
import { statusTone } from '@/features/attractions/domain/attraction';
import { attentionReason } from '@/features/attractions/domain/overview';
import {
  formatStatus,
  formatUpdatedAt,
} from '@/features/attractions/domain/formatters';
import { StatusBadge } from '@/shared/ui/StatusBadge';

import styles from './AttentionQueue.module.css';

export function AttentionQueue({ attractions }: { attractions: AttractionSummary[] }) {
  if (attractions.length === 0) {
    return null;
  }

  return (
    <section className={styles.queue} aria-labelledby="attention-heading">
      <div className={styles.header}>
        <h2 id="attention-heading">Attention required</h2>
        <p className={styles.count}>
          {attractions.length} {attractions.length === 1 ? 'attraction' : 'attractions'}
        </p>
      </div>
      <ul className={styles.list}>
        {attractions.map((attraction) => (
          <li key={attraction.id}>
            <Link className={styles.item} to={`/attractions/${attraction.id}`}>
              <div className={styles.identity}>
                <strong>{attraction.name}</strong>
                <span className={styles.area}>{attraction.area}</span>
              </div>
              <StatusBadge
                tone={statusTone(attraction.status)}
                label={formatStatus(attraction.status)}
              />
              <p className={styles.reason}>{attentionReason(attraction)}</p>
              <p className={styles.when}>{formatUpdatedAt(attraction.updatedAt)}</p>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}
