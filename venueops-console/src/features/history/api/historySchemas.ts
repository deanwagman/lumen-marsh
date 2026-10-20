import { z } from 'zod';

import {
  availabilityClassifications,
  type AnalyticsAnswer,
  type AvailabilityClassification,
  type AvailabilityDetail,
  type AvailabilitySummary,
  type HistoricalDataset,
} from '@/features/history/domain/history';

const nullableString = z.string().nullable().optional();
const classificationSchema = z.enum(availabilityClassifications);

const downtimeSchema = z
  .partialRecord(classificationSchema, z.number())
  .transform((value): Record<AvailabilityClassification, number> => {
    const result = Object.fromEntries(
      availabilityClassifications.map((classification) => [
        classification,
        value[classification] ?? 0,
      ]),
    );
    return result as Record<AvailabilityClassification, number>;
  });

const citationSchema = z
  .object({
    id: z.string(),
    datasetId: z.string(),
    attractionId: z.string(),
    parkDate: z.string(),
    activityId: nullableString,
    commandId: nullableString,
    controlTowerUrl: z.string(),
  })
  .transform((value) => ({
    ...value,
    activityId: value.activityId ?? null,
    commandId: value.commandId ?? null,
  }));

export const historicalDatasetSchema = z
  .object({
    id: z.string(),
    version: z.string(),
    seed: z.number(),
    manifestChecksum: z.string(),
    fromDate: z.string(),
    toDate: z.string(),
    state: z.enum(['LOADING', 'COMPLETED', 'FAILED']),
    createdAt: z.string(),
    completedAt: nullableString,
  })
  .transform((value): HistoricalDataset => ({
    ...value,
    completedAt: value.completedAt ?? null,
  }));

export const historicalDatasetListSchema = z.array(historicalDatasetSchema);

const attractionAvailabilitySchema = z.object({
  attractionId: z.string(),
  attractionName: z.string(),
  scheduledMinutes: z.number(),
  operatingMinutes: z.number(),
  uptimePercentage: z.number(),
  degradedMinutes: z.number(),
  downtimeMinutes: downtimeSchema,
  longestOutageMinutes: z.number(),
  citations: z.array(citationSchema),
});

export const availabilitySummarySchema = z
  .object({
    datasetId: z.string(),
    from: z.string(),
    to: z.string(),
    zoneId: z.string(),
    attractions: z.array(attractionAvailabilitySchema),
  })
  .transform((value): AvailabilitySummary => value);

const availabilitySegmentSchema = z
  .object({
    citationId: z.string(),
    status: z.string(),
    classification: classificationSchema,
    start: z.string(),
    end: z.string(),
    durationMinutes: z.number(),
    reason: nullableString,
    commandId: nullableString,
    activityId: nullableString,
    datasetId: z.string(),
    controlTowerUrl: z.string(),
  })
  .transform((value) => ({
    ...value,
    reason: value.reason ?? null,
    commandId: value.commandId ?? null,
    activityId: value.activityId ?? null,
  }));

export const availabilityDetailSchema = z
  .object({
    datasetId: z.string(),
    attractionId: z.string(),
    attractionName: z.string(),
    parkDate: z.string(),
    zoneId: z.string(),
    expectedStart: z.string(),
    expectedEnd: z.string(),
    plannedClosure: z.boolean(),
    plannedClosureReason: nullableString,
    scheduledMinutes: z.number(),
    operatingMinutes: z.number(),
    uptimePercentage: z.number(),
    degradedMinutes: z.number(),
    downtimeMinutes: downtimeSchema,
    longestOutageMinutes: z.number(),
    segments: z.array(availabilitySegmentSchema),
  })
  .transform((value): AvailabilityDetail => ({
    ...value,
    plannedClosureReason: value.plannedClosureReason ?? null,
  }));

export const analyticsAnswerSchema = z
  .object({
    datasetId: z.string(),
    answer: z.string(),
    claims: z.array(
      z.object({
        text: z.string(),
        citations: z.array(citationSchema),
      }),
    ),
    provider: z.string(),
  })
  .transform((value): AnalyticsAnswer => value);
