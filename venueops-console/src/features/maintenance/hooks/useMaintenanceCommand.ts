import { useRef } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';

import { setCommandReceipt } from '@/features/attractions/api/AttractionQueries';
import {
  MaintenanceCommands,
  type MaintenanceRecommendationCommandInput,
  type MaintenanceWorkOrderCommandInput,
} from '@/features/maintenance/api/MaintenanceCommands';
import { MaintenanceQueries } from '@/features/maintenance/api/MaintenanceQueries';
import {
  applyRecommendation,
  refreshAfterRecommendationCommand,
  refreshAfterWorkOrderCommand,
} from '@/features/maintenance/api/maintenanceCache';
import { newCommandId } from '@/features/maintenance/domain/maintenance';
import {
  DuplicateCommandError,
  VersionConflictError,
} from '@/shared/api/errors';
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

export function workOrderIntentKey(
  input: Omit<MaintenanceWorkOrderCommandInput, 'commandId'> & { commandId?: string },
): string {
  if (input.type === 'RECORD_CHECKLIST_RESULT') {
    return `${input.workOrderId}:${input.type}:${String(input.data?.checklistItemId ?? '')}`;
  }
  if (input.type === 'ADD_EVIDENCE') {
    return `${input.workOrderId}:${input.type}:${String(input.data?.uri ?? '')}`;
  }
  if (input.type === 'ADD_NOTE') {
    return `${input.workOrderId}:${input.type}:${String(input.data?.note ?? input.reason ?? '')}`;
  }
  return `${input.workOrderId}:${input.type}`;
}

export function useMaintenanceWorkOrderCommand() {
  const client = useApiClient();
  const queryClient = useQueryClient();
  const identity = useReusableCommandId();

  const mutation = useMutation({
    mutationFn: (
      input: Omit<MaintenanceWorkOrderCommandInput, 'commandId'> & { commandId?: string },
    ) => {
      const commandId = input.commandId ?? identity.identityFor(workOrderIntentKey(input));
      return MaintenanceCommands.executeWorkOrder(client, { ...input, commandId }).then(
        (result) => ({ result, commandId, type: input.type }),
      );
    },
    onSuccess: async ({ result, type }) => {
      identity.clear();
      await refreshAfterWorkOrderCommand(queryClient, result.workOrder.id);
      if (result.replay) {
        return;
      }
      setCommandReceipt(queryClient, {
        workOrderId: result.workOrder.id,
        summary: `${result.workOrder.workOrderNumber} ${type.replaceAll('_', ' ').toLowerCase()} · version ${result.workOrder.version}`,
        version: result.workOrder.version,
        activityId: null,
      });
    },
    onError: (error) => {
      if (error instanceof VersionConflictError) {
        identity.resetForNewIntent();
      }
      if (error instanceof DuplicateCommandError) {
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

export function useMaintenanceRecommendationCommand() {
  const client = useApiClient();
  const queryClient = useQueryClient();
  const identity = useReusableCommandId();

  const mutation = useMutation({
    mutationFn: (
      input: Omit<MaintenanceRecommendationCommandInput, 'commandId'> & { commandId?: string },
    ) => {
      const commandId =
        input.commandId ?? identity.identityFor(`${input.recommendationId}:${input.type}`);
      return MaintenanceCommands.executeRecommendation(client, { ...input, commandId }).then(
        (recommendation) => ({ recommendation, commandId, type: input.type }),
      );
    },
    onSuccess: async ({ recommendation, type }) => {
      identity.clear();
      applyRecommendation(queryClient, recommendation);
      await refreshAfterRecommendationCommand(queryClient, recommendation);
      setCommandReceipt(queryClient, {
        workOrderId: recommendation.workOrderId ?? undefined,
        summary:
          type === 'ACCEPT'
            ? `Recommendation accepted${recommendation.workOrderId ? ' · work order created' : ''} · version ${recommendation.version}`
            : `Recommendation dismissed · version ${recommendation.version}`,
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

export function useMaintenanceOverviewQueries() {
  const queryClient = useQueryClient();
  return {
    invalidateAll: () =>
      queryClient.invalidateQueries({ queryKey: MaintenanceQueries.all }),
  };
}
