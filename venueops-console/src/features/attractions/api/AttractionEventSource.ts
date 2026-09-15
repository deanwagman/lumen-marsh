import type { QueryClient } from '@tanstack/react-query';

import { environment } from '@/config/environment';
import { STREAM_STALE_AFTER_MS } from '@/features/attractions/domain/streamHealth';
import {
  applyIncidentUpdate,
  replaceIncidentList,
} from '@/features/incidents/api/IncidentQueries';
import {
  incidentSnapshotListSchema,
  incidentSseUpdateSchema,
} from '@/features/incidents/api/incidentSchema';
import {
  applyWeatherRecommendation,
  replaceWeatherRecommendations,
} from '@/features/weather/api/WeatherRecommendationQueries';
import {
  weatherRecommendationListSchema,
  weatherRecommendationSseUpdateSchema,
} from '@/features/weather/api/weatherRecommendationSchema';
import {
  applyWorkOrderSummaryUpdate,
  replaceWorkOrderSummaries,
} from '@/features/maintenance/api/maintenanceCache';
import {
  maintenanceSseUpdateSchema,
  maintenanceWorkOrderSnapshotListSchema,
} from '@/features/maintenance/api/maintenanceSchemas';
import { resolveApiUrl } from '@/shared/api/client';

import {
  dashboardStreamEvents,
  scheduleDashboardInvalidation,
} from '@/features/dashboard/api/dashboardInvalidation';

import {
  applyAttractionOperationalUpdate,
  AttractionQueries,
  replaceAttractionList,
  setStreamHealth,
} from './AttractionQueries';
import { attractionListSchema, attractionSseUpdateSchema } from './attractionSchema';

export function handleAttractionStreamEvent(
  queryClient: QueryClient,
  eventName: string,
  data: string,
  capabilities: { maintenanceRead?: boolean } = {},
): void {
  const maintenanceRead = capabilities.maintenanceRead !== false;
  if (
    !maintenanceRead &&
    (eventName === 'maintenance.work-orders.snapshot' || eventName === 'maintenance.work-order.updated')
  ) {
    return;
  }
  if (eventName === 'attractions.snapshot') {
    const attractions = attractionListSchema.parse(JSON.parse(data) as unknown);
    replaceAttractionList(queryClient, attractions);
  } else if (eventName === 'attraction.updated') {
    const update = attractionSseUpdateSchema.parse(JSON.parse(data) as unknown);
    applyAttractionOperationalUpdate(queryClient, update.attraction);
    void queryClient.invalidateQueries({
      queryKey: AttractionQueries.activity(update.attraction.id),
    });
  } else if (eventName === 'weather.recommendations.snapshot') {
    const recommendations = weatherRecommendationListSchema.parse(JSON.parse(data) as unknown);
    replaceWeatherRecommendations(queryClient, recommendations);
  } else if (
    eventName === 'weather.recommendation.updated' ||
    eventName === 'weather.recommendation.cleared'
  ) {
    const update = weatherRecommendationSseUpdateSchema.parse(JSON.parse(data) as unknown);
    applyWeatherRecommendation(queryClient, update.recommendation);
  } else if (eventName === 'incidents.snapshot') {
    const incidents = incidentSnapshotListSchema.parse(JSON.parse(data) as unknown);
    replaceIncidentList(queryClient, incidents);
  } else if (
    eventName === 'incident.reported' ||
    eventName === 'incident.updated' ||
    eventName === 'incident.resolved'
  ) {
    const update = incidentSseUpdateSchema.parse(JSON.parse(data) as unknown);
    applyIncidentUpdate(queryClient, update.incident);
  } else if (eventName === 'maintenance.work-orders.snapshot') {
    const snapshots = maintenanceWorkOrderSnapshotListSchema.parse(JSON.parse(data) as unknown);
    replaceWorkOrderSummaries(queryClient, snapshots);
  } else if (eventName === 'maintenance.work-order.updated') {
    const update = maintenanceSseUpdateSchema.parse(JSON.parse(data) as unknown);
    applyWorkOrderSummaryUpdate(queryClient, update.workOrder, {
      incidentId: update.incidentId ?? null,
    });
  }

  if (dashboardStreamEvents.has(eventName)) {
    scheduleDashboardInvalidation(queryClient);
  }
}

let reconnectImpl: (() => void) | null = null;

export function reconnectAttractionStream(): void {
  reconnectImpl?.();
}

export const AttractionEventSource = {
  applyUpdate: applyAttractionOperationalUpdate,
  reconnect: reconnectAttractionStream,
  connect(
    queryClient: QueryClient,
    options: {
      getAccessToken: () => string | undefined;
      fetchImpl?: typeof fetch;
      onUnauthorized?: () => void;
      onForbidden?: () => void;
      reconnectDelayMs?: number;
      includeMaintenanceEvents?: boolean;
    },
  ): () => void {
    let staleTimer: ReturnType<typeof setTimeout> | null = null;
    let recreateTimer: ReturnType<typeof setTimeout> | null = null;
    let disposed = false;
    let controller: AbortController | null = null;
    let generation = 0;
    const fetchImpl = options.fetchImpl ?? fetch;

    const clearStaleTimer = () => {
      if (staleTimer !== null) {
        clearTimeout(staleTimer);
        staleTimer = null;
      }
    };

    const clearRecreateTimer = () => {
      if (recreateTimer !== null) {
        clearTimeout(recreateTimer);
        recreateTimer = null;
      }
    };

    const scheduleStale = () => {
      if (staleTimer !== null) {
        return;
      }

      staleTimer = setTimeout(() => {
        staleTimer = null;
        if (!disposed) {
          setStreamHealth(queryClient, { type: 'stale' });
        }
      }, STREAM_STALE_AFTER_MS);
    };

    const applyEvent = (eventName: string, data: string) => {
      try {
        handleAttractionStreamEvent(queryClient, eventName, data, {
          maintenanceRead: options.includeMaintenanceEvents !== false,
        });
        clearStaleTimer();
        setStreamHealth(queryClient, { type: 'message', at: Date.now() });
      } catch (error) {
        console.error(`Failed to apply ${eventName}`, error);
      }
    };

    const scheduleReconnect = () => {
      if (disposed) return;
      setStreamHealth(queryClient, { type: 'error' });
      scheduleStale();
      clearRecreateTimer();
      recreateTimer = setTimeout(() => {
        recreateTimer = null;
        openSource();
      }, options.reconnectDelayMs ?? 1_000);
    };

    const openSource = () => {
      controller?.abort();
      const currentGeneration = ++generation;
      controller = new AbortController();
      setStreamHealth(queryClient, { type: 'connect' });
      const token = options.getAccessToken();
      if (!token) {
        disposed = true;
        options.onUnauthorized?.();
        return;
      }

      void fetchImpl(resolveApiUrl(environment.apiBaseUrl, '/api/v1/operator/events'), {
        method: 'GET',
        headers: {
          Accept: 'text/event-stream',
          Authorization: `Bearer ${token}`,
        },
        signal: controller.signal,
      })
        .then(async (response) => {
          if (response.status === 401) {
            disposed = true;
            options.onUnauthorized?.();
            return;
          }
          if (response.status === 403) {
            disposed = true;
            options.onForbidden?.();
            return;
          }
          if (!response.ok || !response.body) {
            throw new Error(`Operator event stream failed with status ${response.status}`);
          }

          clearStaleTimer();
          setStreamHealth(queryClient, { type: 'open' });
          await consumeEventStream(response.body, applyEvent);
          if (!disposed && currentGeneration === generation) scheduleReconnect();
        })
        .catch((error: unknown) => {
          if (disposed || currentGeneration !== generation) return;
          if (error instanceof DOMException && error.name === 'AbortError') return;
          scheduleReconnect();
        });
    };

    reconnectImpl = () => {
      if (disposed) {
        return;
      }
      clearStaleTimer();
      clearRecreateTimer();
      openSource();
    };

    openSource();

    return () => {
      disposed = true;
      reconnectImpl = null;
      clearStaleTimer();
      clearRecreateTimer();
      controller?.abort();
      controller = null;
    };
  },
};

async function consumeEventStream(
  stream: ReadableStream<Uint8Array>,
  onEvent: (eventName: string, data: string) => void,
): Promise<void> {
  const reader = stream.getReader();
  const decoder = new TextDecoder();
  let buffer = '';

  while (true) {
    const { done, value } = await reader.read();
    buffer += decoder.decode(value, { stream: !done }).replace(/\r\n/g, '\n');
    let boundary = buffer.indexOf('\n\n');
    while (boundary >= 0) {
      const block = buffer.slice(0, boundary);
      buffer = buffer.slice(boundary + 2);
      let eventName = 'message';
      const data: string[] = [];
      for (const line of block.split('\n')) {
        if (line.startsWith('event:')) eventName = line.slice(6).trimStart();
        if (line.startsWith('data:')) data.push(line.slice(5).trimStart());
      }
      if (data.length > 0) onEvent(eventName, data.join('\n'));
      boundary = buffer.indexOf('\n\n');
    }
    if (done) return;
  }
}
