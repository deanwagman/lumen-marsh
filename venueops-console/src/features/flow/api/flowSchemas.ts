import { z } from 'zod';

import {
  forecastConfidences,
  flowRecommendationSeverities,
  flowRecommendationStatuses,
  flowRecommendationTypes,
  queueFreshness,
  queueTrends,
  type FlowActivity,
  type FlowAttractionCard,
  type FlowAttractionDetail,
  type FlowOverview,
  type FlowRecommendation,
  type PageResult,
  type QueueForecast,
  type QueueProjection,
} from '@/features/flow/domain/flow';

const nullableString = z.string().nullable().optional();
const nullableNumber = z.number().nullable().optional();

const attractionCardSchema = z
  .object({
    attractionId: z.string(),
    displayName: z.string(),
    status: z.string(),
    postedWaitMinutes: nullableNumber,
    calculatedWaitMinutes: nullableNumber,
    predictedWaitMinutes15: nullableNumber,
    predictedWaitMinutes30: nullableNumber,
    predictedWaitMinutes60: nullableNumber,
    queueLength: nullableNumber,
    arrivalsPerMinute: nullableNumber,
    throughputPerMinute: nullableNumber,
    operatingCapacityPercent: nullableNumber,
    trend: z.enum(queueTrends).nullable().optional(),
    freshness: z.enum(queueFreshness).nullable().optional(),
    observedAt: nullableString,
    simulated: z.boolean(),
  })
  .transform(
    (value): FlowAttractionCard => ({
      attractionId: value.attractionId,
      displayName: value.displayName,
      status: value.status,
      postedWaitMinutes: value.postedWaitMinutes ?? null,
      calculatedWaitMinutes: value.calculatedWaitMinutes ?? null,
      predictedWaitMinutes15: value.predictedWaitMinutes15 ?? null,
      predictedWaitMinutes30: value.predictedWaitMinutes30 ?? null,
      predictedWaitMinutes60: value.predictedWaitMinutes60 ?? null,
      queueLength: value.queueLength ?? null,
      arrivalsPerMinute: value.arrivalsPerMinute ?? null,
      throughputPerMinute: value.throughputPerMinute ?? null,
      operatingCapacityPercent: value.operatingCapacityPercent ?? null,
      trend: value.trend ?? null,
      freshness: value.freshness ?? null,
      observedAt: value.observedAt ?? null,
      simulated: value.simulated,
    }),
  );

export const flowOverviewSchema: z.ZodType<FlowOverview> = z
  .object({
    guestsInQueues: z.number(),
    risingWaitCount: z.number(),
    staleAttractionCount: z.number(),
    pendingRecommendationCount: z.number(),
    parkCapacityPercent: z.number(),
    lastSynchronizedAt: nullableString,
    attractions: z.array(attractionCardSchema),
    simulated: z.boolean(),
  })
  .transform((value) => ({
    ...value,
    lastSynchronizedAt: value.lastSynchronizedAt ?? null,
  }));

const projectionSchema = z
  .object({
    attractionId: z.string(),
    observationId: z.string(),
    queueLength: z.number(),
    arrivalsPerMinute: z.number(),
    throughputPerMinute: z.number(),
    calculatedWaitMinutes: z.number(),
    postedWaitMinutes: z.number(),
    operatingCapacityPercent: z.number(),
    trend: z.enum(queueTrends),
    freshness: z.enum(queueFreshness),
    observedAt: z.string(),
    receivedAt: z.string(),
    updatedAt: z.string(),
    version: z.number(),
    simulated: z.boolean(),
  })
  .transform((value): QueueProjection => value);

const forecastSchema = z
  .object({
    forecastId: z.string(),
    attractionId: z.string(),
    generatedAt: z.string(),
    basedOnObservationId: z.string(),
    horizonMinutes: z.number(),
    predictedQueueLength: z.number(),
    predictedWaitMinutes: z.number(),
    confidence: z.enum(forecastConfidences),
    assumptions: z.array(z.string()),
    explanation: z.string(),
    simulated: z.boolean(),
  })
  .transform((value): QueueForecast => value);

export const flowAttractionDetailSchema: z.ZodType<FlowAttractionDetail> = z
  .object({
    attractionId: z.string(),
    displayName: z.string(),
    status: z.string(),
    postedWaitMinutes: nullableNumber,
    projection: projectionSchema.nullable().optional(),
    forecasts: z.array(forecastSchema),
    simulated: z.boolean(),
  })
  .transform((value) => ({
    attractionId: value.attractionId,
    displayName: value.displayName,
    status: value.status,
    postedWaitMinutes: value.postedWaitMinutes ?? null,
    projection: value.projection ?? null,
    forecasts: value.forecasts,
    simulated: value.simulated,
  }));

const recommendationSnapshotSchema = z.object({
  recommendationId: z.string(),
  type: z.enum(flowRecommendationTypes),
  status: z.enum(flowRecommendationStatuses),
  severity: z.enum(flowRecommendationSeverities),
  sourceAttractionId: nullableString,
  affectedAttractionIds: z.array(z.string()),
  recommendedDestinationIds: z.array(z.string()),
  summary: z.string(),
  explanation: z.string(),
  guestMessage: nullableString,
  expiresAt: z.string(),
  relatedIncidentId: nullableString,
  relatedWorkOrderId: nullableString,
  version: z.number(),
  createdAt: z.string(),
  updatedAt: z.string(),
  simulated: z.boolean(),
});

export const flowRecommendationSchema: z.ZodType<FlowRecommendation> = z
  .object({
    recommendationId: z.string(),
    recommendation: recommendationSnapshotSchema,
    simulated: z.boolean().optional(),
  })
  .transform((value) => ({
    recommendationId: value.recommendation.recommendationId,
    type: value.recommendation.type,
    status: value.recommendation.status,
    severity: value.recommendation.severity,
    sourceAttractionId: value.recommendation.sourceAttractionId ?? null,
    affectedAttractionIds: value.recommendation.affectedAttractionIds,
    recommendedDestinationIds: value.recommendation.recommendedDestinationIds,
    summary: value.recommendation.summary,
    explanation: value.recommendation.explanation,
    guestMessage: value.recommendation.guestMessage ?? null,
    expiresAt: value.recommendation.expiresAt,
    relatedIncidentId: value.recommendation.relatedIncidentId ?? null,
    relatedWorkOrderId: value.recommendation.relatedWorkOrderId ?? null,
    version: value.recommendation.version,
    createdAt: value.recommendation.createdAt,
    updatedAt: value.recommendation.updatedAt,
    simulated: value.recommendation.simulated,
  }));

export const flowRecommendationPageSchema: z.ZodType<PageResult<FlowRecommendation>> = z
  .object({
    items: z.array(flowRecommendationSchema),
    page: z.number(),
    size: z.number(),
    total: z.number(),
  })
  .transform((value) => value);

export const flowActivityListSchema: z.ZodType<FlowActivity[]> = z.array(
  z
    .object({
      id: z.string(),
      sequence: z.number(),
      eventType: z.string(),
      fromStatus: nullableString,
      toStatus: z.string(),
      actor: z.string(),
      actorType: z.string(),
      reason: nullableString,
      commandId: nullableString,
      correlationId: nullableString,
      occurredAt: z.string(),
      resultingVersion: z.number(),
    })
    .transform(
      (value): FlowActivity => ({
        id: value.id,
        sequence: value.sequence,
        eventType: value.eventType,
        fromStatus: value.fromStatus ?? null,
        toStatus: value.toStatus,
        actor: value.actor,
        actorType: value.actorType,
        reason: value.reason ?? null,
        commandId: value.commandId ?? null,
        correlationId: value.correlationId ?? null,
        occurredAt: value.occurredAt,
        resultingVersion: value.resultingVersion,
      }),
    ),
);
