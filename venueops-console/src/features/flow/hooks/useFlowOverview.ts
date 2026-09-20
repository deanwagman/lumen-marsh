import { useQuery } from '@tanstack/react-query';

import { flowOverviewQuery } from '@/features/flow/api/FlowQueries';
import { canReadFlow } from '@/features/flow/domain/flow';
import { useAuth } from '@/auth/AuthContext';
import { useApiClient } from '@/shared/api/useApiClient';

export function useFlowOverview() {
  const { session } = useAuth();
  const client = useApiClient();
  return useQuery({
    ...flowOverviewQuery(client),
    enabled: canReadFlow(session),
  });
}
