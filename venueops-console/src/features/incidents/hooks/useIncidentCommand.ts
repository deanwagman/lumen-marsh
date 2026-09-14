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
import { useApiClient } from '@/shared/api/useApiClient';

export function useIncidentCommand() {
  const client = useApiClient();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: IncidentCommandInput) => IncidentCommands.execute(client, input),
    onSuccess: async (incident, variables) => {
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
