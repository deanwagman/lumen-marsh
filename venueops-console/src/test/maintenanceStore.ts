import type {
  MaintenanceActivity,
  MaintenanceEventType,
  MaintenanceWorkOrder,
} from '@/features/maintenance/domain/maintenance';
import {
  attractionBlocksWorkOrderCompletion,
  checklistBlocksInspection,
  incompleteRequiredChecklistItems,
  isActiveWorkOrderStatus,
  isTerminalWorkOrderStatus,
  type MaintenanceAsset,
  type ReliabilityRecommendation,
} from '@/features/maintenance/domain/maintenance';
import { resultingStatus } from '@/features/maintenance/domain/maintenanceActions';
import {
  cypressCoilAssets,
  draftCorrectiveWorkOrder,
  vibrationRecommendation,
} from '@/features/maintenance/test/fixtures';
import type { AttractionStatus } from '@/features/attractions/domain/attraction';
import { catalogAttractions } from '@/test/fixtures';

export const maintenanceAssetStore: MaintenanceAsset[] = [];
export const maintenanceRecommendationStore: ReliabilityRecommendation[] = [];
export const maintenanceWorkOrderStore: MaintenanceWorkOrder[] = [];
export const maintenanceActivityStore: MaintenanceActivity[] = [];
export const processedMaintenanceCommands = new Map<
  string,
  { workOrderId?: string; recommendationId?: string; body: unknown }
>();

const commandEventTypes: Record<string, MaintenanceEventType> = {
  OPEN: 'WORK_ORDER_OPENED',
  ASSIGN: 'WORK_ORDER_ASSIGNED',
  START_WORK: 'WORK_STARTED',
  REQUEST_INSPECTION: 'INSPECTION_REQUESTED',
  REJECT_INSPECTION: 'INSPECTION_REJECTED',
  APPROVE_INSPECTION: 'INSPECTION_APPROVED',
  COMPLETE: 'WORK_ORDER_COMPLETED',
  CANCEL: 'WORK_ORDER_CANCELED',
  REASSIGN: 'WORK_ORDER_REASSIGNED',
  SET_ESTIMATED_RESTORE: 'RESTORE_ESTIMATE_UPDATED',
  RECORD_CHECKLIST_RESULT: 'CHECKLIST_RESULT_RECORDED',
  LINK_INCIDENT: 'WORK_ORDER_INCIDENT_LINKED',
  ADD_NOTE: 'NOTE_ADDED',
  ADD_EVIDENCE: 'EVIDENCE_ADDED',
};

let workOrderSequence = 1;

export function resetMaintenanceStores() {
  maintenanceAssetStore.splice(0, maintenanceAssetStore.length, ...cypressCoilAssets.map((item) => ({ ...item })));
  maintenanceRecommendationStore.splice(0, maintenanceRecommendationStore.length, {
    ...vibrationRecommendation,
  });
  maintenanceWorkOrderStore.splice(0, maintenanceWorkOrderStore.length);
  maintenanceActivityStore.splice(0, maintenanceActivityStore.length);
  processedMaintenanceCommands.clear();
  workOrderSequence = 1;
}

export function nextWorkOrderNumber(): string {
  const number = `LM-2026-${String(workOrderSequence).padStart(4, '0')}`;
  workOrderSequence += 1;
  return number;
}

export function acceptRecommendation(recommendation: ReliabilityRecommendation): ReliabilityRecommendation {
  const workOrderNumber = nextWorkOrderNumber();
  const created = draftCorrectiveWorkOrder({
    id: crypto.randomUUID(),
    workOrderNumber,
    sourceReferenceId: recommendation.observationId,
    summary: recommendation.recommendedAction,
    description: recommendation.evidence,
    recommendedAttractionAction: {
      command: 'REPORT_TECHNICAL_FAULT',
      reason: `P1 corrective work order ${workOrderNumber} is active.`,
    },
  });
  maintenanceWorkOrderStore.unshift(created);
  appendActivity(created, 'WORK_ORDER_CREATED', null, 'DRAFT', 'Reliability recommendation accepted');
  const next: ReliabilityRecommendation = {
    ...recommendation,
    status: 'WORK_ORDER_CREATED',
    workOrderId: created.id,
    version: recommendation.version + 1,
    updatedAt: '2026-09-14T18:30:00Z',
  };
  replaceRecommendation(next);
  return next;
}

export function dismissRecommendation(
  recommendation: ReliabilityRecommendation,
): ReliabilityRecommendation {
  const next: ReliabilityRecommendation = {
    ...recommendation,
    status: 'DISMISSED',
    version: recommendation.version + 1,
    updatedAt: '2026-09-14T18:30:00Z',
  };
  replaceRecommendation(next);
  return next;
}

export type WorkOrderCommandOutcome =
  | { ok: true; replay: boolean; workOrder?: MaintenanceWorkOrder; response: ReturnType<typeof commandResponseFrom> }
  | { ok: false; status: number; error: ReturnType<typeof problem> };

export function applyWorkOrderCommand(
  workOrder: MaintenanceWorkOrder,
  body: {
    commandId: string;
    type: string;
    expectedVersion: number;
    reason?: string;
    data?: Record<string, unknown>;
  },
  actor = 'Development Supervisor',
): WorkOrderCommandOutcome {
  const processed = processedMaintenanceCommands.get(body.commandId);
  if (processed) {
    if (processed.workOrderId && processed.workOrderId !== workOrder.id) {
      return {
        ok: false,
        status: 409,
        error: problem('DUPLICATE_COMMAND', 'Duplicate command', 409, {
          commandId: body.commandId,
          existingAggregateId: processed.workOrderId,
          requestedAggregateId: workOrder.id,
        }),
      };
    }
    return {
      ok: true,
      replay: true,
      response: processed.body as ReturnType<typeof commandResponseFrom>,
    };
  }

  if (body.expectedVersion !== workOrder.version) {
    return {
      ok: false,
      status: 409,
      error: problem('STALE_VERSION', 'Stale version', 409, {
        expectedVersion: body.expectedVersion,
        currentVersion: workOrder.version,
        actualVersion: workOrder.version,
      }),
    };
  }

  const next = { ...workOrder, checklist: workOrder.checklist.map((item) => ({ ...item })) };
  const occurredAt = '2026-09-14T18:40:00Z';
  const eventType: string = commandEventTypes[body.type] ?? body.type;
  const previous = next.status;

  switch (body.type) {
    case 'OPEN':
    case 'ASSIGN':
    case 'START_WORK':
    case 'REQUEST_INSPECTION':
    case 'REJECT_INSPECTION':
    case 'APPROVE_INSPECTION':
    case 'COMPLETE':
    case 'CANCEL': {
      if (body.type === 'REQUEST_INSPECTION' && checklistBlocksInspection(next.checklist)) {
        return {
          ok: false,
          status: 422,
          error: problem(
            'MAINTENANCE_PREREQUISITE',
            'Required checklist items must be passed or marked not applicable before inspection',
            422,
          ),
        };
      }
      if (body.type === 'APPROVE_INSPECTION' && incompleteRequiredChecklistItems(next.checklist).length > 0) {
        return {
          ok: false,
          status: 422,
          error: problem(
            'MAINTENANCE_PREREQUISITE',
            'Failed or pending required checklist items block inspection approval',
            422,
          ),
        };
      }
      if (body.type === 'COMPLETE') {
        const attraction = catalogAttractions.find((item) => item.id === next.attractionId);
        const status = (attraction?.status ?? 'CLOSED') as AttractionStatus;
        if (attractionBlocksWorkOrderCompletion(status)) {
          return {
            ok: false,
            status: 422,
            error: problem(
              'MAINTENANCE_PREREQUISITE',
              `Work order ${next.workOrderNumber} cannot complete until attraction testing and return-to-service approval finish`,
              422,
            ),
          };
        }
      }
      const resulting = resultingStatus(next.status, body.type as never);
      if (!resulting) {
        return {
          ok: false,
          status: 409,
          error: problem('INVALID_TRANSITION', 'Invalid maintenance transition', 409, {
            currentStatus: next.status,
            command: body.type,
          }),
        };
      }
      next.status = resulting;
      next.version += 1;
      next.updatedAt = occurredAt;
      if (body.type === 'OPEN') next.openedAt = occurredAt;
      if (body.type === 'START_WORK') next.workStartedAt = occurredAt;
      if (body.type === 'APPROVE_INSPECTION') {
        next.readyForTestingAt = occurredAt;
      }
      if (body.type === 'COMPLETE') next.completedAt = occurredAt;
      if (body.type === 'CANCEL') next.canceledAt = occurredAt;
      if (body.type === 'ASSIGN') {
        next.assignedTeam = String(body.data?.teamId ?? next.assignedTeam ?? 'Ride Systems');
        next.assignedActorSubject = String(body.data?.actorSubject ?? 'tech-cypress-1');
      }
      next.recommendedAttractionAction = recommendedAction(next);
      appendActivity(next, eventType, previous, next.status, body.reason ?? null, actor, body.commandId);
      break;
    }
    case 'REASSIGN':
      next.assignedTeam = String(body.data?.teamId ?? 'Ride Systems');
      next.assignedActorSubject = String(body.data?.actorSubject ?? 'tech-cypress-1');
      next.version += 1;
      next.updatedAt = occurredAt;
      appendActivity(next, 'WORK_ORDER_REASSIGNED', next.status, next.status, body.reason ?? null, actor, body.commandId);
      break;
    case 'SET_ESTIMATED_RESTORE':
      next.estimatedRestoreAt = body.data?.estimatedRestoreAt
        ? String(body.data.estimatedRestoreAt)
        : null;
      next.version += 1;
      next.updatedAt = occurredAt;
      appendActivity(next, 'RESTORE_ESTIMATE_UPDATED', next.status, next.status, body.reason ?? null, actor, body.commandId);
      break;
    case 'RECORD_CHECKLIST_RESULT': {
      const itemId = String(body.data?.checklistItemId ?? '');
      const item = next.checklist.find((entry) => entry.id === itemId);
      if (!item) {
        return {
          ok: false,
          status: 422,
          error: problem('MAINTENANCE_PREREQUISITE', `Checklist item not found: ${itemId}`, 422),
        };
      }
      item.result = (body.data?.result as MaintenanceWorkOrder['checklist'][number]['result']) ?? 'PASSED';
      item.notes = body.data?.notes ? String(body.data.notes) : item.notes;
      item.completedByDisplayName = actor;
      item.completedAt = occurredAt;
      item.version += 1;
      next.version += 1;
      next.updatedAt = occurredAt;
      appendActivity(next, 'CHECKLIST_RESULT_RECORDED', next.status, next.status, body.reason ?? null, actor, body.commandId);
      break;
    }
    case 'LINK_INCIDENT':
      next.incident = {
        id: String(body.data?.incidentId ?? 'inc-lightning-1'),
        title: 'Linked incident',
        status: 'REPORTED',
        severity: 'MAJOR',
      };
      next.version += 1;
      next.updatedAt = occurredAt;
      appendActivity(next, 'WORK_ORDER_INCIDENT_LINKED', next.status, next.status, body.reason ?? null, actor, body.commandId);
      break;
    case 'ADD_NOTE':
      next.version += 1;
      next.updatedAt = occurredAt;
      appendActivity(
        next,
        'NOTE_ADDED',
        next.status,
        next.status,
        String(body.data?.note ?? body.reason ?? ''),
        actor,
        body.commandId,
      );
      break;
    case 'ADD_EVIDENCE':
      next.evidence = [
        ...next.evidence,
        {
          id: crypto.randomUUID(),
          label: String(body.data?.label ?? 'Evidence'),
          contentType: String(body.data?.contentType ?? 'text/uri-list'),
          uri: String(body.data?.uri ?? 'https://example.invalid/evidence'),
          addedBySubject: 'local-operator',
          addedAt: occurredAt,
        },
      ];
      next.version += 1;
      next.updatedAt = occurredAt;
      appendActivity(next, 'EVIDENCE_ADDED', next.status, next.status, body.reason ?? null, actor, body.commandId);
      break;
    default:
      return {
        ok: false,
        status: 409,
        error: problem('INVALID_TRANSITION', 'Invalid maintenance transition', 409, {
          currentStatus: next.status,
          command: body.type,
        }),
      };
  }

  replaceWorkOrder(next);
  const response = commandResponseFrom(next, eventType, false);
  processedMaintenanceCommands.set(body.commandId, { workOrderId: next.id, body: response });
  return { ok: true, replay: false, workOrder: next, response };
}

export function commandResponseFrom(workOrder: MaintenanceWorkOrder, type: string, replay = false) {
  return {
    workOrder: {
      id: workOrder.id,
      workOrderNumber: workOrder.workOrderNumber,
      status: workOrder.status,
      priority: workOrder.priority,
      version: workOrder.version,
      updatedAt: workOrder.updatedAt,
    },
    activity: {
      eventType: commandEventTypes[type] ?? type,
      actorDisplayName: 'Development Supervisor',
      occurredAt: workOrder.updatedAt,
      resultingVersion: workOrder.version,
      details: {},
    },
    replay,
  };
}

export function page<T>(items: T[], page = 0, size = 50) {
  const start = page * size;
  return {
    items: items.slice(start, start + size),
    page,
    size,
    total: items.length,
  };
}

export function filterWorkOrders(
  items: MaintenanceWorkOrder[],
  query: URLSearchParams,
): MaintenanceWorkOrder[] {
  const status = query.get('status');
  const priority = query.get('priority');
  const classification = query.get('classification');
  const attractionId = query.get('attractionId');
  const assetId = query.get('assetId');
  const incidentId = query.get('incidentId');
  const assignedTeam = query.get('assignedTeam');
  const lifecycle = query.get('lifecycle');
  return items.filter((item) => {
    if (status && item.status !== status) return false;
    if (priority && item.priority !== priority) return false;
    if (classification && item.classification !== classification) return false;
    if (attractionId && item.attractionId !== attractionId) return false;
    if (assetId && item.asset.id !== assetId) return false;
    if (incidentId && item.incident?.id !== incidentId) return false;
    if (assignedTeam && item.assignedTeam !== assignedTeam) return false;
    if (lifecycle === 'active' && !isActiveWorkOrderStatus(item.status)) return false;
    if (lifecycle === 'terminal' && !isTerminalWorkOrderStatus(item.status)) return false;
    return true;
  });
}

export function problem(
  code: string,
  detail: string,
  status: number,
  extra: Record<string, unknown> = {},
) {
  return {
    title: detail,
    status,
    detail,
    code,
    ...extra,
  };
}

function recommendedAction(workOrder: MaintenanceWorkOrder) {
  if (!isActiveWorkOrderStatus(workOrder.status)) {
    return null;
  }
  if (workOrder.status === 'READY_FOR_TESTING') {
    return {
      command: 'START_TESTING',
      reason: `Work order ${workOrder.workOrderNumber} is ready for operational testing.`,
    };
  }
  if (workOrder.priority === 'P1' && workOrder.classification === 'CORRECTIVE') {
    return {
      command: 'REPORT_TECHNICAL_FAULT',
      reason: `P1 corrective work order ${workOrder.workOrderNumber} is active.`,
    };
  }
  return null;
}

function replaceWorkOrder(next: MaintenanceWorkOrder) {
  const index = maintenanceWorkOrderStore.findIndex((item) => item.id === next.id);
  if (index >= 0) {
    maintenanceWorkOrderStore[index] = next;
  } else {
    maintenanceWorkOrderStore.unshift(next);
  }
}

function replaceRecommendation(next: ReliabilityRecommendation) {
  const index = maintenanceRecommendationStore.findIndex(
    (item) => item.recommendationId === next.recommendationId,
  );
  if (index >= 0) {
    maintenanceRecommendationStore[index] = next;
  } else {
    maintenanceRecommendationStore.unshift(next);
  }
}

function appendActivity(
  workOrder: MaintenanceWorkOrder,
  eventType: string,
  fromStatus: MaintenanceWorkOrder['status'] | null,
  toStatus: MaintenanceWorkOrder['status'] | null,
  reason: string | null,
  actor = 'Development Supervisor',
  commandId: string | null = null,
) {
  maintenanceActivityStore.push({
    id: crypto.randomUUID(),
    sequence: maintenanceActivityStore.filter((item) => item.details.workOrderId === workOrder.id).length + 1,
    eventType,
    fromStatus,
    toStatus,
    actorSubject: 'local-operator',
    actorDisplayName: actor,
    reason,
    details: { workOrderId: workOrder.id },
    commandId,
    correlationId: crypto.randomUUID(),
    occurredAt: workOrder.updatedAt,
    resultingVersion: workOrder.version,
  });
}

export function activityFor(workOrderId: string): MaintenanceActivity[] {
  return maintenanceActivityStore.filter((item) => item.details.workOrderId === workOrderId);
}
