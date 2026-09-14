import type { Incident, IncidentSeverity, IncidentType } from '@/features/incidents/domain/incident';
import type { IncidentCommandType } from '@/features/incidents/domain/commands';
import type { ApiClient } from '@/shared/api/client';

import { incidentSchema } from './incidentSchema';

export type ReportIncidentInput = {
  title: string;
  type: IncidentType;
  severity: IncidentSeverity;
  internalDescription?: string;
  attractionIds?: string[];
};

export type IncidentCommandInput = {
  incidentId: string;
  type: IncidentCommandType;
  expectedVersion: number;
  reason?: string;
  assignee?: string;
  severity?: IncidentSeverity;
  attractionId?: string;
  guestTitle?: string;
  guestMessage?: string;
};

export const IncidentCommands = {
  async report(client: ApiClient, input: ReportIncidentInput): Promise<Incident> {
    return client.post('/api/v1/operator/incidents', incidentSchema, {
      title: input.title,
      type: input.type,
      severity: input.severity,
      internalDescription: input.internalDescription || undefined,
      attractionIds: input.attractionIds ?? [],
    });
  },

  async execute(client: ApiClient, input: IncidentCommandInput): Promise<Incident> {
    return client.post(
      `/api/v1/operator/incidents/${encodeURIComponent(input.incidentId)}/commands`,
      incidentSchema,
      {
        type: input.type,
        expectedVersion: input.expectedVersion,
        reason: input.reason || undefined,
        assignee: input.assignee || undefined,
        severity: input.severity,
        attractionId: input.attractionId,
        guestTitle: input.guestTitle,
        guestMessage: input.guestMessage,
      },
    );
  },
};
