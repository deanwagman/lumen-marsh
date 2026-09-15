import { useQuery } from '@tanstack/react-query';

import { useAuth } from '@/auth/AuthContext';
import { canReadMaintenance } from '@/features/maintenance/domain/maintenance';
import {
  maintenanceActivityQuery,
  maintenanceWorkOrderQuery,
} from '@/features/maintenance/api/MaintenanceQueries';
import { useApiClient } from '@/shared/api/useApiClient';

export function useMaintenanceWorkOrder(workOrderId: string | undefined) {
  const client = useApiClient();
  const { session } = useAuth();
  return useQuery({
    ...maintenanceWorkOrderQuery(client, workOrderId ?? ''),
    enabled: canReadMaintenance(session) && Boolean(workOrderId),
  });
}

export function useMaintenanceActivity(workOrderId: string | undefined) {
  const client = useApiClient();
  const { session } = useAuth();
  return useQuery({
    ...maintenanceActivityQuery(client, workOrderId ?? ''),
    enabled: canReadMaintenance(session) && Boolean(workOrderId),
  });
}
