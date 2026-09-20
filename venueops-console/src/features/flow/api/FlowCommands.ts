import type { ApiClient } from '@/shared/api/client';
import type { FlowRecommendationCommand } from '@/features/flow/domain/flow';
import { newCorrelationId } from '@/features/flow/domain/flow';

import { flowRecommendationSchema } from './flowSchemas';

export type FlowCommandInput = {
  recommendationId: string;
  commandId: string;
  type: FlowRecommendationCommand;
  expectedVersion: number;
  reason?: string;
  guestMessage?: string;
};

export const FlowCommands = {
  execute(client: ApiClient, input: FlowCommandInput) {
    const body: Record<string, unknown> = {
      commandId: input.commandId,
      type: input.type,
      expectedVersion: input.expectedVersion,
    };
    if (input.reason) {
      body.reason = input.reason;
    }
    if (input.guestMessage) {
      body.data = { guestMessage: input.guestMessage };
    }
    return client.post(
      `/api/v1/operator/flow/recommendations/${encodeURIComponent(input.recommendationId)}/commands`,
      flowRecommendationSchema,
      body,
      { headers: { 'X-Correlation-Id': newCorrelationId() } },
    );
  },
};
