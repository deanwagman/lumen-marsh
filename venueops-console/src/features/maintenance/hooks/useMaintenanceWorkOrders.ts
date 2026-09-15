import { useQuery } from '@tanstack/react-query';

import { useAuth } from '@/auth/AuthContext';
import { canReadMaintenance } from '@/features/maintenance/domain/maintenance';
import {
  maintenanceWorkOrderListQuery,
  toWorkOrderQueryFilters,
} from '@/features/maintenance/api/MaintenanceQueries';
import type { WorkOrderListFilter } from '@/features/maintenance/domain/maintenance';
import { useApiClient } from '@/shared/api/useApiClient';

export function useMaintenanceWorkOrders(filter: WorkOrderListFilter) {
  const client = useApiClient();
  const { session } = useAuth();
  return useQuery({
    ...maintenanceWorkOrderListQuery(client, toWorkOrderQueryFilters(filter)),
    enabled: canReadMaintenance(session),
  });
}
