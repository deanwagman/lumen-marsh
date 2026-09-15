import { useQuery } from '@tanstack/react-query';

import { useAuth } from '@/auth/AuthContext';
import { canReadMaintenance, defaultWorkOrderFilter, maintenanceNavBadgeCount } from '@/features/maintenance/domain/maintenance';
import {
  maintenanceRecommendationListQuery,
  maintenanceWorkOrderListQuery,
  toWorkOrderQueryFilters,
} from '@/features/maintenance/api/MaintenanceQueries';
import { useApiClient } from '@/shared/api/useApiClient';

export function useMaintenanceNavBadge() {
  const { session } = useAuth();
  const client = useApiClient();
  const enabled = canReadMaintenance(session);
  const recommendations = useQuery({
    ...maintenanceRecommendationListQuery(client),
    enabled,
  });
  const workOrders = useQuery({
    ...maintenanceWorkOrderListQuery(
      client,
      toWorkOrderQueryFilters({ ...defaultWorkOrderFilter, lifecycle: 'all', size: 100, page: 0 }),
    ),
    enabled,
  });

  return {
    visible: enabled,
    count: maintenanceNavBadgeCount({
      recommendations: recommendations.data ?? [],
      workOrders: workOrders.data?.items ?? [],
    }),
  };
}
