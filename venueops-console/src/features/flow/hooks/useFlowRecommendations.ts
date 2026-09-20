import { useQuery } from '@tanstack/react-query';

import {
  flowRecommendationListQuery,
  type FlowRecommendationFilters,
} from '@/features/flow/api/FlowQueries';
import { canReadFlow } from '@/features/flow/domain/flow';
import { useAuth } from '@/auth/AuthContext';
import { useApiClient } from '@/shared/api/useApiClient';

export function useFlowRecommendations(filters: FlowRecommendationFilters) {
  const { session } = useAuth();
  const client = useApiClient();
  return useQuery({
    ...flowRecommendationListQuery(client, filters),
    enabled: canReadFlow(session),
  });
}
