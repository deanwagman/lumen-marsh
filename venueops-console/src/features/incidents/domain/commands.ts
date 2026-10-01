import type { AuthSession } from '@/auth/session';

import {
  canCommandIncidents,
  canPublishAdvisory,
  canResolve,
  type Incident,
  type IncidentSeverity,
} from './incident';

export const incidentCommands = [
  'ACKNOWLEDGE',
  'ASSIGN',
  'START_MITIGATION',
  'CHANGE_SEVERITY',
  'LINK_ATTRACTION',
  'UNLINK_ATTRACTION',
  'PUBLISH_GUEST_ADVISORY',
  'WITHDRAW_GUEST_ADVISORY',
  'RESOLVE',
] as const;

export type IncidentCommandType = (typeof incidentCommands)[number];

export type IncidentAction = {
  type: IncidentCommandType;
  label: string;
  hint?: string;
};

export function lifecycleHint(incident: Incident): string | null {
  if (incident.status === 'REPORTED') {
    return 'Acknowledge this incident before mitigation can start.';
  }
  if (incident.status === 'ACKNOWLEDGED') {
    return 'Start mitigation before this incident can be resolved.';
  }
  if (incident.status === 'RESOLVED') {
    return 'This incident is resolved and read-only.';
  }
  return null;
}

export function availableIncidentActions(
  session: AuthSession | null | undefined,
  incident: Incident,
): IncidentAction[] {
  if (incident.status === 'RESOLVED') {
    return [];
  }

  const actions: IncidentAction[] = [];
  if (canCommandIncidents(session)) {
    if (incident.status === 'REPORTED') {
      actions.push({ type: 'ACKNOWLEDGE', label: 'Acknowledge' });
    }
    if (incident.status === 'ACKNOWLEDGED') {
      actions.push({ type: 'START_MITIGATION', label: 'Start mitigation' });
    }
    actions.push({ type: 'ASSIGN', label: 'Assign' });
    actions.push({ type: 'CHANGE_SEVERITY', label: 'Change severity' });
    actions.push({ type: 'LINK_ATTRACTION', label: 'Link attraction' });
    if (incident.attractionIds.length > 0) {
      actions.push({ type: 'UNLINK_ATTRACTION', label: 'Unlink attraction' });
    }
    if (incident.status === 'MITIGATING' && canResolve(session, incident.severity)) {
      actions.push({ type: 'RESOLVE', label: 'Resolve' });
    }
  }
  if (canPublishAdvisory(session) && !incident.guestAdvisoryPublished) {
    actions.push({ type: 'PUBLISH_GUEST_ADVISORY', label: 'Publish guest advisory' });
  }
  if (canPublishAdvisory(session) && incident.guestAdvisoryPublished) {
    actions.push({ type: 'WITHDRAW_GUEST_ADVISORY', label: 'Withdraw guest advisory' });
  }
  return actions;
}

export function resolveBlockedReason(
  session: AuthSession | null | undefined,
  incident: Incident,
): string | null {
  if (incident.status !== 'MITIGATING') {
    return null;
  }
  if (canResolve(session, incident.severity)) {
    return null;
  }
  if (incident.severity === 'MAJOR' || incident.severity === 'CRITICAL') {
    return 'A supervisor must resolve major and critical incidents.';
  }
  return null;
}

export function nextSeverityOptions(current: IncidentSeverity): IncidentSeverity[] {
  return (['MINOR', 'MODERATE', 'MAJOR', 'CRITICAL'] as const).filter(
    (severity) => severity !== current,
  );
}

export function newCommandId(): string {
  return crypto.randomUUID();
}
