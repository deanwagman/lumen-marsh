import { formatLabel, formatUpdatedAt } from '@/features/attractions/domain/formatters';
import { formatDataAge } from '@/features/weather/domain/formatters';
import type { StatusTone } from '@/features/attractions/domain/attraction';

import type {
  AssetServiceStatus,
  ChecklistResult,
  MaintenancePriority,
  MaintenanceRecommendationSeverity,
  MaintenanceRecommendationStatus,
  MaintenanceWorkOrderStatus,
  ReliabilityRecommendation,
} from './maintenance';

export function formatWorkOrderStatus(status: MaintenanceWorkOrderStatus): string {
  return formatLabel(status);
}

export function formatPriority(priority: MaintenancePriority): string {
  return priority;
}

export function formatChecklistResult(result: ChecklistResult): string {
  return formatLabel(result);
}

export function formatRecommendationStatus(status: MaintenanceRecommendationStatus): string {
  return formatLabel(status);
}

export function formatEventType(eventType: string): string {
  const labels: Record<string, string> = {
    MAINTENANCE_RECOMMENDATION_RECEIVED: 'Reliability recommendation received',
    MAINTENANCE_RECOMMENDATION_ACCEPTED: 'Recommendation accepted',
    MAINTENANCE_RECOMMENDATION_DISMISSED: 'Recommendation dismissed',
    WORK_ORDER_CREATED: 'Work order created',
    WORK_ORDER_OPENED: 'Work order opened',
    WORK_ORDER_ASSIGNED: 'Work order assigned',
    WORK_ORDER_REASSIGNED: 'Work order reassigned',
    WORK_STARTED: 'Work started',
    CHECKLIST_RESULT_RECORDED: 'Checklist result recorded',
    INSPECTION_REQUESTED: 'Inspection requested',
    INSPECTION_REJECTED: 'Inspection rejected',
    INSPECTION_APPROVED: 'Inspection approved',
    RESTORE_ESTIMATE_UPDATED: 'Estimated restoration updated',
    WORK_ORDER_READY_FOR_TESTING: 'Ready for testing',
    WORK_ORDER_COMPLETED: 'Work order completed',
    WORK_ORDER_CANCELED: 'Work order canceled',
    WORK_ORDER_INCIDENT_LINKED: 'Incident linked',
    NOTE_ADDED: 'Note added',
    EVIDENCE_ADDED: 'Evidence added',
  };
  return labels[eventType] ?? formatLabel(eventType);
}

export function formatMaintenanceTime(value: string | null | undefined): string {
  if (!value) {
    return '—';
  }
  return formatUpdatedAt(value);
}

export function formatEstimatedRestore(value: string | null | undefined, now = Date.now()): string {
  if (!value) {
    return 'Not estimated';
  }
  const then = Date.parse(value);
  if (Number.isNaN(then)) {
    return value;
  }
  const deltaMinutes = Math.round((then - now) / 60_000);
  if (Math.abs(deltaMinutes) < 60) {
    if (deltaMinutes >= 0) {
      return `in ${deltaMinutes} min`;
    }
    return `${Math.abs(deltaMinutes)} min overdue`;
  }
  return formatUpdatedAt(value);
}

export function formatObservationAge(iso: string, now = Date.now()): string {
  return formatDataAge(iso, now);
}

export function formatSignalValue(recommendation: Pick<ReliabilityRecommendation, 'value' | 'unit'>): string | null {
  if (recommendation.value == null) {
    return null;
  }
  return recommendation.unit
    ? `${recommendation.value} ${recommendation.unit}`
    : String(recommendation.value);
}

export function priorityTone(priority: MaintenancePriority): StatusTone {
  if (priority === 'P1') {
    return 'hold';
  }
  if (priority === 'P2') {
    return 'warning';
  }
  return 'closed';
}

export function workOrderStatusTone(status: MaintenanceWorkOrderStatus): StatusTone {
  if (status === 'READY_FOR_TESTING') {
    return 'operating';
  }
  if (status === 'AWAITING_INSPECTION' || status === 'IN_PROGRESS') {
    return 'warning';
  }
  if (status === 'CANCELED') {
    return 'closed';
  }
  if (status === 'COMPLETED') {
    return 'operating';
  }
  return 'hold';
}

export function recommendationSeverityTone(
  severity: MaintenanceRecommendationSeverity,
): StatusTone {
  if (severity === 'CRITICAL') {
    return 'hold';
  }
  if (severity === 'WARNING') {
    return 'warning';
  }
  return 'closed';
}

export function serviceStatusTone(status: AssetServiceStatus): StatusTone {
  if (status === 'IN_SERVICE') {
    return 'operating';
  }
  if (status === 'RESTRICTED') {
    return 'warning';
  }
  return 'closed';
}

export function assignmentLabel(workOrder: {
  assignedTeam: string | null;
  assignedActorSubject: string | null;
}): string {
  if (workOrder.assignedTeam && workOrder.assignedActorSubject) {
    return `${workOrder.assignedTeam} · ${workOrder.assignedActorSubject}`;
  }
  return workOrder.assignedTeam ?? workOrder.assignedActorSubject ?? 'Unassigned';
}

export function isSafeExternalUri(uri: string): boolean {
  try {
    const parsed = new URL(uri);
    return parsed.protocol === 'https:' || parsed.protocol === 'http:';
  } catch {
    return false;
  }
}

export function datetimeLocalValue(iso: string | null): string {
  if (!iso) {
    return '';
  }
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return '';
  }
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export function fromDatetimeLocal(value: string): string | null {
  if (!value) {
    return null;
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return null;
  }
  return date.toISOString();
}
