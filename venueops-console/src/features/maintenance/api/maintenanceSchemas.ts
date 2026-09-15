import { z } from 'zod';

import {
  assetCriticalities,
  assetServiceStatuses,
  assetTypes,
  checklistResults,
  maintenanceClassifications,
  maintenanceEventTypes,
  maintenancePriorities,
  maintenanceRecommendationSeverities,
  maintenanceRecommendationStatuses,
  maintenanceSignalTypes,
  maintenanceSourceTypes,
  maintenanceWorkOrderStatuses,
  type MaintenanceActivity,
  type MaintenanceAsset,
  type MaintenanceCommandResult,
  type MaintenanceWorkOrder,
  type MaintenanceWorkOrderSummary,
  type PageResult,
  type ReliabilityRecommendation,
} from '@/features/maintenance/domain/maintenance';

const nullableString = z.string().nullable().optional();
const nullableNumber = z.number().nullable().optional();

const assetSummarySchema = z.object({
  id: z.string(),
  assetCode: z.string(),
  name: z.string(),
});

const incidentSummarySchema = z
  .object({
    id: z.string(),
    title: z.string(),
    status: z.string(),
    severity: z.string(),
  })
  .nullable()
  .optional();

const recommendedActionSchema = z
  .object({
    command: z.string(),
    reason: z.string(),
  })
  .nullable()
  .optional();

const checklistItemSchema = z
  .object({
    id: z.string(),
    sequence: z.number(),
    label: z.string(),
    instructions: nullableString,
    required: z.boolean(),
    result: z.enum(checklistResults),
    notes: nullableString,
    completedByDisplayName: nullableString,
    completedAt: nullableString,
    version: z.number(),
  })
  .transform((value) => ({
    id: value.id,
    sequence: value.sequence,
    label: value.label,
    instructions: value.instructions ?? null,
    required: value.required,
    result: value.result,
    notes: value.notes ?? null,
    completedByDisplayName: value.completedByDisplayName ?? null,
    completedAt: value.completedAt ?? null,
    version: value.version,
  }));

const evidenceSchema = z
  .object({
    id: z.string(),
    label: z.string(),
    contentType: z.string(),
    uri: z.string(),
    addedBySubject: z.string(),
    addedAt: z.string(),
  })
  .transform((value) => value);

export const maintenanceWorkOrderSchema = z
  .object({
    id: z.string(),
    workOrderNumber: z.string(),
    status: z.enum(maintenanceWorkOrderStatuses),
    priority: z.enum(maintenancePriorities),
    classification: z.enum(maintenanceClassifications),
    sourceType: z.enum(maintenanceSourceTypes),
    sourceReferenceId: nullableString,
    asset: assetSummarySchema,
    attractionId: z.string(),
    incident: incidentSummarySchema,
    summary: z.string(),
    description: nullableString,
    assignedTeam: nullableString,
    assignedActorSubject: nullableString,
    estimatedRestoreAt: nullableString,
    openedAt: nullableString,
    workStartedAt: nullableString,
    readyForTestingAt: nullableString,
    completedAt: nullableString,
    canceledAt: nullableString,
    recommendedAttractionAction: recommendedActionSchema,
    checklist: z.array(checklistItemSchema).default([]),
    evidence: z.array(evidenceSchema).default([]),
    version: z.number(),
    createdAt: z.string(),
    updatedAt: z.string(),
  })
  .transform(
    (value): MaintenanceWorkOrder => ({
      id: value.id,
      workOrderNumber: value.workOrderNumber,
      status: value.status,
      priority: value.priority,
      classification: value.classification,
      sourceType: value.sourceType,
      sourceReferenceId: value.sourceReferenceId ?? null,
      asset: value.asset,
      attractionId: value.attractionId,
      incident: value.incident ?? null,
      summary: value.summary,
      description: value.description ?? null,
      assignedTeam: value.assignedTeam ?? null,
      assignedActorSubject: value.assignedActorSubject ?? null,
      estimatedRestoreAt: value.estimatedRestoreAt ?? null,
      openedAt: value.openedAt ?? null,
      workStartedAt: value.workStartedAt ?? null,
      readyForTestingAt: value.readyForTestingAt ?? null,
      completedAt: value.completedAt ?? null,
      canceledAt: value.canceledAt ?? null,
      recommendedAttractionAction: value.recommendedAttractionAction ?? null,
      checklist: value.checklist,
      evidence: value.evidence,
      version: value.version,
      createdAt: value.createdAt,
      updatedAt: value.updatedAt,
    }),
  );

export const maintenanceWorkOrderPageSchema = z
  .object({
    items: z.array(maintenanceWorkOrderSchema),
    page: z.number(),
    size: z.number(),
    total: z.number(),
  })
  .transform(
    (value): PageResult<MaintenanceWorkOrder> => ({
      items: value.items,
      page: value.page,
      size: value.size,
      total: value.total,
    }),
  );

export const maintenanceWorkOrderSummarySchema = z
  .object({
    id: z.string(),
    workOrderNumber: z.string(),
    status: z.enum(maintenanceWorkOrderStatuses),
    priority: z.enum(maintenancePriorities),
    version: z.number(),
    updatedAt: z.string(),
  })
  .transform(
    (value): MaintenanceWorkOrderSummary => ({
      id: value.id,
      workOrderNumber: value.workOrderNumber,
      status: value.status,
      priority: value.priority,
      version: value.version,
      updatedAt: value.updatedAt,
    }),
  );

export const maintenanceWorkOrderSnapshotListSchema = z.array(maintenanceWorkOrderSummarySchema);

export const maintenanceCommandResponseSchema = z
  .object({
    workOrder: maintenanceWorkOrderSummarySchema,
    activity: z
      .object({
        eventType: z.enum(maintenanceEventTypes),
        actorDisplayName: z.string(),
        occurredAt: z.string(),
        resultingVersion: z.number(),
        details: z.record(z.string(), z.unknown()).nullable().optional(),
      })
      .nullable()
      .optional(),
    replay: z.boolean(),
  })
  .transform(
    (value): MaintenanceCommandResult => ({
      workOrder: value.workOrder,
      activity: value.activity
        ? {
            eventType: value.activity.eventType,
            actorDisplayName: value.activity.actorDisplayName,
            occurredAt: value.activity.occurredAt,
            resultingVersion: value.activity.resultingVersion,
            details: value.activity.details ?? null,
          }
        : null,
      replay: value.replay,
    }),
  );

export const maintenanceSseUpdateSchema = z.object({
  eventId: z.string(),
  eventType: z.string(),
  aggregateType: z.string().optional(),
  aggregateId: z.string(),
  aggregateVersion: z.number(),
  attractionId: z.string().optional(),
  incidentId: nullableString,
  actorSubject: z.string().optional(),
  actorDisplayName: z.string().optional(),
  correlationId: z.string().optional(),
  occurredAt: z.string(),
  workOrder: maintenanceWorkOrderSummarySchema,
});

export const maintenanceActivitySchema = z
  .object({
    id: z.string(),
    sequence: z.number(),
    eventType: z.string(),
    fromStatus: z.enum(maintenanceWorkOrderStatuses).nullable().optional(),
    toStatus: z.enum(maintenanceWorkOrderStatuses).nullable().optional(),
    actorSubject: z.string(),
    actorDisplayName: z.string(),
    reason: nullableString,
    details: z.record(z.string(), z.unknown()).optional(),
    commandId: nullableString,
    correlationId: z.string(),
    occurredAt: z.string(),
    resultingVersion: z.number(),
  })
  .transform(
    (value): MaintenanceActivity => ({
      id: value.id,
      sequence: value.sequence,
      eventType: value.eventType,
      fromStatus: value.fromStatus ?? null,
      toStatus: value.toStatus ?? null,
      actorSubject: value.actorSubject,
      actorDisplayName: value.actorDisplayName,
      reason: value.reason ?? null,
      details: value.details ?? {},
      commandId: value.commandId ?? null,
      correlationId: value.correlationId,
      occurredAt: value.occurredAt,
      resultingVersion: value.resultingVersion,
    }),
  );

export const maintenanceActivityListSchema = z.array(maintenanceActivitySchema);

export const maintenanceAssetSchema = z
  .object({
    id: z.string(),
    assetCode: z.string(),
    name: z.string(),
    assetType: z.enum(assetTypes),
    attractionId: z.string(),
    parentAssetId: nullableString,
    criticality: z.enum(assetCriticalities),
    serviceStatus: z.enum(assetServiceStatuses),
    manufacturer: nullableString,
    model: nullableString,
    installedAt: nullableString,
    version: z.number(),
    createdAt: z.string(),
    updatedAt: z.string(),
  })
  .transform(
    (value): MaintenanceAsset => ({
      id: value.id,
      assetCode: value.assetCode,
      name: value.name,
      assetType: value.assetType,
      attractionId: value.attractionId,
      parentAssetId: value.parentAssetId ?? null,
      criticality: value.criticality,
      serviceStatus: value.serviceStatus,
      manufacturer: value.manufacturer ?? null,
      model: value.model ?? null,
      installedAt: value.installedAt ?? null,
      version: value.version,
      createdAt: value.createdAt,
      updatedAt: value.updatedAt,
    }),
  );

export const maintenanceAssetPageSchema = z
  .object({
    items: z.array(maintenanceAssetSchema),
    page: z.number(),
    size: z.number(),
    total: z.number(),
  })
  .transform(
    (value): PageResult<MaintenanceAsset> => ({
      items: value.items,
      page: value.page,
      size: value.size,
      total: value.total,
    }),
  );

export const reliabilityRecommendationSchema = z
  .object({
    recommendationId: z.string(),
    observationId: z.string(),
    status: z.enum(maintenanceRecommendationStatuses),
    assetCode: z.string(),
    assetId: z.string(),
    signalType: z.enum(maintenanceSignalTypes),
    severity: z.enum(maintenanceRecommendationSeverities),
    value: nullableNumber,
    unit: nullableString,
    evidence: nullableString,
    recommendedAction: z.string(),
    workOrderId: nullableString,
    observedAt: z.string(),
    receivedAt: z.string(),
    updatedAt: z.string(),
    version: z.number(),
  })
  .transform(
    (value): ReliabilityRecommendation => ({
      recommendationId: value.recommendationId,
      observationId: value.observationId,
      status: value.status,
      assetCode: value.assetCode,
      assetId: value.assetId,
      signalType: value.signalType,
      severity: value.severity,
      value: value.value ?? null,
      unit: value.unit ?? null,
      evidence: value.evidence ?? null,
      recommendedAction: value.recommendedAction,
      workOrderId: value.workOrderId ?? null,
      observedAt: value.observedAt,
      receivedAt: value.receivedAt,
      updatedAt: value.updatedAt,
      version: value.version,
    }),
  );

export const reliabilityRecommendationListSchema = z.array(reliabilityRecommendationSchema);
