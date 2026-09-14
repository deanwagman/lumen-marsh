import { useMutation, useQueryClient } from '@tanstack/react-query';

import {
  AttractionCommands,
  type AttractionCommandInput,
} from '@/features/attractions/api/AttractionCommands';
import {
  applyOperatorAttraction,
  attractionActivityQuery,
  AttractionQueries,
  setCommandReceipt,
} from '@/features/attractions/api/AttractionQueries';
import { commandReceiptSummary } from '@/features/attractions/domain/receipt';
import { useApiClient } from '@/shared/api/useApiClient';

export function useAttractionCommand(attractionId: string) {
  const client = useApiClient();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (
      input: Omit<AttractionCommandInput, 'attractionId'> & {
        attractionId?: string;
      },
    ) =>
      AttractionCommands.execute(client, {
        ...input,
        attractionId: input.attractionId ?? attractionId,
      }),
    onSuccess: async (attraction, variables) => {
      applyOperatorAttraction(queryClient, attraction);

      let activityId: string | null = null;
      try {
        const activity = await queryClient.fetchQuery(
          attractionActivityQuery(client, attraction.id),
        );
        activityId =
          activity.find((item) => item.resultingVersion === attraction.version)?.id ?? null;
      } catch {
        // Receipt still confirms the command; the activity link is omitted.
      }

      setCommandReceipt(queryClient, {
        attractionId: attraction.id,
        attractionName: attraction.name,
        summary: commandReceiptSummary(attraction, variables.type),
        version: attraction.version,
        activityId,
      });

      await queryClient.invalidateQueries({
        queryKey: AttractionQueries.activity(attraction.id),
      });
    },
  });
}
