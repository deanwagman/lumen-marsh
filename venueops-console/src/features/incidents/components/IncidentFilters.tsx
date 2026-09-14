import type { AttractionSummary } from '@/features/attractions/domain/attraction';
import {
  incidentSeverities,
  incidentStatuses,
  incidentTypes,
  type IncidentListFilter,
} from '@/features/incidents/domain/incident';
import {
  formatIncidentSeverity,
  formatIncidentStatus,
  formatIncidentType,
} from '@/features/incidents/domain/formatters';

import styles from './IncidentFilters.module.css';

export function IncidentFilters({
  filter,
  attractions,
  onChange,
}: {
  filter: IncidentListFilter;
  attractions: AttractionSummary[];
  onChange: (next: Partial<IncidentListFilter>) => void;
}) {
  return (
    <form className={styles.filters} aria-label="Incident filters">
      <label>
        Status
        <select
          value={filter.status}
          onChange={(event) =>
            onChange({ status: event.target.value as IncidentListFilter['status'] })
          }
        >
          <option value="open">Open</option>
          {incidentStatuses.map((status) => (
            <option key={status} value={status}>
              {formatIncidentStatus(status)}
            </option>
          ))}
          <option value="all">All</option>
        </select>
      </label>
      <label>
        Severity
        <select
          value={filter.severity}
          onChange={(event) =>
            onChange({ severity: event.target.value as IncidentListFilter['severity'] })
          }
        >
          <option value="all">All severities</option>
          {incidentSeverities.map((severity) => (
            <option key={severity} value={severity}>
              {formatIncidentSeverity(severity)}
            </option>
          ))}
        </select>
      </label>
      <label>
        Type
        <select
          value={filter.type}
          onChange={(event) => onChange({ type: event.target.value as IncidentListFilter['type'] })}
        >
          <option value="all">All types</option>
          {incidentTypes.map((type) => (
            <option key={type} value={type}>
              {formatIncidentType(type)}
            </option>
          ))}
        </select>
      </label>
      <label>
        Assignment
        <select
          value={filter.assignment}
          onChange={(event) =>
            onChange({ assignment: event.target.value as IncidentListFilter['assignment'] })
          }
        >
          <option value="all">All assignments</option>
          <option value="assigned">Assigned</option>
          <option value="unassigned">Unassigned</option>
        </select>
      </label>
      <label>
        Attraction
        <select
          value={filter.attractionId}
          onChange={(event) => onChange({ attractionId: event.target.value })}
        >
          <option value="all">All attractions</option>
          {attractions.map((attraction) => (
            <option key={attraction.id} value={attraction.id}>
              {attraction.name}
            </option>
          ))}
        </select>
      </label>
    </form>
  );
}
