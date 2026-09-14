import { useQuery } from '@tanstack/react-query';

import { incidentDetailQuery } from '@/features/incidents/api/IncidentQueries';
import { useApiClient } from '@/shared/api/useApiClient';

export function useIncidentDetail(incidentId: string | undefined) {
  const client = useApiClient();
  return useQuery({
    ...incidentDetailQuery(client, incidentId ?? ''),
    enabled: Boolean(incidentId),
  });
}
