import { http, HttpResponse } from 'msw';

import type { FlowRecommendation } from '@/features/flow/domain/flow';

import { toApiRecommendation } from '@/features/flow/test/fixtures';
import {
  flowOverviewStore,
  flowRecommendationStore,
  processedFlowCommands,
} from '@/test/flowStore';

function problem(code: string, detail: string, status: number, extra: Record<string, unknown> = {}) {
  return { title: detail, status, detail, code, ...extra };
}

export const flowHandlers = [
  http.get('/api/v1/operator/flow/overview', () => HttpResponse.json(flowOverviewStore.current)),
  http.get('/api/v1/operator/flow/attractions/:id', ({ params }) => {
    const card = flowOverviewStore.current.attractions.find((item) => item.attractionId === params.id);
    if (!card) {
      return HttpResponse.json(problem('ATTRACTION_NOT_FOUND', `Unknown attraction: ${params.id}`, 404), {
        status: 404,
      });
    }
    return HttpResponse.json({
      attractionId: card.attractionId,
      displayName: card.displayName,
      status: card.status,
      postedWaitMinutes: card.postedWaitMinutes,
      projection: card.queueLength == null
        ? null
        : {
            attractionId: card.attractionId,
            observationId: 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb',
            queueLength: card.queueLength,
            arrivalsPerMinute: card.arrivalsPerMinute,
            throughputPerMinute: card.throughputPerMinute,
            calculatedWaitMinutes: card.calculatedWaitMinutes,
            postedWaitMinutes: card.postedWaitMinutes,
            operatingCapacityPercent: card.operatingCapacityPercent,
            trend: card.trend,
            freshness: card.freshness,
            observedAt: card.observedAt,
            receivedAt: card.observedAt,
            updatedAt: card.observedAt,
            version: 1,
            simulated: card.simulated,
          },
      forecasts: [15, 30, 60].map((horizon) => ({
        forecastId: `${horizon}`,
        attractionId: card.attractionId,
        generatedAt: card.observedAt,
        basedOnObservationId: 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb',
        horizonMinutes: horizon,
        predictedQueueLength: card.queueLength,
        predictedWaitMinutes:
          horizon === 15
            ? card.predictedWaitMinutes15
            : horizon === 30
              ? card.predictedWaitMinutes30
              : card.predictedWaitMinutes60,
        confidence: 'MEDIUM',
        assumptions: ['Simulated demonstration data.'],
        explanation: `${card.displayName} wait in ${horizon} minutes is about the predicted value.`,
        simulated: true,
      })),
      simulated: true,
    });
  }),
  http.get('/api/v1/operator/flow/recommendations', ({ request }) => {
    const url = new URL(request.url);
    const status = url.searchParams.get('status');
    const items = flowRecommendationStore.filter((item) => !status || item.status === status);
    return HttpResponse.json({ items: items.map(toApiRecommendation), page: 0, size: 20, total: items.length });
  }),
  http.get('/api/v1/operator/flow/recommendations/:id', ({ params }) => {
    const recommendation = flowRecommendationStore.find((item) => item.recommendationId === params.id);
    if (!recommendation) {
      return HttpResponse.json(problem('FLOW_NOT_FOUND', `Unknown recommendation: ${params.id}`, 404), {
        status: 404,
      });
    }
    return HttpResponse.json(toApiRecommendation(recommendation));
  }),
  http.get('/api/v1/operator/flow/recommendations/:id/activity', () => HttpResponse.json([])),
  http.post('/api/v1/operator/flow/recommendations/:id/commands', async ({ params, request }) => {
    const recommendation = flowRecommendationStore.find((item) => item.recommendationId === params.id);
    if (!recommendation) {
      return HttpResponse.json(problem('FLOW_NOT_FOUND', `Unknown recommendation: ${params.id}`, 404), {
        status: 404,
      });
    }
    const body = (await request.json()) as {
      commandId: string;
      type: 'APPROVE' | 'DISMISS' | 'PUBLISH' | 'WITHDRAW';
      expectedVersion: number;
      reason?: string;
      data?: { guestMessage?: string };
    };
    const processed = processedFlowCommands.get(body.commandId);
    if (processed && processed.recommendationId !== recommendation.recommendationId) {
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
          actualVersion: recommendation.version,
        }),
        { status: 409 },
      );
    }
    const nextStatus =
      body.type === 'APPROVE'
        ? 'APPROVED'
        : body.type === 'DISMISS'
          ? 'DISMISSED'
          : body.type === 'PUBLISH'
            ? 'PUBLISHED'
            : 'WITHDRAWN';
    const next: FlowRecommendation = {
      ...recommendation,
      status: nextStatus,
      version: recommendation.version + 1,
      updatedAt: '2026-09-15T18:35:00Z',
      guestMessage: body.data?.guestMessage ?? recommendation.guestMessage,
    };
    const index = flowRecommendationStore.findIndex(
      (item) => item.recommendationId === recommendation.recommendationId,
    );
    flowRecommendationStore[index] = next;
    const payload = toApiRecommendation(next);
    processedFlowCommands.set(body.commandId, {
      recommendationId: recommendation.recommendationId,
      body: payload,
    });
    return HttpResponse.json(payload);
  }),
];
