import type { Incident } from '@/features/incidents/domain/incident';

export type IncidentCommandBody = {
  type: string;
  expectedVersion: number;
  guestTitle?: string;
  guestMessage?: string;
  reason?: string;
  assignee?: string;
  severity?: Incident['severity'];
  attractionId?: string;
};

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

  switch (command.type) {
    case 'ACKNOWLEDGE':
      return { ...base, status: 'ACKNOWLEDGED' };
    case 'START_MITIGATION':
      return { ...base, status: 'MITIGATING' };
    case 'ASSIGN':
      return { ...base, assignedTo: command.assignee?.trim() || incident.assignedTo };
    case 'CHANGE_SEVERITY':
      return {
        ...base,
        severity: command.severity ?? incident.severity,
      };
    case 'LINK_ATTRACTION': {
      const attractionId = command.attractionId;
      if (!attractionId || incident.attractionIds.includes(attractionId)) {
        return base;
      }
      return {
        ...base,
        attractionIds: [...incident.attractionIds, attractionId],
      };
    }
    case 'UNLINK_ATTRACTION': {
      const attractionId = command.attractionId;
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
        guestTitle: command.guestTitle?.trim() || incident.guestTitle,
        guestMessage: command.guestMessage?.trim() || incident.guestMessage,
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
