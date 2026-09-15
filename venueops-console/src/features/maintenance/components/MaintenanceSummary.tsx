import { Link } from 'react-router-dom';

import type { MaintenanceSection } from '@/features/maintenance/domain/maintenance';

import styles from './MaintenanceSummary.module.css';

export type MaintenanceSummaryMetrics = {
  pendingRecommendations: number;
  activeP1: number;
  activeP2: number;
  awaitingInspection: number;
  readyForTesting: number;
  assetsOutOfService: number;
};

const cards: Array<{
  key: keyof MaintenanceSummaryMetrics;
  label: string;
  hint: string;
  href: string;
  section?: MaintenanceSection;
}> = [
  {
    key: 'pendingRecommendations',
    label: 'Pending recommendations',
    hint: 'Reliability inbox',
    href: '/maintenance?section=inbox',
  },
  {
    key: 'activeP1',
    label: 'Active P1',
    hint: 'Immediate work',
    href: '/maintenance?priority=P1&lifecycle=active',
  },
  {
    key: 'activeP2',
    label: 'Active P2',
    hint: 'Urgent work',
    href: '/maintenance?priority=P2&lifecycle=active',
  },
  {
    key: 'awaitingInspection',
    label: 'Awaiting inspection',
    hint: 'Supervisor review',
    href: '/maintenance?status=AWAITING_INSPECTION',
  },
  {
    key: 'readyForTesting',
    label: 'Ready for testing',
    hint: 'Operations handoff',
    href: '/maintenance?status=READY_FOR_TESTING',
  },
  {
    key: 'assetsOutOfService',
    label: 'Assets out of service',
    hint: 'Equipment status',
    href: '/maintenance?lifecycle=all',
  },
];

export function MaintenanceSummary({ metrics }: { metrics: MaintenanceSummaryMetrics }) {
  return (
    <ul className={styles.grid}>
      {cards.map((card) => (
        <li key={card.key}>
          <Link className={`${styles.card} ${styles[card.key]}`} to={card.href}>
            <span className={styles.label}>{card.label}</span>
            <span className={styles.value}>{metrics[card.key]}</span>
            <span className={styles.hint}>{card.hint}</span>
          </Link>
        </li>
      ))}
    </ul>
  );
}
