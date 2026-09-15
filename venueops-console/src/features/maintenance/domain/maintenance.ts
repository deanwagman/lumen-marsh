import type { AttractionStatus } from '@/features/attractions/domain/attraction';
import type { AuthSession } from '@/auth/session';
import { hasVenueOpsScope, venueOpsScopes } from '@/auth/session';

export const maintenanceWorkOrderStatuses = [
  'DRAFT',
  'OPEN',
  'ASSIGNED',
  'IN_PROGRESS',
  'AWAITING_INSPECTION',
  'READY_FOR_TESTING',
  'COMPLETED',
  'CANCELED',
] as const;

export type MaintenanceWorkOrderStatus = (typeof maintenanceWorkOrderStatuses)[number];

export const maintenancePriorities = ['P1', 'P2', 'P3', 'P4'] as const;

export type MaintenancePriority = (typeof maintenancePriorities)[number];

export const maintenanceClassifications = [
  'CORRECTIVE',
  'PREVENTIVE',
  'INSPECTION',
  'CALIBRATION',
] as const;

export type MaintenanceClassification = (typeof maintenanceClassifications)[number];

export const maintenanceSourceTypes = [
  'MANUAL',
  'INCIDENT',
  'INSPECTION',
  'TELEMETRY',
  'SCHEDULED',
] as const;

export type MaintenanceSourceType = (typeof maintenanceSourceTypes)[number];

export const checklistResults = ['PENDING', 'PASSED', 'FAILED', 'NOT_APPLICABLE'] as const;

export type ChecklistResult = (typeof checklistResults)[number];

export const maintenanceRecommendationStatuses = [
  'PENDING_REVIEW',
  'ACCEPTED',
  'DISMISSED',
  'WORK_ORDER_CREATED',
] as const;

export type MaintenanceRecommendationStatus = (typeof maintenanceRecommendationStatuses)[number];

export const maintenanceSignalTypes = [
  'VIBRATION',
  'TEMPERATURE',
  'CURRENT',
  'PRESSURE',
  'OTHER',
] as const;

export type MaintenanceSignalType = (typeof maintenanceSignalTypes)[number];

export const maintenanceRecommendationSeverities = ['INFO', 'WARNING', 'CRITICAL'] as const;

export type MaintenanceRecommendationSeverity =
  (typeof maintenanceRecommendationSeverities)[number];

export const assetTypes = ['ATTRACTION', 'SYSTEM', 'VEHICLE', 'COMPONENT', 'SENSOR'] as const;

export type AssetType = (typeof assetTypes)[number];

export const assetCriticalities = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'] as const;

export type AssetCriticality = (typeof assetCriticalities)[number];

export const assetServiceStatuses = [
  'IN_SERVICE',
  'RESTRICTED',
  'OUT_OF_SERVICE',
  'RETIRED',
] as const;

export type AssetServiceStatus = (typeof assetServiceStatuses)[number];

export const maintenanceEventTypes = [
  'MAINTENANCE_RECOMMENDATION_RECEIVED',
  'MAINTENANCE_RECOMMENDATION_ACCEPTED',
  'MAINTENANCE_RECOMMENDATION_DISMISSED',
  'WORK_ORDER_CREATED',
  'WORK_ORDER_OPENED',
  'WORK_ORDER_ASSIGNED',
  'WORK_ORDER_REASSIGNED',
  'WORK_STARTED',
  'CHECKLIST_RESULT_RECORDED',
  'INSPECTION_REQUESTED',
  'INSPECTION_REJECTED',
  'INSPECTION_APPROVED',
  'RESTORE_ESTIMATE_UPDATED',
  'WORK_ORDER_READY_FOR_TESTING',
  'WORK_ORDER_COMPLETED',
  'WORK_ORDER_CANCELED',
  'WORK_ORDER_INCIDENT_LINKED',
  'NOTE_ADDED',
  'EVIDENCE_ADDED',
] as const;

export type MaintenanceEventType = (typeof maintenanceEventTypes)[number];

export const maintenanceWorkOrderCommands = [
  'OPEN',
  'ASSIGN',
  'START_WORK',
  'REQUEST_INSPECTION',
  'REJECT_INSPECTION',
  'APPROVE_INSPECTION',
  'COMPLETE',
  'CANCEL',
  'REASSIGN',
  'SET_ESTIMATED_RESTORE',
  'RECORD_CHECKLIST_RESULT',
  'LINK_INCIDENT',
  'ADD_NOTE',
  'ADD_EVIDENCE',
] as const;

export type MaintenanceWorkOrderCommand = (typeof maintenanceWorkOrderCommands)[number];

export const maintenanceRecommendationCommands = ['ACCEPT', 'DISMISS'] as const;

export type MaintenanceRecommendationCommand = (typeof maintenanceRecommendationCommands)[number];

export type MaintenanceAssetSummary = {
  id: string;
  assetCode: string;
  name: string;
};

export type MaintenanceIncidentSummary = {
  id: string;
  title: string;
  status: string;
  severity: string;
};

export type RecommendedAttractionAction = {
  command: string;
  reason: string;
};

export type MaintenanceChecklistItem = {
  id: string;
  sequence: number;
  label: string;
  instructions: string | null;
  required: boolean;
  result: ChecklistResult;
  notes: string | null;
  completedByDisplayName: string | null;
  completedAt: string | null;
  version: number;
};

export type MaintenanceEvidence = {
  id: string;
  label: string;
  contentType: string;
  uri: string;
  addedBySubject: string;
  addedAt: string;
};

export type MaintenanceWorkOrder = {
  id: string;
  workOrderNumber: string;
  status: MaintenanceWorkOrderStatus;
  priority: MaintenancePriority;
  classification: MaintenanceClassification;
  sourceType: MaintenanceSourceType;
  sourceReferenceId: string | null;
  asset: MaintenanceAssetSummary;
  attractionId: string;
  incident: MaintenanceIncidentSummary | null;
  summary: string;
  description: string | null;
  assignedTeam: string | null;
  assignedActorSubject: string | null;
  estimatedRestoreAt: string | null;
  openedAt: string | null;
  workStartedAt: string | null;
  readyForTestingAt: string | null;
  completedAt: string | null;
  canceledAt: string | null;
  recommendedAttractionAction: RecommendedAttractionAction | null;
  checklist: MaintenanceChecklistItem[];
  evidence: MaintenanceEvidence[];
  version: number;
  createdAt: string;
  updatedAt: string;
};

export type MaintenanceWorkOrderSummary = {
  id: string;
  workOrderNumber: string;
  status: MaintenanceWorkOrderStatus;
  priority: MaintenancePriority;
  version: number;
  updatedAt: string;
};

export type MaintenanceCommandResult = {
  workOrder: MaintenanceWorkOrderSummary;
  activity: {
    eventType: MaintenanceEventType;
    actorDisplayName: string;
    occurredAt: string;
    resultingVersion: number;
    details: Record<string, unknown> | null;
  } | null;
  replay: boolean;
};

export type MaintenanceActivity = {
  id: string;
  sequence: number;
  eventType: string;
  fromStatus: MaintenanceWorkOrderStatus | null;
  toStatus: MaintenanceWorkOrderStatus | null;
  actorSubject: string;
  actorDisplayName: string;
  reason: string | null;
  details: Record<string, unknown>;
  commandId: string | null;
  correlationId: string;
  occurredAt: string;
  resultingVersion: number;
};

export type MaintenanceAsset = {
  id: string;
  assetCode: string;
  name: string;
  assetType: AssetType;
  attractionId: string;
  parentAssetId: string | null;
  criticality: AssetCriticality;
  serviceStatus: AssetServiceStatus;
  manufacturer: string | null;
  model: string | null;
  installedAt: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
};

export type ReliabilityRecommendation = {
  recommendationId: string;
  observationId: string;
  status: MaintenanceRecommendationStatus;
  assetCode: string;
  assetId: string;
  signalType: MaintenanceSignalType;
  severity: MaintenanceRecommendationSeverity;
  value: number | null;
  unit: string | null;
  evidence: string | null;
  recommendedAction: string;
  workOrderId: string | null;
  observedAt: string;
  receivedAt: string;
  updatedAt: string;
  version: number;
};

export type PageResult<T> = {
  items: T[];
  page: number;
  size: number;
  total: number;
};

export type WorkOrderListFilter = {
  status: MaintenanceWorkOrderStatus | 'all';
  priority: MaintenancePriority | 'all';
  classification: MaintenanceClassification | 'all';
  attractionId: string;
  assetId: string;
  incidentId: string;
  assignedTeam: string;
  lifecycle: 'active' | 'terminal' | 'all';
  sort: WorkOrderSort;
  page: number;
  size: number;
};

export type WorkOrderSort =
  | 'operational'
  | 'updated'
  | 'estimatedRestore'
  | 'workOrderNumber';

export const defaultWorkOrderFilter: WorkOrderListFilter = {
  status: 'all',
  priority: 'all',
  classification: 'all',
  attractionId: 'all',
  assetId: 'all',
  incidentId: '',
  assignedTeam: '',
  lifecycle: 'active',
  sort: 'operational',
  page: 0,
  size: 50,
};

export type AssetListFilter = {
  attractionId: string;
  assetType: AssetType | 'all';
  criticality: AssetCriticality | 'all';
  serviceStatus: AssetServiceStatus | 'all';
  page: number;
  size: number;
};

export const defaultAssetFilter: AssetListFilter = {
  attractionId: 'all',
  assetType: 'all',
  criticality: 'all',
  serviceStatus: 'all',
  page: 0,
  size: 50,
};

export type MaintenanceSection = 'work-orders' | 'inbox';

export function isActiveWorkOrderStatus(status: MaintenanceWorkOrderStatus): boolean {
  return status !== 'COMPLETED' && status !== 'CANCELED';
}

export function isTerminalWorkOrderStatus(status: MaintenanceWorkOrderStatus): boolean {
  return status === 'COMPLETED' || status === 'CANCELED';
}

export function isHighPriority(priority: MaintenancePriority): boolean {
  return priority === 'P1' || priority === 'P2';
}

export function canReadMaintenance(session: AuthSession | null | undefined): boolean {
  return hasVenueOpsScope(session, venueOpsScopes.maintenanceRead);
}

export function canCommandMaintenance(session: AuthSession | null | undefined): boolean {
  return hasVenueOpsScope(session, venueOpsScopes.maintenanceCommand);
}

export function canInspectMaintenance(session: AuthSession | null | undefined): boolean {
  return (
    hasVenueOpsScope(session, venueOpsScopes.maintenanceInspect) && session?.role === 'supervisor'
  );
}

export function canCommandAttractions(session: AuthSession | null | undefined): boolean {
  return (
    hasVenueOpsScope(session, venueOpsScopes.attractionsCommand) && session?.role === 'supervisor'
  );
}

export function checklistBlocksInspection(checklist: MaintenanceChecklistItem[]): boolean {
  return checklist.some((item) => item.required && (item.result === 'PENDING' || item.result === 'FAILED'));
}

export function incompleteRequiredChecklistItems(
  checklist: MaintenanceChecklistItem[],
): MaintenanceChecklistItem[] {
  return checklist.filter(
    (item) => item.required && (item.result === 'PENDING' || item.result === 'FAILED'),
  );
}

export function checklistProgress(checklist: MaintenanceChecklistItem[]): {
  resolved: number;
  total: number;
} {
  const required = checklist.filter((item) => item.required);
  const resolved = required.filter(
    (item) => item.result === 'PASSED' || item.result === 'NOT_APPLICABLE',
  ).length;
  return { resolved, total: required.length };
}

export function recommendedAttractionCommand(
  workOrder: Pick<MaintenanceWorkOrder, 'recommendedAttractionAction'>,
): string | null {
  return workOrder.recommendedAttractionAction?.command ?? null;
}

export function attractionBlocksWorkOrderCompletion(status: AttractionStatus | undefined): boolean {
  return (
    status === 'TECHNICAL_DELAY' || status === 'TESTING' || status === 'RETURNING_TO_SERVICE'
  );
}

export type MaintenanceQueueItem =
  | { kind: 'work-order'; workOrder: MaintenanceWorkOrder }
  | { kind: 'recommendation'; recommendation: ReliabilityRecommendation };

export function queueRank(item: MaintenanceQueueItem): number {
  if (item.kind === 'recommendation') {
    return item.recommendation.status === 'PENDING_REVIEW' ? 5 : 99;
  }
  const { status, priority } = item.workOrder;
  if (!isActiveWorkOrderStatus(status)) {
    return 99;
  }
  if (status === 'READY_FOR_TESTING') {
    return 3;
  }
  if (status === 'AWAITING_INSPECTION') {
    return 4;
  }
  if (priority === 'P1') {
    return 1;
  }
  if (priority === 'P2') {
    return 2;
  }
  return 6;
}

export function compareWorkOrdersOperational(
  left: MaintenanceWorkOrder,
  right: MaintenanceWorkOrder,
): number {
  const rankDelta =
    queueRank({ kind: 'work-order', workOrder: left }) -
    queueRank({ kind: 'work-order', workOrder: right });
  if (rankDelta !== 0) {
    return rankDelta;
  }
  return Date.parse(right.updatedAt) - Date.parse(left.updatedAt);
}

export function compareQueueItems(left: MaintenanceQueueItem, right: MaintenanceQueueItem): number {
  const rankDelta = queueRank(itemRankSource(left)) - queueRank(itemRankSource(right));
  if (rankDelta !== 0) {
    return rankDelta;
  }
  return Date.parse(updatedAt(right)) - Date.parse(updatedAt(left));
}

function itemRankSource(item: MaintenanceQueueItem): MaintenanceQueueItem {
  return item;
}

function updatedAt(item: MaintenanceQueueItem): string {
  return item.kind === 'work-order' ? item.workOrder.updatedAt : item.recommendation.updatedAt;
}

export function sortWorkOrders(
  workOrders: MaintenanceWorkOrder[],
  sort: WorkOrderSort,
): MaintenanceWorkOrder[] {
  return [...workOrders].sort((left, right) => {
    if (sort === 'updated') {
      return Date.parse(right.updatedAt) - Date.parse(left.updatedAt);
    }
    if (sort === 'estimatedRestore') {
      const leftRestore = left.estimatedRestoreAt ? Date.parse(left.estimatedRestoreAt) : Number.POSITIVE_INFINITY;
      const rightRestore = right.estimatedRestoreAt
        ? Date.parse(right.estimatedRestoreAt)
        : Number.POSITIVE_INFINITY;
      if (leftRestore !== rightRestore) {
        return leftRestore - rightRestore;
      }
    }
    if (sort === 'workOrderNumber') {
      return left.workOrderNumber.localeCompare(right.workOrderNumber);
    }
    return compareWorkOrdersOperational(left, right);
  });
}

export function summarizeMaintenance(input: {
  workOrders: MaintenanceWorkOrder[];
  recommendations: ReliabilityRecommendation[];
  assets: MaintenanceAsset[];
}): {
  pendingRecommendations: number;
  activeP1: number;
  activeP2: number;
  awaitingInspection: number;
  readyForTesting: number;
  assetsOutOfService: number;
} {
  const active = input.workOrders.filter((workOrder) => isActiveWorkOrderStatus(workOrder.status));
  return {
    pendingRecommendations: input.recommendations.filter(
      (recommendation) => recommendation.status === 'PENDING_REVIEW',
    ).length,
    activeP1: active.filter((workOrder) => workOrder.priority === 'P1').length,
    activeP2: active.filter((workOrder) => workOrder.priority === 'P2').length,
    awaitingInspection: input.workOrders.filter((workOrder) => workOrder.status === 'AWAITING_INSPECTION')
      .length,
    readyForTesting: input.workOrders.filter((workOrder) => workOrder.status === 'READY_FOR_TESTING').length,
    assetsOutOfService: input.assets.filter((asset) => asset.serviceStatus === 'OUT_OF_SERVICE').length,
  };
}

export function maintenanceNavBadgeCount(input: {
  workOrders: Array<Pick<MaintenanceWorkOrder, 'status' | 'priority'>>;
  recommendations: Array<Pick<ReliabilityRecommendation, 'status'>>;
}): number {
  const pending = input.recommendations.filter((item) => item.status === 'PENDING_REVIEW').length;
  const urgent = input.workOrders.filter(
    (workOrder) =>
      isActiveWorkOrderStatus(workOrder.status) && isHighPriority(workOrder.priority),
  ).length;
  return pending + urgent;
}

export function applyNewerWorkOrder<T extends { id: string; version: number }>(
  current: T | undefined,
  next: T,
): T | undefined {
  if (!current) {
    return next;
  }
  if (current.id !== next.id) {
    return current;
  }
  if (next.version < current.version) {
    return undefined;
  }
  return next;
}

export function newCommandId(): string {
  return crypto.randomUUID();
}

export function newCorrelationId(): string {
  return crypto.randomUUID();
}
