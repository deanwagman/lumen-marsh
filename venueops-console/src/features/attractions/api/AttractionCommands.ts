import type { AttractionCommandType } from '@/features/attractions/domain/commands';
import type { ApiClient } from '@/shared/api/client';

import { operatorAttractionSchema } from './attractionSchema';

export type AttractionCommandInput = {
  attractionId: string;
  commandId: string;
  type: AttractionCommandType;
  expectedVersion: number;
  reason?: string;
  waitMinutes?: number;
};

export const AttractionCommands = {
  execute(client: ApiClient, input: AttractionCommandInput) {
    const body: Record<string, unknown> = {
      commandId: input.commandId,
      type: input.type,
      expectedVersion: input.expectedVersion,
    };

    if (input.reason) {
      body.reason = input.reason;
    }

    if (input.waitMinutes !== undefined) {
      body.data = { waitMinutes: input.waitMinutes };
    }

    return client.post(
      `/api/v1/operator/attractions/${encodeURIComponent(input.attractionId)}/commands`,
      operatorAttractionSchema,
      body,
    );
  },
};
