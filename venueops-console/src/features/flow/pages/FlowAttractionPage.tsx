import { Link, useParams } from 'react-router-dom';

import { formatLabel, formatUpdatedAt, formatWaitTime } from '@/features/attractions/domain/formatters';
import { ForecastChart } from '@/features/flow/components/ForecastChart';
import { flowAttractionQuery } from '@/features/flow/api/FlowQueries';
import { canReadFlow, type FlowAttractionCard } from '@/features/flow/domain/flow';
import { useAuth } from '@/auth/AuthContext';
import { useApiClient } from '@/shared/api/useApiClient';
import { useQuery } from '@tanstack/react-query';

import styles from './FlowPage.module.css';

export default function FlowAttractionPage() {
  const { attractionId = '' } = useParams();
  const { session } = useAuth();
  const client = useApiClient();
  const detail = useQuery({
    ...flowAttractionQuery(client, attractionId),
    enabled: canReadFlow(session) && Boolean(attractionId),
  });

  if (!canReadFlow(session)) {
    return (
      <section className={styles.page}>
        <h1>Park Flow</h1>
        <p>This workspace requires the venueops/flow.read scope.</p>
      </section>
    );
  }

  const data = detail.data;
  const card: FlowAttractionCard | null = data
    ? {
        attractionId: data.attractionId,
        displayName: data.displayName,
        status: data.status,
        postedWaitMinutes: data.postedWaitMinutes,
        calculatedWaitMinutes: data.projection?.calculatedWaitMinutes ?? null,
        predictedWaitMinutes15:
          data.forecasts.find((item) => item.horizonMinutes === 15)?.predictedWaitMinutes ?? null,
        predictedWaitMinutes30:
          data.forecasts.find((item) => item.horizonMinutes === 30)?.predictedWaitMinutes ?? null,
        predictedWaitMinutes60:
          data.forecasts.find((item) => item.horizonMinutes === 60)?.predictedWaitMinutes ?? null,
        queueLength: data.projection?.queueLength ?? null,
        arrivalsPerMinute: data.projection?.arrivalsPerMinute ?? null,
        throughputPerMinute: data.projection?.throughputPerMinute ?? null,
        operatingCapacityPercent: data.projection?.operatingCapacityPercent ?? null,
        trend: data.projection?.trend ?? null,
        freshness: data.projection?.freshness ?? null,
        observedAt: data.projection?.observedAt ?? null,
        simulated: data.simulated,
      }
    : null;

  return (
    <section className={styles.page}>
      <p>
        <Link to="/park-flow">Park Flow</Link>
      </p>
      <h1>{data?.displayName ?? 'Attraction flow'}</h1>
      {detail.isPending ? (
        <p className={styles.muted} role="status">
          Loading attraction flow
        </p>
      ) : null}
      {detail.isError ? (
        <div className={styles.error} role="alert">
          <p>Unable to load this attraction forecast.</p>
        </div>
      ) : null}
      {card ? <ForecastChart card={card} /> : null}
      {data?.projection ? (
        <dl className={styles.metrics}>
          <div>
            <dt>Posted</dt>
            <dd>{formatWaitTime(data.postedWaitMinutes)}</dd>
          </div>
          <div>
            <dt>Calculated</dt>
            <dd>{formatWaitTime(data.projection.calculatedWaitMinutes)}</dd>
          </div>
          <div>
            <dt>Freshness</dt>
            <dd>{formatLabel(data.projection.freshness)}</dd>
          </div>
          <div>
            <dt>Observed</dt>
            <dd>{formatUpdatedAt(data.projection.observedAt)}</dd>
          </div>
        </dl>
      ) : null}
      {(data?.forecasts ?? []).map((forecast) => (
        <article key={forecast.forecastId}>
          <h2>{forecast.horizonMinutes}-minute forecast</h2>
          <p>{forecast.explanation}</p>
          <ul>
            {forecast.assumptions.map((assumption) => (
              <li key={assumption}>{assumption}</li>
            ))}
          </ul>
        </article>
      ))}
    </section>
  );
}
