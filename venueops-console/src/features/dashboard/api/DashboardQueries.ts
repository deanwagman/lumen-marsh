import type { QueryClient } from '@tanstack/react-query';

import type { OperatorDashboard } from '@/features/dashboard/domain/dashboard';
import type { ApiClient } from '@/shared/api/client';

import { operatorDashboardSchema } from './dashboardSchema';

export const DashboardQueries = {
  all: ['dashboard'] as const,
  snapshot: () => [...DashboardQueries.all, 'snapshot'] as const,
};

export function dashboardSnapshotQuery(client: ApiClient, queryClient: QueryClient) {
  return {
    queryKey: DashboardQueries.snapshot(),
    queryFn: async ({ signal }: { signal: AbortSignal }) => {
      const next = await client.get(
        '/api/v1/operator/dashboard',
        operatorDashboardSchema,
        { signal },
      );
      const current = queryClient.getQueryData<OperatorDashboard>(DashboardQueries.snapshot());
      if (current && Date.parse(next.generatedAt) < Date.parse(current.generatedAt)) {
        return current;
      }
      return next;
    },
  };
}
