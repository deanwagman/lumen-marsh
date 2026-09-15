import { QueryClient } from '@tanstack/react-query';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { catalogAttractions, lightningHoldRecommendation, mangroveRun } from '@/test/fixtures';
import {
  AttractionEventSource,
  handleAttractionStreamEvent,
} from './AttractionEventSource';
import { AttractionQueries } from './AttractionQueries';
import { IncidentQueries } from '@/features/incidents/api/IncidentQueries';
import { WeatherRecommendationQueries } from '@/features/weather/api/WeatherRecommendationQueries';
import { DashboardQueries } from '@/features/dashboard/api/DashboardQueries';
import { MaintenanceQueries } from '@/features/maintenance/api/MaintenanceQueries';
import {
  DASHBOARD_INVALIDATE_DEBOUNCE_MS,
  resetDashboardInvalidation,
} from '@/features/dashboard/api/dashboardInvalidation';

describe('handleAttractionStreamEvent', () => {
  afterEach(() => {
    resetDashboardInvalidation();
  });
  it('replaces the list from an attractions.snapshot event', () => {
    const queryClient = new QueryClient();

    handleAttractionStreamEvent(
      queryClient,
      'attractions.snapshot',
      JSON.stringify(catalogAttractions),
    );

    expect(queryClient.getQueryData(AttractionQueries.list())).toEqual(catalogAttractions);
  });

  it('writes attraction.updated through setQueryData', () => {
    const queryClient = new QueryClient();
    queryClient.setQueryData(AttractionQueries.list(), catalogAttractions);
    queryClient.setQueryData(AttractionQueries.detail('mangrove-run'), {
      id: mangroveRun.id,
      name: mangroveRun.name,
      area: mangroveRun.area,
      type: mangroveRun.type,
      status: mangroveRun.status,
      capacityMode: mangroveRun.capacityMode,
      waitMinutes: mangroveRun.waitMinutes,
      updatedAt: mangroveRun.updatedAt,
      version: mangroveRun.version,
    });

    handleAttractionStreamEvent(
      queryClient,
      'attraction.updated',
      JSON.stringify({
        eventId: 'activity-wait-1',
        eventType: 'WAIT_TIME_CHANGED',
        occurredAt: '2026-08-28T16:05:00Z',
        attraction: {
          id: 'mangrove-run',
          status: 'OPERATING',
          capacityMode: 'NORMAL',
          waitMinutes: 35,
          statusMessage: null,
          updatedAt: '2026-08-28T16:05:00Z',
          version: 1,
        },
      }),
    );

    const list = queryClient.getQueryData(AttractionQueries.list()) as typeof catalogAttractions;
    expect(list.find((item) => item.id === 'mangrove-run')?.waitMinutes).toBe(35);
    expect(queryClient.getQueryData(AttractionQueries.detail('mangrove-run'))).toMatchObject({
      waitMinutes: 35,
      version: 1,
    });
  });

  it('writes weather.recommendation.updated into the weather inbox cache', () => {
    const queryClient = new QueryClient();

    handleAttractionStreamEvent(
      queryClient,
      'weather.recommendation.updated',
      JSON.stringify({
        eventId: 'weather-evt-1',
        eventType: 'UPDATED',
        occurredAt: '2026-09-01T16:00:00Z',
        recommendation: lightningHoldRecommendation,
      }),
    );

    expect(queryClient.getQueryData(WeatherRecommendationQueries.list())).toEqual([
      lightningHoldRecommendation,
    ]);
  });

  it('upserts incident.updated and ignores an older version', () => {
    const queryClient = new QueryClient();
    handleAttractionStreamEvent(
      queryClient,
      'incidents.snapshot',
      JSON.stringify([
        {
          id: 'inc-lightning-1',
          title: 'Lightning activity near western basin',
          type: 'WEATHER',
          severity: 'MAJOR',
          status: 'REPORTED',
          assignedTo: null,
          guestAdvisoryPublished: false,
          guestTitle: null,
          guestMessage: null,
          affectedAttractionIds: ['mangrove-run'],
          updatedAt: '2026-09-01T16:10:00Z',
          version: 2,
        },
      ]),
    );

    handleAttractionStreamEvent(
      queryClient,
      'incident.updated',
      JSON.stringify({
        eventId: 'activity-incident-1',
        eventType: 'INCIDENT_UPDATED',
        occurredAt: '2026-09-01T16:20:00Z',
        incident: {
          id: 'inc-lightning-1',
          title: 'Lightning activity near western basin',
          type: 'WEATHER',
          severity: 'MAJOR',
          status: 'ACKNOWLEDGED',
          assignedTo: null,
          guestAdvisoryPublished: false,
          guestTitle: null,
          guestMessage: null,
          affectedAttractionIds: ['mangrove-run'],
          updatedAt: '2026-09-01T16:20:00Z',
          version: 1,
        },
      }),
    );

    const list = queryClient.getQueryData(IncidentQueries.list()) as Array<{ status: string; version: number }>;
    expect(list[0]).toMatchObject({ status: 'REPORTED', version: 2 });
  });

  it('does not let an older incident snapshot replace a newer streamed update', () => {
    const queryClient = new QueryClient();
    handleAttractionStreamEvent(
      queryClient,
      'incident.updated',
      JSON.stringify({
        eventId: 'activity-incident-2',
        eventType: 'INCIDENT_UPDATED',
        occurredAt: '2026-09-01T16:20:00Z',
        incident: {
          id: 'inc-lightning-1',
          title: 'Lightning activity near western basin',
          type: 'WEATHER',
          severity: 'MAJOR',
          status: 'ACKNOWLEDGED',
          assignedTo: null,
          guestAdvisoryPublished: false,
          guestTitle: null,
          guestMessage: null,
          affectedAttractionIds: ['mangrove-run'],
          updatedAt: '2026-09-01T16:20:00Z',
          version: 3,
        },
      }),
    );

    handleAttractionStreamEvent(
      queryClient,
      'incidents.snapshot',
      JSON.stringify([
        {
          id: 'inc-lightning-1',
          title: 'Lightning activity near western basin',
          type: 'WEATHER',
          severity: 'MAJOR',
          status: 'REPORTED',
          assignedTo: null,
          guestAdvisoryPublished: false,
          guestTitle: null,
          guestMessage: null,
          affectedAttractionIds: ['mangrove-run'],
          updatedAt: '2026-09-01T16:10:00Z',
          version: 2,
        },
      ]),
    );

    const list = queryClient.getQueryData(IncidentQueries.list()) as Array<{
      status: string;
      version: number;
    }>;
    expect(list[0]).toMatchObject({ status: 'ACKNOWLEDGED', version: 3 });
  });

  it('debounces dashboard invalidation across a burst of relevant events', () => {
    vi.useFakeTimers();
    const queryClient = new QueryClient();
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries');

    handleAttractionStreamEvent(
      queryClient,
      'attractions.snapshot',
      JSON.stringify(catalogAttractions),
    );
    handleAttractionStreamEvent(
      queryClient,
      'incident.updated',
      JSON.stringify({
        eventId: 'activity-incident-1',
        eventType: 'INCIDENT_UPDATED',
        occurredAt: '2026-09-01T16:20:00Z',
        incident: {
          id: 'inc-lightning-1',
          title: 'Lightning activity near western basin',
          type: 'WEATHER',
          severity: 'MAJOR',
          status: 'ACKNOWLEDGED',
          assignedTo: null,
          guestAdvisoryPublished: false,
          guestTitle: null,
          guestMessage: null,
          affectedAttractionIds: ['mangrove-run'],
          updatedAt: '2026-09-01T16:20:00Z',
          version: 1,
        },
      }),
    );

    expect(invalidate).not.toHaveBeenCalledWith({ queryKey: DashboardQueries.snapshot() });
    vi.advanceTimersByTime(DASHBOARD_INVALIDATE_DEBOUNCE_MS);
    expect(invalidate).toHaveBeenCalledWith({ queryKey: DashboardQueries.snapshot() });
    vi.useRealTimers();
  });

  it('ignores maintenance events when the subscriber cannot read maintenance', () => {
    const queryClient = new QueryClient();
    handleAttractionStreamEvent(
      queryClient,
      'maintenance.work-orders.snapshot',
      JSON.stringify([
        {
          id: 'd1111111-1111-4111-8111-111111111111',
          workOrderNumber: 'LM-2026-0001',
          status: 'OPEN',
          priority: 'P1',
          version: 2,
          updatedAt: '2026-09-14T18:50:00Z',
        },
      ]),
      { maintenanceRead: false },
    );
    expect(queryClient.getQueryData(MaintenanceQueries.summaries())).toBeUndefined();
  });
});

describe('AttractionEventSource.connect', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it('sends bearer auth and applies streamed events', async () => {
    const queryClient = new QueryClient();
    const encoder = new TextEncoder();
    const stream = new ReadableStream<Uint8Array>({
      start(controller) {
        controller.enqueue(
          encoder.encode(
            `event: attractions.snapshot\ndata: ${JSON.stringify(catalogAttractions)}\n\n`,
          ),
        );
      },
    });
    const fetchImpl = vi.fn().mockResolvedValue(
      new Response(stream, {
        status: 200,
        headers: { 'Content-Type': 'text/event-stream' },
      }),
    );

    const disconnect = AttractionEventSource.connect(queryClient, {
      getAccessToken: () => 'stream-token',
      fetchImpl,
    });
    expect(queryClient.getQueryData(AttractionQueries.stream())).toMatchObject({
      status: 'connecting',
    });

    await vi.waitFor(() => {
      expect(queryClient.getQueryData(AttractionQueries.list())).toEqual(catalogAttractions);
    });
    const [url, request] = fetchImpl.mock.calls[0] as [string, RequestInit];
    expect(url).toContain('/api/v1/operator/events');
    expect(new Headers(request.headers).get('Authorization')).toBe('Bearer stream-token');

    disconnect();
  });

  it('stops reconnecting and expires auth after a 401', async () => {
    vi.useFakeTimers();
    const queryClient = new QueryClient();
    const onUnauthorized = vi.fn();
    const fetchImpl = vi.fn().mockResolvedValue(new Response(null, { status: 401 }));

    AttractionEventSource.connect(queryClient, {
      getAccessToken: () => 'expired-token',
      fetchImpl,
      onUnauthorized,
    });

    await Promise.resolve();
    await Promise.resolve();
    expect(onUnauthorized).toHaveBeenCalledOnce();
    vi.runAllTimers();
    expect(fetchImpl).toHaveBeenCalledOnce();
  });
});
