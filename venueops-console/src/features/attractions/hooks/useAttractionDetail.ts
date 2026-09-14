import { useQuery } from '@tanstack/react-query';

import { attractionDetailQuery } from '@/features/attractions/api/AttractionQueries';
import { useApiClient } from '@/shared/api/useApiClient';

export function useAttractionDetail(attractionId: string | undefined) {
  const client = useApiClient();

  return useQuery({
    ...attractionDetailQuery(client, attractionId ?? ''),
    enabled: Boolean(attractionId),
  });
}
