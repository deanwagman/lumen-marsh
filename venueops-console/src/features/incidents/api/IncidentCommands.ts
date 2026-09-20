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
  commandId: string;
  type: IncidentCommandType;
  expectedVersion: number;
  reason?: string;
  assignee?: string;
  severity?: IncidentSeverity;
  attractionId?: string;
  guestTitle?: string;
  guestMessage?: string;
  confirmActiveWorkOrders?: boolean;
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
    const data: Record<string, unknown> = {};
    if (input.assignee) {
      data.assignee = input.assignee;
    }
    if (input.severity) {
      data.severity = input.severity;
    }
    if (input.attractionId) {
      data.attractionId = input.attractionId;
    }
    if (input.guestTitle) {
      data.guestTitle = input.guestTitle;
    }
    if (input.guestMessage) {
      data.guestMessage = input.guestMessage;
    }
    if (input.confirmActiveWorkOrders) {
      data.confirmActiveWorkOrders = true;
    }
    const body: Record<string, unknown> = {
      commandId: input.commandId,
      type: input.type,
      expectedVersion: input.expectedVersion,
      reason: input.reason || undefined,
    };
    if (Object.keys(data).length > 0) {
      body.data = data;
    }
    return client.post(
      `/api/v1/operator/incidents/${encodeURIComponent(input.incidentId)}/commands`,
      incidentSchema,
      body,
    );
  },
};
