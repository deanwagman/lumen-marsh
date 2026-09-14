import { useQuery } from '@tanstack/react-query';

import { incidentActivityQuery } from '@/features/incidents/api/IncidentQueries';
import { useApiClient } from '@/shared/api/useApiClient';

export function useIncidentActivity(incidentId: string | undefined) {
  const client = useApiClient();
  return useQuery({
    ...incidentActivityQuery(client, incidentId ?? ''),
    enabled: Boolean(incidentId),
  });
}
