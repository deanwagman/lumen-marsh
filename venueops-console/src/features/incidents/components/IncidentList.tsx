import { Link } from 'react-router-dom';

import type { AttractionSummary } from '@/features/attractions/domain/attraction';
import type { Incident } from '@/features/incidents/domain/incident';
import {
  formatIncidentSeverity,
  formatIncidentStatus,
  formatIncidentTime,
  formatIncidentType,
  formatIncidentVersion,
  resolveAttractionLabels,
} from '@/features/incidents/domain/formatters';
import { advisoryLabel, severityTone, statusTone } from '@/features/incidents/domain/presentation';
import { StatusBadge } from '@/shared/ui/StatusBadge';

import styles from './IncidentList.module.css';

export function IncidentList({
  incidents,
  attractions,
}: {
  incidents: Incident[];
  attractions: AttractionSummary[];
}) {
  const names = new Map(attractions.map((attraction) => [attraction.id, attraction.name]));

  if (incidents.length === 0) {
    return (
      <div className={styles.emptyState}>
        <h2>No incidents match this view</h2>
        <p className={styles.empty}>Adjust or clear the current filters.</p>
      </div>
    );
  }

  return (
    <ul className={styles.list}>
      {incidents.map((incident) => {
        const attractionLabels = resolveAttractionLabels(incident.attractionIds, names);
        const accessibleName = [
          incident.title,
          formatIncidentType(incident.type),
          formatIncidentSeverity(incident.severity),
          formatIncidentStatus(incident.status),
          incident.assignedTo ?? 'Unassigned',
        ].join(', ');

        return (
          <li key={incident.id} className={styles.row}>
            <Link
              className={styles.link}
              to={`/incidents/${incident.id}`}
              aria-label={accessibleName}
            >
              <span className={styles.title}>{incident.title}</span>
              <span className={styles.meta}>
                {formatIncidentType(incident.type)} · {formatIncidentSeverity(incident.severity)} ·{' '}
                {formatIncidentStatus(incident.status)}
              </span>
              <span className={styles.badges}>
                <StatusBadge
                  tone={severityTone(incident.severity)}
                  label={formatIncidentSeverity(incident.severity)}
                />
                <StatusBadge
                  tone={statusTone(incident.status)}
                  label={formatIncidentStatus(incident.status)}
                />
              </span>
              <span>{incident.assignedTo ?? 'Unassigned'}</span>
              <span>{attractionLabels.join(', ') || 'No attractions'}</span>
              <span>{advisoryLabel(incident.guestAdvisoryPublished)}</span>
              <span>
                Updated {formatIncidentTime(incident.updatedAt)} ·{' '}
                {formatIncidentVersion(incident.version)}
              </span>
            </Link>
          </li>
        );
      })}
    </ul>
  );
}
