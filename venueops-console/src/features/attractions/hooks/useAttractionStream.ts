import { useQuery } from '@tanstack/react-query';

import { AttractionQueries } from '@/features/attractions/api/AttractionQueries';
import {
  defaultStreamHealth,
  type AttractionStreamHealth,
} from '@/features/attractions/domain/streamHealth';

export function useAttractionStreamHealth() {
  return useQuery<AttractionStreamHealth>({
    queryKey: AttractionQueries.stream(),
    queryFn: () => defaultStreamHealth,
    staleTime: Infinity,
    gcTime: Infinity,
    initialData: defaultStreamHealth,
  });
}
