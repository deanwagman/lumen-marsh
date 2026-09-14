import type { StatusTone } from '@/features/attractions/domain/attraction';

import styles from './StatusBadge.module.css';

export function StatusBadge({ tone, label }: { tone: StatusTone; label: string }) {
  return (
    <span className={`${styles.badge} ${styles[tone]}`}>
      <span className={styles.dot} aria-hidden="true" />
      {label}
    </span>
  );
}
