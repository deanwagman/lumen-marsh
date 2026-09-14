import { keepPreviousData, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef } from 'react';

import { useAttractionStreamHealth } from '@/features/attractions/hooks/useAttractionStream';
import { dashboardSnapshotQuery } from '@/features/dashboard/api/DashboardQueries';
import { useApiClient } from '@/shared/api/useApiClient';

export function useDashboard() {
  const client = useApiClient();
  const queryClient = useQueryClient();
  const stream = useAttractionStreamHealth();
  const previousStreamStatus = useRef(stream.data.status);
  const { refetch, ...query } = useQuery({
    ...dashboardSnapshotQuery(client, queryClient),
    placeholderData: keepPreviousData,
  });

  useEffect(() => {
    const previous = previousStreamStatus.current;
    const next = stream.data.status;
    previousStreamStatus.current = next;
    if ((previous === 'reconnecting' || previous === 'stale') && next === 'live') {
      void refetch();
    }
  }, [refetch, stream.data.status]);

  return { ...query, refetch };
}
