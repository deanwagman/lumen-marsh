import { Link } from 'react-router-dom';

import { formatLabel, formatWaitTime } from '@/features/attractions/domain/formatters';
import { ForecastChart } from '@/features/flow/components/ForecastChart';
import type { FlowAttractionCard } from '@/features/flow/domain/flow';

import styles from './AttractionFlowCard.module.css';

export function AttractionFlowCard({ card }: { card: FlowAttractionCard }) {
  return (
    <article className={styles.card}>
      <header className={styles.header}>
        <h3>
          <Link to={`/park-flow/${card.attractionId}`}>{card.displayName}</Link>
        </h3>
        <p>
          {formatLabel(card.status)}
          {card.simulated ? <span className={styles.badge}>Simulated</span> : null}
        </p>
      </header>
      <dl className={styles.facts}>
        <div>
          <dt>Posted wait</dt>
          <dd>{formatWaitTime(card.postedWaitMinutes)}</dd>
        </div>
        <div>
          <dt>Calculated wait</dt>
          <dd>{formatWaitTime(card.calculatedWaitMinutes)}</dd>
        </div>
        <div>
          <dt>Queue</dt>
          <dd>{card.queueLength ?? '—'}</dd>
        </div>
        <div>
          <dt>Arrivals / min</dt>
          <dd>{card.arrivalsPerMinute?.toFixed(1) ?? '—'}</dd>
        </div>
        <div>
          <dt>Throughput / min</dt>
          <dd>{card.throughputPerMinute?.toFixed(1) ?? '—'}</dd>
        </div>
        <div>
          <dt>Capacity</dt>
          <dd>{card.operatingCapacityPercent == null ? '—' : `${card.operatingCapacityPercent}%`}</dd>
        </div>
        <div>
          <dt>Trend</dt>
          <dd>{card.trend ? formatLabel(card.trend) : '—'}</dd>
        </div>
        <div>
          <dt>Freshness</dt>
          <dd>{card.freshness ? formatLabel(card.freshness) : '—'}</dd>
        </div>
      </dl>
      <ForecastChart card={card} />
    </article>
  );
}
