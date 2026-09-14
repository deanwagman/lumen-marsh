import { z } from 'zod';

import { attractionEventTypes } from '@/features/attractions/domain/activity';
import { attractionCommands } from '@/features/attractions/domain/commands';
import {
  attractionStatuses,
  attractionTypes,
  capacityModes,
} from '@/features/attractions/domain/attraction';

const instantSchema = z.string().refine((value) => !Number.isNaN(Date.parse(value)), {
  message: 'Expected an ISO-8601 instant',
});

export const attractionStatusSchema = z.enum(attractionStatuses);
export const capacityModeSchema = z.enum(capacityModes);
export const attractionTypeSchema = z.enum(attractionTypes);
export const attractionCommandSchema = z.enum(attractionCommands);
export const attractionEventTypeSchema = z.enum(attractionEventTypes);

export const attractionSummarySchema = z.object({
  id: z.string().min(1),
  name: z.string().min(1),
  area: z.string().min(1),
  type: attractionTypeSchema,
  status: attractionStatusSchema,
  capacityMode: capacityModeSchema,
  waitMinutes: z.number().int().nonnegative().nullable(),
  statusMessage: z.string().nullable(),
  updatedAt: instantSchema,
  version: z.number().int().nonnegative(),
  thumbnailUrl: z.string().min(1),
  thumbnailAltText: z.string().min(1),
});

export const attractionListSchema = z.array(attractionSummarySchema);

export const operatorAttractionSchema = z.object({
  id: z.string().min(1),
  name: z.string().min(1),
  area: z.string().min(1),
  type: attractionTypeSchema,
  status: attractionStatusSchema,
  capacityMode: capacityModeSchema,
  waitMinutes: z.number().int().nonnegative().nullable(),
  updatedAt: instantSchema,
  version: z.number().int().nonnegative(),
});

export const attractionOperationalUpdateSchema = z.object({
  id: z.string().min(1),
  status: attractionStatusSchema,
  capacityMode: capacityModeSchema,
  waitMinutes: z.number().int().nonnegative().nullable(),
  statusMessage: z.string().nullable(),
  updatedAt: instantSchema,
  version: z.number().int().nonnegative(),
});

export const attractionSseUpdateSchema = z.object({
  eventId: z.string().min(1),
  eventType: z.enum(['STATUS_CHANGED', 'CAPACITY_CHANGED', 'WAIT_TIME_CHANGED']),
  occurredAt: instantSchema,
  attraction: attractionOperationalUpdateSchema,
});

export const attractionActivitySchema = z.object({
  id: z.string().min(1),
  attractionId: z.string().min(1),
  type: attractionEventTypeSchema,
  actor: z.string().min(1),
  reason: z
    .string()
    .nullish()
    .transform((value) => value ?? null),
  occurredAt: instantSchema,
  previousVersion: z.number().int().nonnegative(),
  resultingVersion: z.number().int().nonnegative(),
  previousStatus: attractionStatusSchema.nullish().transform((value) => value ?? null),
  newStatus: attractionStatusSchema.nullish().transform((value) => value ?? null),
  previousMode: capacityModeSchema.nullish().transform((value) => value ?? null),
  newMode: capacityModeSchema.nullish().transform((value) => value ?? null),
  previousMinutes: z
    .number()
    .int()
    .nonnegative()
    .nullish()
    .transform((value) => value ?? null),
  newMinutes: z
    .number()
    .int()
    .nonnegative()
    .nullish()
    .transform((value) => value ?? null),
});

export const attractionActivityListSchema = z.array(attractionActivitySchema);
