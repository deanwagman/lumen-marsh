import type { Incident } from '@/features/incidents/domain/incident';

export type IncidentCommandBody = {
  type: string;
  expectedVersion: number;
  commandId?: string;
  reason?: string;
  data?: Record<string, unknown>;
  guestTitle?: string;
  guestMessage?: string;
  assignee?: string;
  severity?: Incident['severity'];
  attractionId?: string;
  confirmActiveWorkOrders?: boolean;
};

function field<T>(command: IncidentCommandBody, key: string): T | undefined {
  const fromData = command.data?.[key];
  if (fromData !== undefined) {
    return fromData as T;
  }
  return (command as Record<string, unknown>)[key] as T | undefined;
}

/**
 * Applies incident command side effects for local MSW / demos.
 * Keeps advisory publish/withdraw/resolve behavior aligned with the API.
 */
export function applyIncidentCommandSideEffects(
  incident: Incident,
  command: IncidentCommandBody,
  updatedAt = new Date().toISOString(),
): Incident {
  const nextVersion = incident.version + 1;
  const base = {
    ...incident,
    version: nextVersion,
    updatedAt,
  };
  const assignee = field<string>(command, 'assignee');
  const severity = field<Incident['severity']>(command, 'severity');
  const attractionId = field<string>(command, 'attractionId');
  const guestTitle = field<string>(command, 'guestTitle');
  const guestMessage = field<string>(command, 'guestMessage');

  switch (command.type) {
    case 'ACKNOWLEDGE':
      return { ...base, status: 'ACKNOWLEDGED' };
    case 'START_MITIGATION':
      return { ...base, status: 'MITIGATING' };
    case 'ASSIGN':
      return { ...base, assignedTo: assignee?.trim() || incident.assignedTo };
    case 'CHANGE_SEVERITY':
      return {
        ...base,
        severity: severity ?? incident.severity,
      };
    case 'LINK_ATTRACTION': {
      if (!attractionId || incident.attractionIds.includes(attractionId)) {
        return base;
      }
      return {
        ...base,
        attractionIds: [...incident.attractionIds, attractionId],
      };
    }
    case 'UNLINK_ATTRACTION': {
      if (!attractionId) {
        return base;
      }
      return {
        ...base,
        attractionIds: incident.attractionIds.filter((id) => id !== attractionId),
      };
    }
    case 'PUBLISH_GUEST_ADVISORY':
      return {
        ...base,
        guestAdvisoryPublished: true,
        guestTitle: guestTitle?.trim() || incident.guestTitle,
        guestMessage: guestMessage?.trim() || incident.guestMessage,
      };
    case 'WITHDRAW_GUEST_ADVISORY':
      return {
        ...base,
        guestAdvisoryPublished: false,
        guestTitle: null,
        guestMessage: null,
      };
    case 'RESOLVE':
      return {
        ...base,
        status: 'RESOLVED',
        guestAdvisoryPublished: false,
        guestTitle: null,
        guestMessage: null,
      };
    default:
      return base;
  }
}
