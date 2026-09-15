import { useQuery } from '@tanstack/react-query';

import { useAuth } from '@/auth/AuthContext';
import { canReadMaintenance } from '@/features/maintenance/domain/maintenance';
import { maintenanceRecommendationListQuery } from '@/features/maintenance/api/MaintenanceQueries';
import { useApiClient } from '@/shared/api/useApiClient';

export function useMaintenanceRecommendations() {
  const client = useApiClient();
  const { session } = useAuth();
  return useQuery({
    ...maintenanceRecommendationListQuery(client),
    enabled: canReadMaintenance(session),
  });
}
