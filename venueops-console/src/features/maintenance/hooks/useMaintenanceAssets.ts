import { useQuery } from '@tanstack/react-query';

import { useAuth } from '@/auth/AuthContext';
import { canReadMaintenance } from '@/features/maintenance/domain/maintenance';
import {
  maintenanceAssetListQuery,
  toAssetQueryFilters,
} from '@/features/maintenance/api/MaintenanceQueries';
import {
  defaultAssetFilter,
  type AssetListFilter,
} from '@/features/maintenance/domain/maintenance';
import { useApiClient } from '@/shared/api/useApiClient';

export function useMaintenanceAssets(filter: AssetListFilter = defaultAssetFilter) {
  const client = useApiClient();
  const { session } = useAuth();
  return useQuery({
    ...maintenanceAssetListQuery(client, toAssetQueryFilters(filter)),
    enabled: canReadMaintenance(session),
  });
}
