import { useQuery } from '@tanstack/react-query';

import { flowActivityQuery, flowRecommendationQuery } from '@/features/flow/api/FlowQueries';
import { canReadFlow } from '@/features/flow/domain/flow';
import { useAuth } from '@/auth/AuthContext';
import { useApiClient } from '@/shared/api/useApiClient';

export function useFlowRecommendation(recommendationId: string | null) {
  const { session } = useAuth();
  const client = useApiClient();
  return useQuery({
    ...flowRecommendationQuery(client, recommendationId ?? ''),
    enabled: canReadFlow(session) && Boolean(recommendationId),
  });
}

export function useFlowActivity(recommendationId: string | null) {
  const { session } = useAuth();
  const client = useApiClient();
  return useQuery({
    ...flowActivityQuery(client, recommendationId ?? ''),
    enabled: canReadFlow(session) && Boolean(recommendationId),
  });
}
