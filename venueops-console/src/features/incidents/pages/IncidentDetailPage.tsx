import { Link, useNavigate, useParams } from 'react-router-dom';
import { useEffect, useRef } from 'react';

import { useAuth } from '@/auth/AuthContext';
import { VersionConflictBanner } from '@/features/attractions/components/CommandWorkspace';
import { useAttractions } from '@/features/attractions/hooks/useAttractions';
import { IncidentActivityTimeline } from '@/features/incidents/components/IncidentActivityTimeline';
import { IncidentCommandPanel } from '@/features/incidents/components/IncidentCommandPanel';
import { useIncidentActivity } from '@/features/incidents/hooks/useIncidentActivity';
import { useIncidentCommand } from '@/features/incidents/hooks/useIncidentCommand';
import { useIncidentDetail } from '@/features/incidents/hooks/useIncidentDetail';
import { useWeatherRecommendations } from '@/features/weather/hooks/useWeatherRecommendations';
import {
  formatIncidentSeverity,
  formatIncidentStatus,
  formatIncidentTime,
  formatIncidentType,
  formatIncidentVersion,
} from '@/features/incidents/domain/formatters';
import {
  advisoryStateLabel,
  severityTone,
  statusTone,
} from '@/features/incidents/domain/presentation';
import { formatLabel } from '@/features/attractions/domain/formatters';
import { ForbiddenError, NotFoundError, VersionConflictError } from '@/shared/api/errors';
import { Button } from '@/shared/ui/Button';
import { StatusBadge } from '@/shared/ui/StatusBadge';
import { HelpLink } from '@/shared/ui/HelpLink';

import styles from './IncidentDetailPage.module.css';

export default function IncidentDetailPage() {
  const { session } = useAuth();
  const { incidentId } = useParams();
  const navigate = useNavigate();
  const detail = useIncidentDetail(incidentId);
  const activity = useIncidentActivity(incidentId);
  const command = useIncidentCommand();
  const attractions = useAttractions();
  const weather = useWeatherRecommendations();
  const headingRef = useRef<HTMLHeadingElement>(null);
  const catalog = attractions.data ?? [];
  const names = new Map(catalog.map((attraction) => [attraction.id, attraction.name]));
  const linkedRecommendation = weather.data?.find(
    (recommendation) => recommendation.linkedIncidentId === incidentId,
  );

  useEffect(() => {
    if (!detail.data) {
      return;
    }
    headingRef.current?.focus();
  }, [detail.data]);

  if (detail.isPending) {
    return (
      <section className={styles.page} aria-busy="true">
        <p className={styles.kicker}>Incident workspace</p>
        <div className={styles.skeleton} role="status" aria-label="Loading incident">
          <div className={styles.skeletonBlock} />
          <div className={styles.skeletonBlock} />
          <div className={styles.skeletonBlock} />
        </div>
      </section>
    );
  }

  if (detail.isError) {
    return (
      <section className={styles.page}>
        <Link className={styles.back} to="/incidents">
          ← Incidents
        </Link>
        <h1>
          {detail.error instanceof NotFoundError
            ? 'Incident not found'
            : 'Unable to load incident'}
        </h1>
        <p className={styles.muted}>
          {detail.error instanceof NotFoundError
            ? 'The incident may have been removed or the address may be incorrect.'
            : detail.error.message}
        </p>
        {detail.error instanceof NotFoundError ? (
          <Link to="/incidents">Return to incidents</Link>
        ) : (
          <Button onClick={() => void detail.refetch()}>Retry</Button>
        )}
      </section>
    );
  }

  const incident = detail.data;
  if (!incident) {
    return null;
  }

  return (
    <section className={styles.page}>
      <button type="button" className={styles.back} onClick={() => navigate(-1)}>
        ← Incidents
      </button>
      <header className={styles.header}>
        <div>
          <p className={styles.kicker}>Incident workspace</p>
          <h1 ref={headingRef} tabIndex={-1}>
            {incident.title}
          </h1>
          <p className={styles.typeLine}>{formatIncidentType(incident.type)}</p>
        </div>
        <div className={styles.badges}>
          <StatusBadge
            tone={severityTone(incident.severity)}
            label={formatIncidentSeverity(incident.severity)}
          />
          <StatusBadge
            tone={statusTone(incident.status)}
            label={formatIncidentStatus(incident.status)}
          />
        </div>
      </header>
      <dl className={styles.facts}>
        <div>
          <dt>Status</dt>
          <dd>{formatIncidentStatus(incident.status)}</dd>
        </div>
        <div>
          <dt>Severity</dt>
          <dd>{formatIncidentSeverity(incident.severity)}</dd>
        </div>
        <div>
          <dt>Type</dt>
          <dd>{formatIncidentType(incident.type)}</dd>
        </div>
        <div>
          <dt>Assignment</dt>
          <dd>{incident.assignedTo ?? 'Unassigned'}</dd>
        </div>
        <div>
          <dt>Created</dt>
          <dd>{formatIncidentTime(incident.createdAt)}</dd>
        </div>
        <div>
          <dt>Updated</dt>
          <dd>{formatIncidentTime(incident.updatedAt)}</dd>
        </div>
        <div>
          <dt>Version</dt>
          <dd>{formatIncidentVersion(incident.version)}</dd>
        </div>
      </dl>
      <section className={styles.panel} aria-labelledby="internal-description-heading">
        <h2 id="internal-description-heading">Internal description</h2>
        <p>
          {incident.internalDescription ?? 'No internal description recorded for this incident.'}
        </p>
        <p className={styles.muted}>
          Internal notes stay inside the authenticated operator console and are never guest-facing.
        </p>
      </section>
      <section className={styles.panel} aria-labelledby="linked-attractions-heading">
        <h2 id="linked-attractions-heading">Affected attractions</h2>
        {incident.attractionIds.length === 0 ? <p>None linked.</p> : null}
        <ul className={styles.attractionList}>
          {incident.attractionIds.map((id) => (
            <li key={id}>
              <Link to={`/attractions/${id}`}>{names.get(id) ?? id}</Link>
            </li>
          ))}
        </ul>
        <p className={styles.muted}>
          Incident links do not change attraction operating state.
        </p>
      </section>
      <section className={styles.panel} aria-labelledby="guest-advisory-readonly-heading">
        <div className={styles.panelHeading}>
          <h2 id="guest-advisory-readonly-heading">Guest advisory</h2>
          <HelpLink
            article="incidents"
            section={session?.role === 'operator' ? 'advisories-operators' : 'advisories'}
            label="Help: guest advisories"
          />
        </div>
        <p>{advisoryStateLabel(incident.guestAdvisoryPublished)}</p>
        {incident.guestAdvisoryPublished ? (
          <blockquote className={styles.advisory}>
            <p>{incident.guestTitle}</p>
            <p>{incident.guestMessage}</p>
          </blockquote>
        ) : (
          <p className={styles.muted}>No guest-facing message is active for this incident.</p>
        )}
      </section>
      {linkedRecommendation ? (
        <section className={styles.panel} aria-labelledby="source-recommendation-heading">
          <h2 id="source-recommendation-heading">Source recommendation</h2>
          <p className={styles.recommendationTitle}>{linkedRecommendation.summary}</p>
          <p className={styles.muted}>
            {formatLabel(linkedRecommendation.severity)} ·{' '}
            {formatLabel(linkedRecommendation.status)} ·{' '}
            {formatLabel(linkedRecommendation.operatorStatus)}
          </p>
        </section>
      ) : null}
      {command.error instanceof VersionConflictError ? (
        <VersionConflictBanner
          error={command.error}
          reloading={detail.isFetching}
          onReload={() => {
            command.reset();
            void detail.refetch();
            void activity.refetch();
          }}
        />
      ) : null}
      {command.error instanceof ForbiddenError ? (
        <p role="alert">Access denied. Loaded incident data was kept.</p>
      ) : null}
      <div className={styles.workspace}>
        <IncidentCommandPanel
          session={session}
          incident={incident}
          attractions={catalog}
          pending={command.isPending}
          error={command.error ?? null}
          onCommand={(input) =>
            command.mutate({
              incidentId: incident.id,
              expectedVersion: incident.version,
              ...input,
            })
          }
        />
        <section aria-labelledby="incident-activity-heading">
          <h2 id="incident-activity-heading">Activity</h2>
          <IncidentActivityTimeline
            activity={activity.data}
            isPending={activity.isPending}
            isError={activity.isError}
            error={activity.error ?? null}
            attractionNames={names}
            onRetry={() => void activity.refetch()}
            retrying={activity.isFetching}
          />
        </section>
      </div>
    </section>
  );
}
