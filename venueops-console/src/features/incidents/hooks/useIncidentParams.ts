import { useSearchParams } from 'react-router-dom';

import {
  defaultIncidentFilter,
  incidentSeverities,
  incidentStatuses,
  incidentTypes,
  type IncidentListFilter,
  type IncidentSeverity,
  type IncidentType,
} from '@/features/incidents/domain/incident';

const statuses = new Set<string>(['open', 'all', ...incidentStatuses]);
const severities = new Set<string>(incidentSeverities);
const types = new Set<string>(incidentTypes);

export function useIncidentParams() {
  const [params, setParams] = useSearchParams();

  const status = parseStatus(params.get('status'));
  const severity = parseSeverity(params.get('severity'));
  const type = parseType(params.get('type'));
  const assignment = parseAssignment(params.get('assignment'));
  const attractionId = params.get('attraction') || 'all';

  function update(next: Partial<IncidentListFilter>) {
    const merged: IncidentListFilter = {
      status: next.status ?? status,
      severity: next.severity ?? severity,
      type: next.type ?? type,
      assignment: next.assignment ?? assignment,
      attractionId: next.attractionId ?? attractionId,
    };
    const nextParams = new URLSearchParams(params);
    setOrDelete(nextParams, 'status', merged.status, defaultIncidentFilter.status);
    setOrDelete(nextParams, 'severity', merged.severity, 'all');
    setOrDelete(nextParams, 'type', merged.type, 'all');
    setOrDelete(nextParams, 'assignment', merged.assignment, 'all');
    setOrDelete(nextParams, 'attraction', merged.attractionId, 'all');
    setParams(nextParams, { replace: true });
  }

  return {
    filter: { status, severity, type, assignment, attractionId } satisfies IncidentListFilter,
    setFilter: update,
  };
}

function setOrDelete(
  params: URLSearchParams,
  key: string,
  value: string,
  defaultValue: string,
) {
  if (value === defaultValue) {
    params.delete(key);
  } else {
    params.set(key, value);
  }
}

function parseStatus(value: string | null): IncidentListFilter['status'] {
  if (value && statuses.has(value)) {
    return value as IncidentListFilter['status'];
  }
  return defaultIncidentFilter.status;
}

function parseSeverity(value: string | null): IncidentSeverity | 'all' {
  if (value && severities.has(value)) {
    return value as IncidentSeverity;
  }
  return 'all';
}

function parseType(value: string | null): IncidentType | 'all' {
  if (value && types.has(value)) {
    return value as IncidentType;
  }
  return 'all';
}

function parseAssignment(value: string | null): IncidentListFilter['assignment'] {
  if (value === 'assigned' || value === 'unassigned') {
    return value;
  }
  return 'all';
}
