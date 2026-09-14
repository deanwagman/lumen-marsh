import { z } from 'zod';

import { attractionStatuses, capacityModes } from '@/features/attractions/domain/attraction';
import { incidentSeverities, incidentStatuses } from '@/features/incidents/domain/incident';
import {
  weatherRecommendationOperatorStatuses,
  weatherRecommendationSeverities,
} from '@/features/weather/domain/recommendation';

import {
  dashboardActivityDomains,
  dashboardAttentionKinds,
  dashboardFreshnessStatuses,
  type OperatorDashboard,
} from '@/features/dashboard/domain/dashboard';

const instantSchema = z.string().refine((value) => !Number.isNaN(Date.parse(value)), {
  message: 'Expected an ISO-8601 instant',
});

const nullableInstantSchema = z
  .string()
  .nullable()
  .refine((value) => value === null || !Number.isNaN(Date.parse(value)), {
    message: 'Expected an ISO-8601 instant',
  });

const summarySchema = z.object({
  operatingAttractions: z.number().int().nonnegative(),
  attractionsNeedingAttention: z.number().int().nonnegative(),
  closedAttractions: z.number().int().nonnegative(),
  weatherHoldAttractions: z.number().int().nonnegative(),
  openIncidents: z.number().int().nonnegative(),
  majorOrCriticalIncidents: z.number().int().nonnegative(),
  pendingWeatherRecommendations: z.number().int().nonnegative(),
  publishedGuestAdvisories: z.number().int().nonnegative(),
});

const attentionItemSchema = z.object({
  kind: z.enum(dashboardAttentionKinds),
  reason: z.string().min(1),
  href: z.string().min(1),
  subjectId: z.string().min(1),
  subjectLabel: z.string().min(1),
  updatedAt: nullableInstantSchema,
});

const incidentSchema = z.object({
  id: z.string().min(1),
  title: z.string().min(1),
  severity: z.enum(incidentSeverities),
  status: z.enum(incidentStatuses),
  assignedTo: z.string().nullable(),
  attractionIds: z.array(z.string().min(1)),
  guestAdvisoryPublished: z.boolean(),
  updatedAt: instantSchema,
  version: z.number().int().nonnegative(),
});

const attractionSchema = z.object({
  id: z.string().min(1),
  name: z.string().min(1),
  area: z.string().min(1),
  status: z.enum(attractionStatuses),
  capacityMode: z.enum(capacityModes),
  waitMinutes: z.number().int().nonnegative().nullable(),
  statusMessage: z.string().nullable(),
  updatedAt: instantSchema,
  relatedOpenIncidentCount: z.number().int().nonnegative(),
});

const weatherSchema = z.object({
  id: z.string().min(1),
  severity: z.enum(weatherRecommendationSeverities),
  recommendedAction: z.string().min(1),
  evidence: z.string().min(1),
  affectedAttractionIds: z.array(z.string().min(1)),
  operatorStatus: z.enum(weatherRecommendationOperatorStatuses),
  linkedIncidentId: z.string().nullable(),
  observedAt: instantSchema,
  updatedAt: instantSchema,
  simulated: z.boolean(),
});

const advisorySchema = z.object({
  id: z.string().min(1),
  title: z.string().min(1),
  message: z.string().min(1),
  severity: z.enum(incidentSeverities),
  affectedAttractionIds: z.array(z.string().min(1)),
  updatedAt: instantSchema,
  incidentId: z.string().min(1),
});

const activitySchema = z.object({
  occurredAt: instantSchema,
  actor: z.string().min(1),
  domain: z.enum(dashboardActivityDomains),
  action: z.string().min(1),
  subject: z.string().min(1),
  reason: z.string().nullable(),
  resultingVersion: z.number().int().nonnegative(),
  href: z.string().min(1),
});

const sourceFreshnessSchema = z.object({
  status: z.enum(dashboardFreshnessStatuses),
  lastUpdatedAt: nullableInstantSchema,
});

export const operatorDashboardSchema: z.ZodType<OperatorDashboard> = z.object({
  generatedAt: instantSchema,
  summary: summarySchema,
  needsAttention: z.array(attentionItemSchema),
  openIncidents: z.array(incidentSchema),
  attractionsNeedingAttention: z.array(attractionSchema),
  pendingWeatherRecommendations: z.array(weatherSchema),
  publishedGuestAdvisories: z.array(advisorySchema),
  recentActivity: z.array(activitySchema),
  freshness: z.object({
    venueOps: sourceFreshnessSchema,
    environmentalData: sourceFreshnessSchema,
  }),
});
