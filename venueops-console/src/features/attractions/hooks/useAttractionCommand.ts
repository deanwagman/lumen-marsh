import { useRef } from 'react';
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
import { newCommandId } from '@/features/attractions/domain/commands';
import { DuplicateCommandError, VersionConflictError } from '@/shared/api/errors';
import { useApiClient } from '@/shared/api/useApiClient';

function useReusableCommandId() {
  const pending = useRef<{ key: string; commandId: string } | null>(null);

  function identityFor(key: string): string {
    if (pending.current?.key === key) {
      return pending.current.commandId;
    }
    const commandId = newCommandId();
    pending.current = { key, commandId };
    return commandId;
  }

  function clear() {
    pending.current = null;
  }

  function resetForNewIntent() {
    pending.current = null;
  }

  return { identityFor, clear, resetForNewIntent };
}

export function useAttractionCommand(attractionId: string) {
  const client = useApiClient();
  const queryClient = useQueryClient();
  const identity = useReusableCommandId();

  return useMutation({
    mutationFn: (
      input: Omit<AttractionCommandInput, 'attractionId' | 'commandId'> & {
        attractionId?: string;
        commandId?: string;
      },
    ) => {
      const resolvedAttractionId = input.attractionId ?? attractionId;
      const commandId =
        input.commandId ?? identity.identityFor(`${resolvedAttractionId}:${input.type}`);
      return AttractionCommands.execute(client, {
        ...input,
        attractionId: resolvedAttractionId,
        commandId,
      });
    },
    onSuccess: async (attraction, variables) => {
      identity.clear();
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
    onError: (error) => {
      if (error instanceof VersionConflictError || error instanceof DuplicateCommandError) {
        identity.resetForNewIntent();
      }
    },
  });
}
