import { http, HttpResponse } from 'msw';

import {
  acceptRecommendation,
  activityFor,
  applyWorkOrderCommand,
  dismissRecommendation,
  filterWorkOrders,
  maintenanceAssetStore,
  maintenanceRecommendationStore,
  maintenanceWorkOrderStore,
  page,
  problem,
  processedMaintenanceCommands,
} from '@/test/maintenanceStore';

export const maintenanceHandlers = [
  http.get('/api/v1/operator/maintenance/recommendations', () =>
    HttpResponse.json(maintenanceRecommendationStore),
  ),
  http.get('/api/v1/operator/maintenance/recommendations/:id', ({ params }) => {
    const recommendation = maintenanceRecommendationStore.find(
      (item) => item.recommendationId === params.id,
    );
    if (!recommendation) {
      return HttpResponse.json(
        problem('MAINTENANCE_NOT_FOUND', `Unknown recommendation: ${params.id}`, 404),
        { status: 404 },
      );
    }
    return HttpResponse.json(recommendation);
  }),
  http.post('/api/v1/operator/maintenance/recommendations/:id/commands', async ({ params, request }) => {
    const recommendation = maintenanceRecommendationStore.find(
      (item) => item.recommendationId === params.id,
    );
    if (!recommendation) {
      return HttpResponse.json(
        problem('MAINTENANCE_NOT_FOUND', `Unknown recommendation: ${params.id}`, 404),
        { status: 404 },
      );
    }
    const body = (await request.json()) as {
      commandId: string;
      type: 'ACCEPT' | 'DISMISS';
      expectedVersion: number;
      reason?: string;
    };
    const processed = processedMaintenanceCommands.get(body.commandId);
    if (processed?.recommendationId && processed.recommendationId !== recommendation.recommendationId) {
      return HttpResponse.json(
        problem('DUPLICATE_COMMAND', 'Duplicate command', 409, {
          commandId: body.commandId,
          existingAggregateId: processed.recommendationId,
          requestedAggregateId: recommendation.recommendationId,
        }),
        { status: 409 },
      );
    }
    if (processed?.recommendationId === recommendation.recommendationId) {
      return HttpResponse.json(processed.body as Record<string, unknown>);
    }
    if (body.expectedVersion !== recommendation.version) {
      return HttpResponse.json(
        problem('STALE_VERSION', 'Stale version', 409, {
          expectedVersion: body.expectedVersion,
          currentVersion: recommendation.version,
          actualVersion: recommendation.version,
        }),
        { status: 409 },
      );
    }
    if (recommendation.status !== 'PENDING_REVIEW' && body.type === 'DISMISS') {
      return HttpResponse.json(
        problem('INVALID_TRANSITION', 'Invalid recommendation transition', 409, {
          currentStatus: recommendation.status,
          command: body.type,
        }),
        { status: 409 },
      );
    }
    const next =
      body.type === 'ACCEPT' ? acceptRecommendation(recommendation) : dismissRecommendation(recommendation);
    processedMaintenanceCommands.set(body.commandId, {
      recommendationId: next.recommendationId,
      body: next,
    });
    return HttpResponse.json(next);
  }),
  http.get('/api/v1/operator/maintenance/work-orders', ({ request }) => {
    const url = new URL(request.url);
    const filtered = filterWorkOrders(maintenanceWorkOrderStore, url.searchParams);
    const pageNumber = Number(url.searchParams.get('page') ?? 0);
    const size = Number(url.searchParams.get('size') ?? 50);
    return HttpResponse.json(page(filtered, pageNumber, size));
  }),
  http.get('/api/v1/operator/maintenance/work-orders/:id', ({ params }) => {
    const workOrder = maintenanceWorkOrderStore.find((item) => item.id === params.id);
    if (!workOrder) {
      return HttpResponse.json(
        problem('MAINTENANCE_NOT_FOUND', `Unknown work order: ${params.id}`, 404),
        { status: 404 },
      );
    }
    return HttpResponse.json(workOrder);
  }),
  http.get('/api/v1/operator/maintenance/work-orders/:id/activity', ({ params }) => {
    const workOrder = maintenanceWorkOrderStore.find((item) => item.id === params.id);
    if (!workOrder) {
      return HttpResponse.json(
        problem('MAINTENANCE_NOT_FOUND', `Unknown work order: ${params.id}`, 404),
        { status: 404 },
      );
    }
    return HttpResponse.json(activityFor(String(params.id)));
  }),
  http.post('/api/v1/operator/maintenance/work-orders/:id/commands', async ({ params, request }) => {
    const workOrder = maintenanceWorkOrderStore.find((item) => item.id === params.id);
    if (!workOrder) {
      return HttpResponse.json(
        problem('MAINTENANCE_NOT_FOUND', `Unknown work order: ${params.id}`, 404),
        { status: 404 },
      );
    }
    const body = (await request.json()) as {
      commandId: string;
      type: string;
      expectedVersion: number;
      reason?: string;
      data?: Record<string, unknown>;
    };
    const result = applyWorkOrderCommand(workOrder, body);
    if (!result.ok) {
      return HttpResponse.json(result.error, { status: result.status });
    }
    return HttpResponse.json({ ...result.response, replay: result.replay });
  }),
  http.get('/api/v1/operator/maintenance/assets', ({ request }) => {
    const url = new URL(request.url);
    const attractionId = url.searchParams.get('attractionId');
    const serviceStatus = url.searchParams.get('serviceStatus');
    const assetType = url.searchParams.get('assetType');
    const criticality = url.searchParams.get('criticality');
    const filtered = maintenanceAssetStore.filter((asset) => {
      if (attractionId && asset.attractionId !== attractionId) return false;
      if (serviceStatus && asset.serviceStatus !== serviceStatus) return false;
      if (assetType && asset.assetType !== assetType) return false;
      if (criticality && asset.criticality !== criticality) return false;
      return true;
    });
    return HttpResponse.json(
      page(filtered, Number(url.searchParams.get('page') ?? 0), Number(url.searchParams.get('size') ?? 50)),
    );
  }),
  http.get('/api/v1/operator/maintenance/assets/:id', ({ params }) => {
    const asset = maintenanceAssetStore.find((item) => item.id === params.id);
    if (!asset) {
      return HttpResponse.json(
        problem('MAINTENANCE_NOT_FOUND', `Unknown asset: ${params.id}`, 404),
        { status: 404 },
      );
    }
    return HttpResponse.json(asset);
  }),
  http.get('/api/v1/operator/maintenance/assets/:id/work-orders', ({ params, request }) => {
    const asset = maintenanceAssetStore.find((item) => item.id === params.id);
    if (!asset) {
      return HttpResponse.json(
        problem('MAINTENANCE_NOT_FOUND', `Unknown asset: ${params.id}`, 404),
        { status: 404 },
      );
    }
    const url = new URL(request.url);
    const items = maintenanceWorkOrderStore.filter((item) => item.asset.id === asset.id);
    return HttpResponse.json(
      page(items, Number(url.searchParams.get('page') ?? 0), Number(url.searchParams.get('size') ?? 50)),
    );
  }),
];
