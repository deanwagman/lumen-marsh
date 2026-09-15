import type { AttractionSummary, OperatorAttraction } from '@/features/attractions/domain/attraction';
import { validCommands } from '@/features/attractions/domain/commands';
import type { AuthSession } from '@/auth/session';

import {
  attractionBlocksWorkOrderCompletion,
  canCommandAttractions,
  canCommandMaintenance,
  canInspectMaintenance,
  checklistBlocksInspection,
  incompleteRequiredChecklistItems,
  isHighPriority,
  isTerminalWorkOrderStatus,
  type MaintenanceWorkOrder,
  type MaintenanceWorkOrderCommand,
  type ReliabilityRecommendation,
} from './maintenance';

export type MaintenanceActionKind = 'primary' | 'secondary' | 'supporting' | 'destructive';

export type MaintenanceAction = {
  type: MaintenanceWorkOrderCommand;
  label: string;
  kind: MaintenanceActionKind;
  requiresConfirmation: boolean;
  requiresReason: boolean;
  disabledReason?: string;
};

const confirmationCommands = new Set<MaintenanceWorkOrderCommand>([
  'CANCEL',
  'APPROVE_INSPECTION',
  'REJECT_INSPECTION',
  'COMPLETE',
]);

const reasonCommands = new Set<MaintenanceWorkOrderCommand>([
  'CANCEL',
  'APPROVE_INSPECTION',
  'REJECT_INSPECTION',
  'COMPLETE',
  'REASSIGN',
  'ADD_NOTE',
]);

export function resultingStatus(
  current: MaintenanceWorkOrder['status'],
  command: MaintenanceWorkOrderCommand,
): MaintenanceWorkOrder['status'] | null {
  const transitions: Partial<
    Record<MaintenanceWorkOrder['status'], Partial<Record<MaintenanceWorkOrderCommand, MaintenanceWorkOrder['status']>>>
  > = {
    DRAFT: { OPEN: 'OPEN', CANCEL: 'CANCELED' },
    OPEN: { ASSIGN: 'ASSIGNED', CANCEL: 'CANCELED' },
    ASSIGNED: { START_WORK: 'IN_PROGRESS', CANCEL: 'CANCELED' },
    IN_PROGRESS: { REQUEST_INSPECTION: 'AWAITING_INSPECTION', CANCEL: 'CANCELED' },
    AWAITING_INSPECTION: {
      APPROVE_INSPECTION: 'READY_FOR_TESTING',
      REJECT_INSPECTION: 'IN_PROGRESS',
      CANCEL: 'CANCELED',
    },
    READY_FOR_TESTING: { COMPLETE: 'COMPLETED', CANCEL: 'CANCELED' },
  };
  return transitions[current]?.[command] ?? null;
}

export function availableMaintenanceActions({
  workOrder,
  session,
  attraction,
}: {
  workOrder: MaintenanceWorkOrder;
  session: AuthSession | null | undefined;
  attraction?: AttractionSummary | OperatorAttraction | null;
}): MaintenanceAction[] {
  if (isTerminalWorkOrderStatus(workOrder.status)) {
    return canCommandMaintenance(session)
      ? [
          action('ADD_NOTE', 'Add note', 'supporting'),
        ]
      : [];
  }

  const canCommand = canCommandMaintenance(session);
  const canInspect = canInspectMaintenance(session);
  const actions: MaintenanceAction[] = [];

  if (canCommand) {
    if (workOrder.status === 'DRAFT') {
      actions.push(action('OPEN', 'Open work order', 'primary'));
    }
    if (workOrder.status === 'OPEN') {
      actions.push(action('ASSIGN', 'Assign', 'primary'));
    }
    if (workOrder.status === 'ASSIGNED') {
      actions.push(action('START_WORK', 'Start work', 'primary'));
      actions.push(action('REASSIGN', 'Reassign', 'secondary'));
    }
    if (workOrder.status === 'IN_PROGRESS') {
      const blocked = incompleteRequiredChecklistItems(workOrder.checklist);
      actions.push({
        ...action('REQUEST_INSPECTION', 'Request inspection', 'primary'),
        disabledReason: blocked.length
          ? `Complete required checklist items first: ${blocked.map((item) => item.label).join(', ')}.`
          : undefined,
      });
      actions.push(action('REASSIGN', 'Reassign', 'secondary'));
    }
    if (workOrder.status === 'OPEN' || workOrder.status === 'DRAFT') {
      actions.push(action('SET_ESTIMATED_RESTORE', 'Update estimated restoration', 'supporting'));
    }
    if (workOrder.status === 'ASSIGNED' || workOrder.status === 'IN_PROGRESS') {
      actions.push(action('SET_ESTIMATED_RESTORE', 'Update estimated restoration', 'supporting'));
    }
    if (!workOrder.incident) {
      actions.push(action('LINK_INCIDENT', 'Link incident', 'supporting'));
    }
    actions.push(action('ADD_NOTE', 'Add note', 'supporting'));
    actions.push(action('ADD_EVIDENCE', 'Add evidence', 'supporting'));
  }

  if (workOrder.status === 'AWAITING_INSPECTION') {
    if (canInspect) {
      const blocked = incompleteRequiredChecklistItems(workOrder.checklist);
      actions.push({
        ...action('APPROVE_INSPECTION', 'Approve inspection', 'primary'),
        disabledReason: blocked.length
          ? `Required checklist items still block approval: ${blocked.map((item) => item.label).join(', ')}.`
          : undefined,
      });
      actions.push(action('REJECT_INSPECTION', 'Reject inspection', 'destructive'));
    }
  }

  if (workOrder.status === 'READY_FOR_TESTING' && canInspect) {
    actions.push({
      ...action('COMPLETE', 'Complete work order', 'primary'),
      disabledReason: attractionBlocksWorkOrderCompletion(attraction?.status)
        ? 'Attraction testing is still in progress. Operations must finish testing and return-to-service before this work order can complete.'
        : undefined,
    });
  }

  if (canCancel(workOrder, session, canCommand)) {
    actions.push(action('CANCEL', 'Cancel work order', 'destructive'));
  }

  return actions;
}

export function primaryMaintenanceAction(
  actions: MaintenanceAction[],
): MaintenanceAction | undefined {
  return actions.find((item) => item.kind === 'primary' && !item.disabledReason);
}

export function inspectionAuthorizationHint(
  workOrder: MaintenanceWorkOrder,
  session: AuthSession | null | undefined,
): string | null {
  if (workOrder.status !== 'AWAITING_INSPECTION') {
    return null;
  }
  if (canInspectMaintenance(session)) {
    return null;
  }
  return 'Supervisor approval with maintenance inspect scope is required before this work order can be released for testing.';
}

export function completionAuthorizationHint(
  workOrder: MaintenanceWorkOrder,
  session: AuthSession | null | undefined,
): string | null {
  if (workOrder.status !== 'READY_FOR_TESTING') {
    return null;
  }
  if (canInspectMaintenance(session)) {
    return null;
  }
  return 'A supervisor with maintenance inspect scope completes the work order after Operations finishes testing.';
}

export function canStartAttractionTesting({
  workOrder,
  session,
  attraction,
}: {
  workOrder: MaintenanceWorkOrder;
  session: AuthSession | null | undefined;
  attraction?: AttractionSummary | OperatorAttraction | null;
}): boolean {
  if (workOrder.recommendedAttractionAction?.command !== 'START_TESTING') {
    return false;
  }
  if (!canCommandAttractions(session) || !attraction) {
    return false;
  }
  return validCommands(attraction.status, attraction.capacityMode).includes('START_TESTING');
}

export function canReportTechnicalFault({
  workOrder,
  session,
  attraction,
}: {
  workOrder: MaintenanceWorkOrder;
  session: AuthSession | null | undefined;
  attraction?: AttractionSummary | OperatorAttraction | null;
}): boolean {
  if (workOrder.recommendedAttractionAction?.command !== 'REPORT_TECHNICAL_FAULT') {
    return false;
  }
  if (!canCommandAttractions(session) || !attraction) {
    return false;
  }
  return validCommands(attraction.status, attraction.capacityMode).includes('REPORT_TECHNICAL_FAULT');
}

export function attractionHandoffOwnershipMessage(
  attraction?: AttractionSummary | OperatorAttraction | null,
): string {
  if (attraction?.status === 'TESTING') {
    return 'Operations owns the next step. Complete testing, then approve return to service.';
  }
  if (attraction?.status === 'RETURNING_TO_SERVICE') {
    return 'Operations owns the next step. A supervisor must approve return to service.';
  }
  if (attraction?.status === 'OPERATING') {
    return 'Operations has returned the attraction to service. Complete the work order when policy allows.';
  }
  return 'Operations owns the next step. A supervisor with attraction command scope can start testing.';
}

export function nextActionLabel(
  workOrder: MaintenanceWorkOrder,
  attraction?: AttractionSummary | OperatorAttraction | null,
): string {
  if (workOrder.status === 'DRAFT') {
    return 'Open the work order';
  }
  if (workOrder.status === 'OPEN') {
    return workOrder.assignedTeam ? 'Start work after assignment confirmation' : 'Assign a crew';
  }
  if (workOrder.status === 'ASSIGNED') {
    return 'Start inspection and repair work';
  }
  if (workOrder.status === 'IN_PROGRESS') {
    return checklistBlocksInspection(workOrder.checklist)
      ? 'Complete required checklist items'
      : 'Request inspection';
  }
  if (workOrder.status === 'AWAITING_INSPECTION') {
    return 'Supervisor inspection approval';
  }
  if (workOrder.status === 'READY_FOR_TESTING') {
    if (attraction?.status === 'TESTING') {
      return 'Wait for Operations to finish testing';
    }
    if (attraction?.status === 'RETURNING_TO_SERVICE') {
      return 'Wait for Operations to approve return to service';
    }
    if (attraction?.status === 'OPERATING') {
      return 'Complete the work order';
    }
    return workOrder.recommendedAttractionAction?.command === 'START_TESTING'
      ? 'Hand off to Operations for testing'
      : 'Complete the work order';
  }
  if (workOrder.status === 'COMPLETED') {
    return 'Closed';
  }
  return 'Canceled';
}

export function recommendationNextAction(
  status: ReliabilityRecommendation['status'],
): string {
  if (status === 'PENDING_REVIEW') {
    return 'Review reliability evidence';
  }
  if (status === 'WORK_ORDER_CREATED') {
    return 'Open the corrective work order';
  }
  if (status === 'DISMISSED') {
    return 'No further maintenance action';
  }
  return 'Work order is being created';
}

function canCancel(
  workOrder: MaintenanceWorkOrder,
  session: AuthSession | null | undefined,
  canCommand: boolean,
): boolean {
  if (isTerminalWorkOrderStatus(workOrder.status)) {
    return false;
  }
  if (isHighPriority(workOrder.priority)) {
    return canCommand && session?.role === 'supervisor';
  }
  return canCommand;
}

function action(
  type: MaintenanceWorkOrderCommand,
  label: string,
  kind: MaintenanceActionKind,
): MaintenanceAction {
  return {
    type,
    label,
    kind,
    requiresConfirmation: confirmationCommands.has(type),
    requiresReason: reasonCommands.has(type),
  };
}
