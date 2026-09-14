import { z } from 'zod';

import {
  incidentSeverities,
  incidentStatuses,
  incidentTypes,
  type Incident,
  type IncidentActivity,
  type IncidentOperationalSnapshot,
} from '@/features/incidents/domain/incident';

const nullableString = z.string().nullable().optional();

export const incidentSchema = z
  .object({
    id: z.string(),
    title: z.string(),
    type: z.enum(incidentTypes),
    severity: z.enum(incidentSeverities),
    status: z.enum(incidentStatuses),
    internalDescription: nullableString,
    assignedTo: nullableString,
    guestAdvisoryPublished: z.boolean(),
    guestTitle: nullableString,
    guestMessage: nullableString,
    attractionIds: z.array(z.string()).default([]),
    createdAt: nullableString,
    updatedAt: z.string(),
    version: z.number(),
  })
  .transform(
    (value): Incident => ({
      id: value.id,
      title: value.title,
      type: value.type,
      severity: value.severity,
      status: value.status,
      internalDescription: value.internalDescription ?? null,
      assignedTo: value.assignedTo ?? null,
      guestAdvisoryPublished: value.guestAdvisoryPublished,
      guestTitle: value.guestTitle ?? null,
      guestMessage: value.guestMessage ?? null,
      attractionIds: value.attractionIds,
      createdAt: value.createdAt ?? null,
      updatedAt: value.updatedAt,
      version: value.version,
    }),
  );

export const incidentListSchema = z.array(incidentSchema);

export const incidentOperationalSnapshotSchema = z
  .object({
    id: z.string(),
    title: z.string(),
    type: z.enum(incidentTypes),
    severity: z.enum(incidentSeverities),
    status: z.enum(incidentStatuses),
    assignedTo: nullableString,
    guestAdvisoryPublished: z.boolean(),
    guestTitle: nullableString,
    guestMessage: nullableString,
    affectedAttractionIds: z.array(z.string()).default([]),
    updatedAt: z.string(),
    version: z.number(),
  })
  .transform(
    (value): IncidentOperationalSnapshot => ({
      id: value.id,
      title: value.title,
      type: value.type,
      severity: value.severity,
      status: value.status,
      assignedTo: value.assignedTo ?? null,
      guestAdvisoryPublished: value.guestAdvisoryPublished,
      guestTitle: value.guestTitle ?? null,
      guestMessage: value.guestMessage ?? null,
      affectedAttractionIds: value.affectedAttractionIds,
      updatedAt: value.updatedAt,
      version: value.version,
    }),
  );

export const incidentSnapshotListSchema = z.array(incidentOperationalSnapshotSchema);

export const incidentSseUpdateSchema = z.object({
  eventId: z.string(),
  eventType: z.enum(['INCIDENT_REPORTED', 'INCIDENT_UPDATED', 'INCIDENT_RESOLVED']),
  occurredAt: z.string(),
  incident: incidentOperationalSnapshotSchema,
});

export const incidentActivitySchema = z
  .object({
    id: z.string(),
    incidentId: z.string(),
    type: z.string(),
    actor: z.string(),
    reason: nullableString,
    occurredAt: z.string(),
    previousVersion: z.number(),
    resultingVersion: z.number(),
    title: nullableString,
    incidentType: z.enum(incidentTypes).nullable().optional(),
    severity: z.enum(incidentSeverities).nullable().optional(),
    attractionIds: z.array(z.string()).optional(),
    previousStatus: z.enum(incidentStatuses).nullable().optional(),
    newStatus: z.enum(incidentStatuses).nullable().optional(),
    assignee: nullableString,
    previousSeverity: z.enum(incidentSeverities).nullable().optional(),
    newSeverity: z.enum(incidentSeverities).nullable().optional(),
    attractionId: nullableString,
    guestTitle: nullableString,
    guestMessage: nullableString,
  })
  .transform(
    (value): IncidentActivity => ({
      id: value.id,
      incidentId: value.incidentId,
      type: value.type,
      actor: value.actor,
      reason: value.reason ?? null,
      occurredAt: value.occurredAt,
      previousVersion: value.previousVersion,
      resultingVersion: value.resultingVersion,
      title: value.title,
      incidentType: value.incidentType,
      severity: value.severity,
      attractionIds: value.attractionIds,
      previousStatus: value.previousStatus,
      newStatus: value.newStatus,
      assignee: value.assignee,
      previousSeverity: value.previousSeverity,
      newSeverity: value.newSeverity,
      attractionId: value.attractionId,
      guestTitle: value.guestTitle,
      guestMessage: value.guestMessage,
    }),
  );

export const incidentActivityListSchema = z.array(incidentActivitySchema);
