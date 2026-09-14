import { useCallback } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';

import {
  AttractionQueries,
  setCommandReceipt,
} from '@/features/attractions/api/AttractionQueries';
import type { CommandReceipt } from '@/features/attractions/domain/receipt';

export function useCommandReceipt() {
  const queryClient = useQueryClient();
  const query = useQuery<CommandReceipt | null>({
    queryKey: AttractionQueries.receipt(),
    queryFn: () => null,
    staleTime: Infinity,
    gcTime: Infinity,
    initialData: null,
  });

  const dismiss = useCallback(() => {
    setCommandReceipt(queryClient, null);
  }, [queryClient]);

  return {
    receipt: query.data ?? null,
    dismiss,
  };
}
