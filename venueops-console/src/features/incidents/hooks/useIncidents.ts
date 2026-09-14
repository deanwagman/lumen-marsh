import { useQuery } from '@tanstack/react-query';

import { incidentListQuery } from '@/features/incidents/api/IncidentQueries';
import { useApiClient } from '@/shared/api/useApiClient';

export function useIncidents() {
  const client = useApiClient();
  return useQuery(incidentListQuery(client));
}
