import { useQuery } from '@tanstack/react-query';

import { useAuth } from '@/auth/AuthContext';
import { flowRecommendationListQuery } from '@/features/flow/api/FlowQueries';
import { canReadFlow } from '@/features/flow/domain/flow';
import { useApiClient } from '@/shared/api/useApiClient';

export function useFlowNavBadge() {
  const { session } = useAuth();
  const client = useApiClient();
  const enabled = canReadFlow(session);
  const recommendations = useQuery({
    ...flowRecommendationListQuery(client, {
      status: 'PENDING_REVIEW',
      page: 0,
      size: 20,
    }),
    enabled,
  });

  return {
    visible: enabled,
    count: recommendations.data?.total ?? 0,
  };
}
