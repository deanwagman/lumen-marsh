import { useRef } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';

import { setCommandReceipt } from '@/features/attractions/api/AttractionQueries';
import {
  IncidentCommands,
  type IncidentCommandInput,
  type ReportIncidentInput,
} from '@/features/incidents/api/IncidentCommands';
import {
  applyOperatorIncident,
  incidentActivityQuery,
  IncidentQueries,
} from '@/features/incidents/api/IncidentQueries';
import { newCommandId } from '@/features/incidents/domain/commands';
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

export function useIncidentCommand() {
  const client = useApiClient();
  const queryClient = useQueryClient();
  const identity = useReusableCommandId();

  return useMutation({
    mutationFn: (input: Omit<IncidentCommandInput, 'commandId'> & { commandId?: string }) => {
      const commandId =
        input.commandId ?? identity.identityFor(`${input.incidentId}:${input.type}`);
      return IncidentCommands.execute(client, { ...input, commandId });
    },
    onSuccess: async (incident, variables) => {
      identity.clear();
      applyOperatorIncident(queryClient, incident);
      let activityId: string | null = null;
      try {
        const activity = await queryClient.fetchQuery(
          incidentActivityQuery(client, incident.id),
        );
        activityId =
          activity.find((item) => item.resultingVersion === incident.version)?.id ?? null;
      } catch {
        // Receipt still confirms the command.
      }
      setCommandReceipt(queryClient, {
        incidentId: incident.id,
        summary: `${incident.title} ${variables.type.replaceAll('_', ' ').toLowerCase()} · version ${incident.version}`,
        version: incident.version,
        activityId,
      });
      await queryClient.invalidateQueries({
        queryKey: IncidentQueries.activity(incident.id),
      });
    },
    onError: (error) => {
      if (error instanceof VersionConflictError || error instanceof DuplicateCommandError) {
        identity.resetForNewIntent();
      }
    },
  });
}

export function useReportIncident() {
  const client = useApiClient();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: ReportIncidentInput) => IncidentCommands.report(client, input),
    onSuccess: (incident) => {
      applyOperatorIncident(queryClient, incident);
      setCommandReceipt(queryClient, {
        incidentId: incident.id,
        summary: `${incident.title} reported · version ${incident.version}`,
        version: incident.version,
        activityId: null,
      });
    },
  });
}
