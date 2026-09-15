import type { ApiClient } from '@/shared/api/client';
import { newCorrelationId } from '@/features/maintenance/domain/maintenance';
import type {
  MaintenanceRecommendationCommand,
  MaintenanceWorkOrderCommand,
} from '@/features/maintenance/domain/maintenance';

import {
  maintenanceCommandResponseSchema,
  reliabilityRecommendationSchema,
} from './maintenanceSchemas';

export type MaintenanceWorkOrderCommandInput = {
  workOrderId: string;
  commandId: string;
  type: MaintenanceWorkOrderCommand;
  expectedVersion: number;
  reason?: string;
  data?: Record<string, unknown>;
};

export type MaintenanceRecommendationCommandInput = {
  recommendationId: string;
  commandId: string;
  type: MaintenanceRecommendationCommand;
  expectedVersion: number;
  reason?: string;
};

export const MaintenanceCommands = {
  executeWorkOrder(client: ApiClient, input: MaintenanceWorkOrderCommandInput) {
    const body: Record<string, unknown> = {
      commandId: input.commandId,
      type: input.type,
      expectedVersion: input.expectedVersion,
    };
    if (input.reason) {
      body.reason = input.reason;
    }
    if (input.data && Object.keys(input.data).length > 0) {
      body.data = input.data;
    }
    return client.post(
      `/api/v1/operator/maintenance/work-orders/${encodeURIComponent(input.workOrderId)}/commands`,
      maintenanceCommandResponseSchema,
      body,
      { headers: { 'X-Correlation-Id': newCorrelationId() } },
    );
  },

  executeRecommendation(client: ApiClient, input: MaintenanceRecommendationCommandInput) {
    const body: Record<string, unknown> = {
      commandId: input.commandId,
      type: input.type,
      expectedVersion: input.expectedVersion,
    };
    if (input.reason) {
      body.reason = input.reason;
    }
    return client.post(
      `/api/v1/operator/maintenance/recommendations/${encodeURIComponent(input.recommendationId)}/commands`,
      reliabilityRecommendationSchema,
      body,
      { headers: { 'X-Correlation-Id': newCorrelationId() } },
    );
  },
};
