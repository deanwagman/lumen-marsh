import type { QueryClient } from '@tanstack/react-query';

import { guestStatusMessages } from '@/features/attractions/domain/commands';
import type { CommandReceipt } from '@/features/attractions/domain/receipt';
import type {
  AttractionOperationalUpdate,
  AttractionSummary,
  OperatorAttraction,
} from '@/features/attractions/domain/attraction';
import {
  defaultStreamHealth,
  reduceStreamHealth,
  type AttractionStreamHealth,
  type StreamHealthEvent,
} from '@/features/attractions/domain/streamHealth';
import type { ApiClient } from '@/shared/api/client';

import {
  attractionActivityListSchema,
  attractionListSchema,
  operatorAttractionSchema,
} from './attractionSchema';

export const AttractionQueries = {
  all: ['attractions'] as const,
  list: () => [...AttractionQueries.all, 'list'] as const,
  detail: (id: string) => [...AttractionQueries.all, 'detail', id] as const,
  activity: (id: string) => [...AttractionQueries.all, 'activity', id] as const,
  stream: () => [...AttractionQueries.all, 'stream'] as const,
  receipt: () => [...AttractionQueries.all, 'receipt'] as const,
};

export function setStreamHealth(
  queryClient: QueryClient,
  event: StreamHealthEvent,
): AttractionStreamHealth {
  const current =
    queryClient.getQueryData<AttractionStreamHealth>(AttractionQueries.stream()) ??
    defaultStreamHealth;
  const next = reduceStreamHealth(current, event);
  queryClient.setQueryData(AttractionQueries.stream(), next);
  return next;
}

export function setCommandReceipt(
  queryClient: QueryClient,
  receipt: CommandReceipt | null,
): void {
  queryClient.setQueryData(AttractionQueries.receipt(), receipt);
}

export function attractionListQuery(client: ApiClient) {
  return {
    queryKey: AttractionQueries.list(),
    queryFn: ({ signal }: { signal: AbortSignal }) =>
      client.get('/api/v1/attractions', attractionListSchema, { signal }),
  };
}

export function attractionDetailQuery(client: ApiClient, attractionId: string) {
  return {
    queryKey: AttractionQueries.detail(attractionId),
    queryFn: ({ signal }: { signal: AbortSignal }) =>
      client.get(
        `/api/v1/operator/attractions/${encodeURIComponent(attractionId)}`,
        operatorAttractionSchema,
        { signal },
      ),
  };
}

export function attractionActivityQuery(client: ApiClient, attractionId: string) {
  return {
    queryKey: AttractionQueries.activity(attractionId),
    queryFn: ({ signal }: { signal: AbortSignal }) =>
      client.get(
        `/api/v1/operator/attractions/${encodeURIComponent(attractionId)}/activity`,
        attractionActivityListSchema,
        { signal },
      ),
  };
}

export function applyOperatorAttraction(
  queryClient: QueryClient,
  attraction: OperatorAttraction,
): void {
  queryClient.setQueryData(AttractionQueries.detail(attraction.id), attraction);
  queryClient.setQueryData<AttractionSummary[]>(AttractionQueries.list(), (current) => {
    if (!current) {
      return current;
    }

    return current.map((item) => {
      if (item.id !== attraction.id || attraction.version < item.version) {
        return item;
      }

      return {
        ...item,
        status: attraction.status,
        capacityMode: attraction.capacityMode,
        waitMinutes: attraction.waitMinutes,
        statusMessage: guestStatusMessages[attraction.status],
        updatedAt: attraction.updatedAt,
        version: attraction.version,
      };
    });
  });
}

export function applyAttractionOperationalUpdate(
  queryClient: QueryClient,
  update: AttractionOperationalUpdate,
): void {
  queryClient.setQueryData<AttractionSummary[]>(AttractionQueries.list(), (current) => {
    if (!current) {
      return current;
    }

    return current.map((attraction) => {
      if (attraction.id !== update.id || update.version < attraction.version) {
        return attraction;
      }

      return {
        ...attraction,
        status: update.status,
        capacityMode: update.capacityMode,
        waitMinutes: update.waitMinutes,
        statusMessage: update.statusMessage,
        updatedAt: update.updatedAt,
        version: update.version,
      };
    });
  });

  queryClient.setQueryData<OperatorAttraction>(
    AttractionQueries.detail(update.id),
    (current) => {
      if (!current || update.version < current.version) {
        return current;
      }

      return {
        ...current,
        status: update.status,
        capacityMode: update.capacityMode,
        waitMinutes: update.waitMinutes,
        updatedAt: update.updatedAt,
        version: update.version,
      };
    },
  );
}

export function replaceAttractionList(
  queryClient: QueryClient,
  attractions: AttractionSummary[],
): void {
  queryClient.setQueryData(AttractionQueries.list(), attractions);

  for (const attraction of attractions) {
    queryClient.setQueryData<OperatorAttraction>(
      AttractionQueries.detail(attraction.id),
      (current) => {
        if (current && current.version > attraction.version) {
          return current;
        }

        return {
          id: attraction.id,
          name: attraction.name,
          area: attraction.area,
          type: attraction.type,
          status: attraction.status,
          capacityMode: attraction.capacityMode,
          waitMinutes: attraction.waitMinutes,
          updatedAt: attraction.updatedAt,
          version: attraction.version,
        };
      },
    );
  }
}
