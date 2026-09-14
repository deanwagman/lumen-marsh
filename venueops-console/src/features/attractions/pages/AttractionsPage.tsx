import { AttractionTable } from '@/features/attractions/components/AttractionTable';
import { AttentionQueue } from '@/features/attractions/components/AttentionQueue';
import { OverviewControls } from '@/features/attractions/components/OverviewControls';
import { ShiftSummary } from '@/features/attractions/components/ShiftSummary';
import { attentionQueue, presentAttractions } from '@/features/attractions/domain/overview';
import { useAttractionStreamHealth } from '@/features/attractions/hooks/useAttractionStream';
import { useAttractions } from '@/features/attractions/hooks/useAttractions';
import { useLiveAttractionChanges } from '@/features/attractions/hooks/useLiveAttractionChanges';
import { useOverviewParams } from '@/features/attractions/hooks/useOverviewParams';
import { WeatherRecommendations } from '@/features/weather/components/WeatherRecommendations';
import { NetworkError } from '@/shared/api/errors';
import { Button } from '@/shared/ui/Button';

import styles from './AttractionsPage.module.css';

export default function AttractionsPage() {
  const { data, error, isError, isPending, refetch, isFetching, dataUpdatedAt } = useAttractions();
  const stream = useAttractionStreamHealth();
  const { filter, sort, setFilter, setSort } = useOverviewParams();
  const { flashes, announcement } = useLiveAttractionChanges(data);
  const cached = data ?? [];
  const rows = presentAttractions(cached, filter, sort);
  const queue = attentionQueue(cached);
  const syncedAt = stream.data.lastEventAt ?? (dataUpdatedAt || null);
  const showStale = stream.data.status === 'stale' && cached.length > 0;

  return (
    <section className={styles.page}>
      <header className={styles.header}>
        <p className={styles.kicker}>Live shift</p>
        <h1>Shift overview</h1>
        <p className={styles.muted}>Park conditions for the current operating period.</p>
      </header>
      <div className={styles.liveRegion} aria-live="polite" aria-atomic="true">
        {announcement}
      </div>
      {cached.length === 0 ? <WeatherRecommendations attractions={cached} /> : null}
      {isPending && cached.length === 0 ? <LoadingState /> : null}
      {isError && cached.length === 0 ? (
        <AttractionErrorState
          error={error}
          onRetry={() => {
            void refetch();
          }}
          retrying={isFetching}
        />
      ) : null}
      {cached.length > 0 ? (
        <>
          {showStale ? (
            <div
              className={`${styles.panel} ${styles.panelError}`}
              role="alert"
              aria-labelledby="stale-heading"
            >
              <h2 id="stale-heading">Showing last-known conditions</h2>
              <p className={styles.muted}>
                Live updates are delayed. Cached attraction data remains visible until the
                console reconnects.
              </p>
            </div>
          ) : null}
          <ShiftSummary attractions={cached} syncedAt={syncedAt} />
          <WeatherRecommendations attractions={cached} />
          <OverviewControls
            filter={filter}
            sort={sort}
            onFilterChange={setFilter}
            onSortChange={setSort}
          />
          <AttentionQueue attractions={queue} />
          {rows.length > 0 ? <AttractionTable attractions={rows} flashes={flashes} /> : null}
          {rows.length === 0 ? (
            <div className={styles.panel}>
              <h2>No attractions match this view</h2>
              <p className={styles.muted}>Clear the filter to see the full park catalog.</p>
            </div>
          ) : null}
        </>
      ) : null}
      {data && data.length === 0 ? <AttractionEmptyState /> : null}
    </section>
  );
}

function LoadingState() {
  return (
    <div aria-busy="true">
      <p className={styles.muted} role="status">
        Loading attractions
      </p>
      <div className={styles.skeleton} aria-hidden="true">
        <div className={styles.skeletonRow} />
        <div className={styles.skeletonRow} />
        <div className={styles.skeletonRow} />
      </div>
    </div>
  );
}

function AttractionEmptyState() {
  return (
    <div className={styles.panel}>
      <h2>No attractions reported</h2>
      <p className={styles.muted}>
        VenueOps returned an empty catalog. Confirm seed data is enabled on the API.
      </p>
    </div>
  );
}

function AttractionErrorState({
  error,
  onRetry,
  retrying,
}: {
  error: Error;
  onRetry: () => void;
  retrying: boolean;
}) {
  const message =
    error instanceof NetworkError
      ? 'The console could not reach VenueOps API. Confirm the service is running on port 8080.'
      : error.message;

  return (
    <div className={`${styles.panel} ${styles.panelError}`} role="alert">
      <h2>Unable to load attractions</h2>
      <p>{message}</p>
      <Button onClick={onRetry} disabled={retrying}>
        {retrying ? 'Retrying…' : 'Retry'}
      </Button>
    </div>
  );
}
