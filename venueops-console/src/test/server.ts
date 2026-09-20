import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';

import type { Incident } from '@/features/incidents/domain/incident';
import type { WeatherRecommendation } from '@/features/weather/domain/recommendation';
import { applyIncidentCommandSideEffects } from './applyIncidentCommandSideEffects';
import { catalogAttractions, lightningIncident, lightningIncidentActivity, toOperatorAttraction } from './fixtures';
import { calmDashboard } from '@/features/dashboard/test/fixtures';
import { maintenanceHandlers } from './maintenanceHandlers';
import { flowHandlers } from './flowHandlers';
import { resetMaintenanceStores } from './maintenanceStore';
import { resetFlowStores } from './flowStore';

export const weatherInbox: WeatherRecommendation[] = [];
export const incidentStore: IncidentRecord[] = [];

type IncidentRecord = typeof lightningIncident;

export function resetWeatherInbox() {
  weatherInbox.splice(0, weatherInbox.length);
}

export function resetIncidents() {
  incidentStore.splice(0, incidentStore.length);
}

export { resetMaintenanceStores, resetFlowStores };

export const handlers = [
  http.get('/api/v1/operator/dashboard', () => HttpResponse.json(calmDashboard)),
  http.get('/api/v1/attractions', () => HttpResponse.json(catalogAttractions)),
  http.get('/api/v1/operator/attractions/:id', ({ params }) => {
    const attraction = catalogAttractions.find((item) => item.id === params.id);
    if (!attraction) {
      return HttpResponse.json(
        {
          title: 'Attraction not found',
          status: 404,
          detail: `Unknown attraction: ${params.id}`,
          code: 'ATTRACTION_NOT_FOUND',
        },
        { status: 404 },
      );
    }
    return HttpResponse.json(toOperatorAttraction(attraction));
  }),
  http.get('/api/v1/operator/attractions/:id/activity', ({ params }) => {
    const attraction = catalogAttractions.find((item) => item.id === params.id);
    if (!attraction) {
      return HttpResponse.json(
        {
          title: 'Attraction not found',
          status: 404,
          detail: `Unknown attraction: ${params.id}`,
          code: 'ATTRACTION_NOT_FOUND',
        },
        { status: 404 },
      );
    }
    return HttpResponse.json([]);
  }),
  http.get('/api/v1/operator/weather/recommendations', () => HttpResponse.json(weatherInbox)),
  http.get('/api/v1/operator/incidents', () => HttpResponse.json(incidentStore)),
  http.get('/api/v1/operator/incidents/:id', ({ params }) => {
    const incident = incidentStore.find((item) => item.id === params.id);
    if (!incident) {
      return HttpResponse.json(
        { title: 'Incident not found', status: 404, detail: `Unknown incident: ${params.id}` },
        { status: 404 },
      );
    }
    return HttpResponse.json(incident);
  }),
  http.get('/api/v1/operator/incidents/:id/activity', ({ params }) => {
    if (!incidentStore.some((item) => item.id === params.id)) {
      return HttpResponse.json(
        { title: 'Incident not found', status: 404, detail: `Unknown incident: ${params.id}` },
        { status: 404 },
      );
    }
    return HttpResponse.json(
      params.id === lightningIncidentActivity.incidentId ? [lightningIncidentActivity] : [],
    );
  }),
  http.post('/api/v1/operator/incidents/:id/commands', async ({ params, request }) => {
    const incident = incidentStore.find((item) => item.id === params.id);
    if (!incident) {
      return HttpResponse.json(
        { title: 'Incident not found', status: 404, detail: `Unknown incident: ${params.id}` },
        { status: 404 },
      );
    }
    const body = (await request.json()) as {
      type: string;
      expectedVersion: number;
      guestTitle?: string;
      guestMessage?: string;
      reason?: string;
      assignee?: string;
      severity?: Incident['severity'];
      attractionId?: string;
    };
    if (body.expectedVersion !== incident.version) {
      return HttpResponse.json(
        {
          title: 'Stale version',
          status: 409,
          code: 'STALE_VERSION',
          expectedVersion: body.expectedVersion,
          actualVersion: incident.version,
        },
        { status: 409 },
      );
    }
    const next = applyIncidentCommandSideEffects(incident, body, '2026-09-01T16:20:00Z');
    const index = incidentStore.findIndex((item) => item.id === incident.id);
    incidentStore[index] = next;
    return HttpResponse.json(next);
  }),
  http.post('/api/v1/operator/incidents', async ({ request }) => {
    const body = (await request.json()) as {
      title: string;
      type: string;
      severity: string;
      internalDescription?: string;
      attractionIds?: string[];
    };
    const created: Incident = {
      id: 'inc-weather-1',
      title: body.title,
      type: body.type as Incident['type'],
      severity: body.severity as Incident['severity'],
      status: 'REPORTED',
      internalDescription: body.internalDescription ?? null,
      assignedTo: null,
      guestAdvisoryPublished: false,
      guestTitle: null,
      guestMessage: null,
      attractionIds: body.attractionIds ?? [],
      createdAt: '2026-09-01T16:10:00Z',
      updatedAt: '2026-09-01T16:10:00Z',
      version: 1,
    };
    incidentStore.unshift(created);
    return HttpResponse.json(created, { status: 201 });
  }),
  http.post('/api/v1/operator/weather/recommendations/:id/commands', async ({ params, request }) => {
    const body = (await request.json()) as {
      type: string;
      expectedVersion: number;
      incidentId?: string;
    };
    const current = weatherInbox.find((item) => item.id === params.id);
    if (!current) {
      return HttpResponse.json(
        {
          title: 'Weather recommendation not found',
          status: 404,
          detail: `Unknown recommendation: ${params.id}`,
          code: 'WEATHER_RECOMMENDATION_NOT_FOUND',
        },
        { status: 404 },
      );
    }

    const next: WeatherRecommendation = {
      ...current,
      version: current.version + 1,
      updatedAt: '2026-09-01T16:05:00Z',
      operatorStatus:
        body.type === 'ACKNOWLEDGE'
          ? 'ACKNOWLEDGED'
          : body.type === 'DISMISS'
            ? 'DISMISSED'
            : 'LINKED',
      linkedIncidentId:
        body.type === 'LINK_INCIDENT' ? (body.incidentId ?? 'inc-weather-1') : current.linkedIncidentId,
    };
    const index = weatherInbox.findIndex((item) => item.id === current.id);
    weatherInbox[index] = next;
    return HttpResponse.json(next);
  }),
  ...maintenanceHandlers,
  ...flowHandlers,
];

export const server = setupServer(...handlers);
