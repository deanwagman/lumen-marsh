import { useQuery } from '@tanstack/react-query';

import { attractionActivityQuery } from '@/features/attractions/api/AttractionQueries';
import { useApiClient } from '@/shared/api/useApiClient';

export function useAttractionActivity(attractionId: string | undefined) {
  const client = useApiClient();

  return useQuery({
    ...attractionActivityQuery(client, attractionId ?? ''),
    enabled: Boolean(attractionId),
  });
}
