import type { AuthSession, OperatorRole } from '@/auth/session';
import { hasVenueOpsScope, venueOpsScopes } from '@/auth/session';

export const incidentTypes = [
  'WEATHER',
  'TECHNICAL',
  'MEDICAL',
  'OPERATIONS',
  'GUEST_EXPERIENCE',
] as const;

export const incidentSeverities = ['MINOR', 'MODERATE', 'MAJOR', 'CRITICAL'] as const;

export const incidentStatuses = [
  'REPORTED',
  'ACKNOWLEDGED',
  'MITIGATING',
  'RESOLVED',
] as const;

export type IncidentType = (typeof incidentTypes)[number];
export type IncidentSeverity = (typeof incidentSeverities)[number];
export type IncidentStatus = (typeof incidentStatuses)[number];

export type Incident = {
  id: string;
  title: string;
  type: IncidentType;
  severity: IncidentSeverity;
  status: IncidentStatus;
  internalDescription: string | null;
  assignedTo: string | null;
  guestAdvisoryPublished: boolean;
  guestTitle: string | null;
  guestMessage: string | null;
  attractionIds: string[];
  createdAt: string | null;
  updatedAt: string;
  version: number;
};

export type IncidentOperationalSnapshot = {
  id: string;
  title: string;
  type: IncidentType;
  severity: IncidentSeverity;
  status: IncidentStatus;
  assignedTo: string | null;
  guestAdvisoryPublished: boolean;
  guestTitle: string | null;
  guestMessage: string | null;
  affectedAttractionIds: string[];
  updatedAt: string;
  version: number;
};

export type IncidentActivity = {
  id: string;
  incidentId: string;
  type: string;
  actor: string;
  reason: string | null;
  occurredAt: string;
  previousVersion: number;
  resultingVersion: number;
  title?: string | null;
  incidentType?: IncidentType | null;
  severity?: IncidentSeverity | null;
  attractionIds?: string[];
  previousStatus?: IncidentStatus | null;
  newStatus?: IncidentStatus | null;
  assignee?: string | null;
  previousSeverity?: IncidentSeverity | null;
  newSeverity?: IncidentSeverity | null;
  attractionId?: string | null;
  guestTitle?: string | null;
  guestMessage?: string | null;
};

export function isOpenIncident(incident: Pick<Incident, 'status'>): boolean {
  return incident.status !== 'RESOLVED';
}

export function isSupervisor(role: OperatorRole | undefined): boolean {
  return role === 'supervisor';
}

export function canViewIncidents(session: AuthSession | null | undefined): boolean {
  return (
    hasVenueOpsScope(session, venueOpsScopes.operatorRead) &&
    (session?.role === 'operator' || session?.role === 'supervisor')
  );
}

export function canCommandIncidents(session: AuthSession | null | undefined): boolean {
  return (
    hasVenueOpsScope(session, venueOpsScopes.incidentsCommand) &&
    (session?.role === 'operator' || session?.role === 'supervisor')
  );
}

export function canPublishAdvisory(session: AuthSession | null | undefined): boolean {
  return (
    hasVenueOpsScope(session, venueOpsScopes.advisoriesPublish) &&
    session?.role === 'supervisor'
  );
}

export function canResolve(
  session: AuthSession | null | undefined,
  severity: IncidentSeverity,
): boolean {
  if (!canCommandIncidents(session)) {
    return false;
  }
  if (severity === 'MAJOR' || severity === 'CRITICAL') {
    return session?.role === 'supervisor';
  }
  return true;
}

const severityRank: Record<IncidentSeverity, number> = {
  CRITICAL: 0,
  MAJOR: 1,
  MODERATE: 2,
  MINOR: 3,
};

export function compareIncidents(left: Incident, right: Incident): number {
  const openDelta = Number(isOpenIncident(right)) - Number(isOpenIncident(left));
  if (openDelta !== 0) {
    return openDelta;
  }
  const severityDelta = severityRank[left.severity] - severityRank[right.severity];
  if (severityDelta !== 0) {
    return severityDelta;
  }
  const updatedDelta = right.updatedAt.localeCompare(left.updatedAt);
  if (updatedDelta !== 0) {
    return updatedDelta;
  }
  return left.id.localeCompare(right.id);
}

export type IncidentListFilter = {
  status: 'open' | IncidentStatus | 'all';
  severity: IncidentSeverity | 'all';
  type: IncidentType | 'all';
  assignment: 'all' | 'assigned' | 'unassigned';
  attractionId: string | 'all';
};

export const defaultIncidentFilter: IncidentListFilter = {
  status: 'open',
  severity: 'all',
  type: 'all',
  assignment: 'all',
  attractionId: 'all',
};

export function filterIncidents(incidents: Incident[], filter: IncidentListFilter): Incident[] {
  return incidents.filter((incident) => {
    if (filter.status === 'open' && !isOpenIncident(incident)) {
      return false;
    }
    if (filter.status !== 'open' && filter.status !== 'all' && incident.status !== filter.status) {
      return false;
    }
    if (filter.severity !== 'all' && incident.severity !== filter.severity) {
      return false;
    }
    if (filter.type !== 'all' && incident.type !== filter.type) {
      return false;
    }
    if (filter.assignment === 'assigned' && !incident.assignedTo) {
      return false;
    }
    if (filter.assignment === 'unassigned' && incident.assignedTo) {
      return false;
    }
    if (filter.attractionId !== 'all' && !incident.attractionIds.includes(filter.attractionId)) {
      return false;
    }
    return true;
  });
}

export function summarizeIncidents(incidents: Incident[]) {
  const open = incidents.filter(isOpenIncident);
  return {
    open: open.length,
    criticalOrMajor: open.filter(
      (incident) => incident.severity === 'CRITICAL' || incident.severity === 'MAJOR',
    ).length,
    unassigned: open.filter((incident) => !incident.assignedTo).length,
    publishedAdvisories: incidents.filter((incident) => incident.guestAdvisoryPublished).length,
  };
}

export function fromOperationalSnapshot(
  snapshot: IncidentOperationalSnapshot,
  previous?: Incident,
): Incident {
  return {
    id: snapshot.id,
    title: snapshot.title,
    type: snapshot.type,
    severity: snapshot.severity,
    status: snapshot.status,
    internalDescription: previous?.internalDescription ?? null,
    assignedTo: snapshot.assignedTo,
    guestAdvisoryPublished: snapshot.guestAdvisoryPublished,
    guestTitle: snapshot.guestTitle,
    guestMessage: snapshot.guestMessage,
    attractionIds: snapshot.affectedAttractionIds,
    createdAt: previous?.createdAt ?? null,
    updatedAt: snapshot.updatedAt,
    version: snapshot.version,
  };
}

export function applyNewerIncident(current: Incident | undefined, next: Incident): Incident | undefined {
  if (!current) {
    return next;
  }
  if (next.version < current.version) {
    return current;
  }
  return {
    ...next,
    internalDescription: next.internalDescription ?? current.internalDescription,
    createdAt: next.createdAt ?? current.createdAt,
  };
}
