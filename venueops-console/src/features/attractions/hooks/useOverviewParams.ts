import { useSearchParams } from 'react-router-dom';

import {
  parseOverviewFilter,
  parseOverviewSort,
  type OverviewFilter,
  type OverviewSort,
} from '@/features/attractions/domain/overview';

export function useOverviewParams() {
  const [params, setParams] = useSearchParams();
  const filter = parseOverviewFilter(params.get('filter'));
  const sort = parseOverviewSort(params.get('sort'));

  function update(next: { filter?: OverviewFilter; sort?: OverviewSort }) {
    const nextParams = new URLSearchParams(params);
    const nextFilter = next.filter ?? filter;
    const nextSort = next.sort ?? sort;

    if (nextFilter === 'all') {
      nextParams.delete('filter');
    } else {
      nextParams.set('filter', nextFilter);
    }

    if (nextSort === 'severity') {
      nextParams.delete('sort');
    } else {
      nextParams.set('sort', nextSort);
    }

    setParams(nextParams, { replace: true });
  }

  return {
    filter,
    sort,
    setFilter: (next: OverviewFilter) => {
      update({ filter: next });
    },
    setSort: (next: OverviewSort) => {
      update({ sort: next });
    },
  };
}
