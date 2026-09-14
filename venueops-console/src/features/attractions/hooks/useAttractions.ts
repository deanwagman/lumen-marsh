import { useQuery } from '@tanstack/react-query';

import { attractionListQuery } from '@/features/attractions/api/AttractionQueries';
import { useApiClient } from '@/shared/api/useApiClient';

export function useAttractions() {
  const client = useApiClient();
  return useQuery(attractionListQuery(client));
}
