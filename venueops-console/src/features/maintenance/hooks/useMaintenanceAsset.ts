import { useQuery } from '@tanstack/react-query';

import { useAuth } from '@/auth/AuthContext';
import { canReadMaintenance } from '@/features/maintenance/domain/maintenance';
import {
  maintenanceAssetQuery,
  maintenanceAssetWorkOrdersQuery,
} from '@/features/maintenance/api/MaintenanceQueries';
import { useApiClient } from '@/shared/api/useApiClient';

export function useMaintenanceAsset(assetId: string | undefined) {
  const client = useApiClient();
  const { session } = useAuth();
  return useQuery({
    ...maintenanceAssetQuery(client, assetId ?? ''),
    enabled: canReadMaintenance(session) && Boolean(assetId),
  });
}

export function useMaintenanceAssetWorkOrders(assetId: string | undefined) {
  const client = useApiClient();
  const { session } = useAuth();
  return useQuery({
    ...maintenanceAssetWorkOrdersQuery(client, assetId ?? ''),
    enabled: canReadMaintenance(session) && Boolean(assetId),
  });
}
