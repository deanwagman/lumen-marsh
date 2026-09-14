import type {
  IncidentActivity,
  IncidentSeverity,
  IncidentStatus,
  IncidentType,
} from './incident';

const activityLabels: Record<string, string> = {
  INCIDENT_REPORTED: 'Incident reported',
  INCIDENT_ACKNOWLEDGED: 'Incident acknowledged',
  INCIDENT_ASSIGNED: 'Incident assigned',
  MITIGATION_STARTED: 'Mitigation started',
  SEVERITY_CHANGED: 'Severity changed',
  ATTRACTION_LINKED: 'Attraction linked',
  ATTRACTION_UNLINKED: 'Attraction unlinked',
  GUEST_ADVISORY_PUBLISHED: 'Guest advisory published',
  GUEST_ADVISORY_WITHDRAWN: 'Guest advisory withdrawn',
  INCIDENT_RESOLVED: 'Incident resolved',
};

export function formatActivityType(type: string): string {
  return (
    activityLabels[type] ??
    type.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase())
  );
}

export function formatIncidentType(type: IncidentType): string {
  return type.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase());
}

export function formatIncidentStatus(status: IncidentStatus): string {
  return status.charAt(0) + status.slice(1).toLowerCase();
}

export function formatIncidentSeverity(severity: IncidentSeverity): string {
  return severity.charAt(0) + severity.slice(1).toLowerCase();
}

export function formatIncidentTime(value: string | null): string {
  if (!value) {
    return 'Unknown';
  }
  return new Date(value).toLocaleString();
}

export function formatIncidentVersion(version: number): string {
  return `v${version}`;
}

export function resolveAttractionLabels(
  attractionIds: string[],
  names: Map<string, string> | Record<string, string>,
): string[] {
  const lookup = names instanceof Map ? names : new Map(Object.entries(names));
  return attractionIds.map((id) => lookup.get(id) ?? id);
}

export function formatActivityDetails(
  activity: IncidentActivity,
  attractionNames: Map<string, string> = new Map(),
): string[] {
  const details: string[] = [];

  if (activity.incidentType || activity.severity) {
    const parts = [
      activity.incidentType ? formatIncidentType(activity.incidentType) : null,
      activity.severity ? formatIncidentSeverity(activity.severity) : null,
    ].filter(Boolean);
    if (parts.length > 0) {
      details.push(parts.join(' · '));
    }
  }

  if (activity.previousStatus || activity.newStatus) {
    details.push(
      [
        activity.previousStatus ? formatIncidentStatus(activity.previousStatus) : '—',
        activity.newStatus ? formatIncidentStatus(activity.newStatus) : '—',
      ].join(' → '),
    );
  }

  if (activity.previousSeverity || activity.newSeverity) {
    details.push(
      [
        activity.previousSeverity
          ? formatIncidentSeverity(activity.previousSeverity)
          : '—',
        activity.newSeverity ? formatIncidentSeverity(activity.newSeverity) : '—',
      ].join(' → '),
    );
  }

  if (activity.assignee) {
    details.push(`Assignee ${activity.assignee}`);
  }

  if (activity.attractionIds && activity.attractionIds.length > 0) {
    details.push(resolveAttractionLabels(activity.attractionIds, attractionNames).join(', '));
  }

  if (activity.attractionId) {
    details.push(attractionNames.get(activity.attractionId) ?? activity.attractionId);
  }

  if (activity.guestTitle) {
    details.push(activity.guestTitle);
  }

  if (activity.guestMessage) {
    details.push(activity.guestMessage);
  }

  if (activity.title && activity.type !== 'INCIDENT_REPORTED') {
    details.push(activity.title);
  }

  return details;
}
