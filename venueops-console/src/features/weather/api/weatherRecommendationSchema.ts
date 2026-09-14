import { z } from 'zod';

import {
  weatherRecommendationOperatorStatuses,
  weatherRecommendationSeverities,
  weatherRecommendationStatuses,
  type WeatherRecommendation,
} from '@/features/weather/domain/recommendation';

const instantSchema = z.string().refine((value) => !Number.isNaN(Date.parse(value)), {
  message: 'Expected an ISO-8601 instant',
});

export const weatherRecommendationSchema: z.ZodType<WeatherRecommendation> = z.object({
  id: z.string().min(1),
  ruleId: z.string().min(1),
  status: z.enum(weatherRecommendationStatuses),
  operatorStatus: z.enum(weatherRecommendationOperatorStatuses),
  severity: z.enum(weatherRecommendationSeverities),
  summary: z.string().min(1),
  evidence: z.string().min(1),
  recommendedAction: z.string().min(1),
  affectedAttractionIds: z.array(z.string().min(1)),
  observedAt: instantSchema,
  receivedAt: instantSchema,
  updatedAt: instantSchema,
  sourceVersion: z.number().int().positive(),
  version: z.number().int().positive(),
  simulated: z.boolean(),
  linkedIncidentId: z
    .string()
    .nullish()
    .transform((value) => value ?? null),
});

export const weatherRecommendationListSchema = z.array(weatherRecommendationSchema);

export const weatherRecommendationSseUpdateSchema = z.object({
  eventId: z.string().min(1),
  eventType: z.enum(['UPDATED', 'CLEARED']),
  occurredAt: instantSchema,
  recommendation: weatherRecommendationSchema,
});

export const createdIncidentSchema = z.object({
  id: z.string().min(1),
  type: z.string().min(1),
  severity: z.string().min(1),
});
