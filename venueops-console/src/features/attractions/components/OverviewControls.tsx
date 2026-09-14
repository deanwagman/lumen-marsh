import {
  overviewFilterLabels,
  overviewFilters,
  overviewSortLabels,
  overviewSorts,
  type OverviewFilter,
  type OverviewSort,
} from '@/features/attractions/domain/overview';

import styles from './OverviewControls.module.css';

export function OverviewControls({
  filter,
  sort,
  onFilterChange,
  onSortChange,
}: {
  filter: OverviewFilter;
  sort: OverviewSort;
  onFilterChange: (filter: OverviewFilter) => void;
  onSortChange: (sort: OverviewSort) => void;
}) {
  return (
    <div className={styles.bar}>
      <div className={styles.filters} role="group" aria-label="Filter attractions">
        {overviewFilters.map((option) => (
          <button
            key={option}
            type="button"
            className={filter === option ? `${styles.chip} ${styles.chipActive}` : styles.chip}
            aria-pressed={filter === option}
            onClick={() => {
              onFilterChange(option);
            }}
          >
            {overviewFilterLabels[option]}
          </button>
        ))}
      </div>
      <label className={styles.sort}>
        Sort
        <select
          value={sort}
          aria-label="Sort attractions"
          onChange={(event) => {
            onSortChange(event.target.value as OverviewSort);
          }}
        >
          {overviewSorts.map((option) => (
            <option key={option} value={option}>
              {overviewSortLabels[option]}
            </option>
          ))}
        </select>
      </label>
    </div>
  );
}
