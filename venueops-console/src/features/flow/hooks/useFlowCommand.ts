import { useRef } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';

import { setCommandReceipt } from '@/features/attractions/api/AttractionQueries';
import { FlowCommands, type FlowCommandInput } from '@/features/flow/api/FlowCommands';
import { FlowQueries } from '@/features/flow/api/FlowQueries';
import { newCommandId } from '@/features/flow/domain/flow';
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

  return { identityFor, clear, resetForNewIntent, peek: () => pending.current };
}

export function useFlowCommand() {
  const client = useApiClient();
  const queryClient = useQueryClient();
  const identity = useReusableCommandId();

  const mutation = useMutation({
    mutationFn: (input: Omit<FlowCommandInput, 'commandId'> & { commandId?: string }) => {
      const commandId =
        input.commandId ?? identity.identityFor(`${input.recommendationId}:${input.type}`);
      return FlowCommands.execute(client, { ...input, commandId }).then((recommendation) => ({
        recommendation,
        commandId,
        type: input.type,
      }));
    },
    onSuccess: async ({ recommendation, type }) => {
      identity.clear();
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: FlowQueries.all }),
      ]);
      setCommandReceipt(queryClient, {
        summary: `Flow ${type.toLowerCase()} · ${recommendation.summary} · version ${recommendation.version}`,
        version: recommendation.version,
        activityId: null,
      });
    },
    onError: (error) => {
      if (error instanceof VersionConflictError || error instanceof DuplicateCommandError) {
        identity.resetForNewIntent();
      }
    },
  });

  return {
    ...mutation,
    pendingCommandId: identity.peek()?.commandId ?? null,
    beginNewIntent: identity.resetForNewIntent,
  };
}
