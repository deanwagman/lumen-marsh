import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';

import { useAuth } from '@/auth/AuthContext';
import { useAttractionStreamHealth } from '@/features/attractions/hooks/useAttractionStream';
import { formatCapacity, formatLabel, formatStatus, formatWaitTime } from '@/features/attractions/domain/formatters';
import { statusTone } from '@/features/attractions/domain/attraction';
import { streamStatusLabel, type StreamStatus } from '@/features/attractions/domain/streamHealth';
import { ShiftHandoffDialog } from '@/features/dashboard/components/ShiftHandoffDialog';
import {
  activityDomainLabels,
  attentionKindLabels,
  dashboardMetrics,
  type DashboardAttentionKind,
  type OperatorDashboard,
} from '@/features/dashboard/domain/dashboard';
import { formatShiftHandoff } from '@/features/dashboard/domain/handoff';
import { formatParkDate, formatParkDateTime, formatParkTime, formatSyncAge } from '@/features/dashboard/domain/parkTime';
import { useDashboard } from '@/features/dashboard/hooks/useDashboard';
import { useParkClock } from '@/features/dashboard/hooks/useParkClock';
import { useAttractions } from '@/features/attractions/hooks/useAttractions';
import {
  formatIncidentSeverity,
  formatIncidentStatus,
  formatIncidentTime,
} from '@/features/incidents/domain/formatters';
import { advisoryStateLabel, severityTone, statusTone as incidentStatusTone } from '@/features/incidents/domain/presentation';
import { formatDataAge } from '@/features/weather/domain/formatters';
import { severityTone as weatherSeverityTone } from '@/features/weather/domain/recommendation';
import { NetworkError } from '@/shared/api/errors';
import { Button } from '@/shared/ui/Button';
import { StatusBadge } from '@/shared/ui/StatusBadge';

import styles from './DashboardPage.module.css';

const attentionTone: Record<DashboardAttentionKind, 'hold' | 'warning' | 'operating' | 'closed'> = {
  CRITICAL_INCIDENT: 'hold',
  MAJOR_INCIDENT: 'hold',
  WEATHER_HAZARD: 'hold',
  WEATHER_HOLD: 'hold',
  ABNORMAL_ATTRACTION: 'warning',
  UNASSIGNED_INCIDENT: 'warning',
  UNACKNOWLEDGED_INCIDENT: 'warning',
  ORPHAN_ADVISORY: 'warning',
  STALE_DATA: 'closed',
};

export default function DashboardPage() {
  const auth = useAuth();
  const operator = auth.session!;
  const dashboard = useDashboard();
  const stream = useAttractionStreamHealth();
  const attractions = useAttractions();
  const now = useParkClock();
  const [handoffOpen, setHandoffOpen] = useState(false);
  const snapshot = dashboard.data;
  const names = new Map((attractions.data ?? []).map((attraction) => [attraction.id, attraction.name]));
  const announcement = useImportantChangeAnnouncement(snapshot);
  const showUnavailable = dashboard.isError && !snapshot;
  const refreshing = dashboard.isFetching && Boolean(snapshot);
  const reconnecting = stream.data.status === 'reconnecting';
  const stale =
    stream.data.status === 'stale' ||
    snapshot?.freshness.venueOps.status === 'STALE' ||
    snapshot?.freshness.environmentalData.status === 'STALE';

  return (
    <section className={styles.page} aria-labelledby="dashboard-heading" aria-busy={refreshing}>
      <header className={`${styles.header} print-hide`}>
        <div>
          <p className={styles.kicker}>Control Tower</p>
          <h1 id="dashboard-heading">Operations dashboard</h1>
          <p className={styles.muted}>Park-wide conditions for the current operating period.</p>
        </div>
        {snapshot ? (
          <Button onClick={() => setHandoffOpen(true)}>Generate shift handoff</Button>
        ) : null}
      </header>

      <div className={styles.liveRegion} aria-live="polite" aria-atomic="true">
        {announcement}
      </div>

      {snapshot ? (
        <OperationalHeader
          now={now}
          dashboard={snapshot}
          connectionStatus={stream.data.status}
          lastEventAt={stream.data.lastEventAt}
          operatorName={operator.displayName}
          operatorRole={operator.role}
          operatorId={operator.operatorId}
          reconnecting={reconnecting}
          stale={stale && !reconnecting}
          refreshing={refreshing}
        />
      ) : null}

      {dashboard.isPending && !snapshot ? <LoadingState /> : null}

      {showUnavailable ? (
        <UnavailableState
          error={dashboard.error}
          onRetry={() => {
            void dashboard.refetch();
          }}
          retrying={dashboard.isFetching}
        />
      ) : null}

      {dashboard.isError && snapshot ? (
        <div className={`${styles.banner} ${styles.bannerWarning}`} role="status">
          <p>Could not refresh the dashboard. Showing the last successful snapshot.</p>
          <Button onClick={() => void dashboard.refetch()} disabled={dashboard.isFetching}>
            Retry
          </Button>
        </div>
      ) : null}

      {snapshot ? (
        <>
          <ParkStatusMetrics summary={snapshot.summary} />
          <NeedsAttention items={snapshot.needsAttention} />
          <div className={styles.secondary}>
            <ActiveIncidents incidents={snapshot.openIncidents} names={names} />
            <AttractionConditions attractions={snapshot.attractionsNeedingAttention} />
            <WeatherRecommendations items={snapshot.pendingWeatherRecommendations} names={names} />
            <PublishedAdvisories advisories={snapshot.publishedGuestAdvisories} names={names} />
          </div>
          <RecentActivity events={snapshot.recentActivity} />
        </>
      ) : null}

      {handoffOpen && snapshot ? (
        <ShiftHandoffDialog
          summary={formatShiftHandoff(snapshot, {
            generatedBy: operator.displayName,
            connectionStatus: stream.data.status,
            lastEventAt: stream.data.lastEventAt,
          })}
          onClose={() => setHandoffOpen(false)}
        />
      ) : null}
    </section>
  );
}

function OperationalHeader({
  now,
  dashboard,
  connectionStatus,
  lastEventAt,
  operatorName,
  operatorRole,
  operatorId,
  reconnecting,
  stale,
  refreshing,
}: {
  now: Date;
  dashboard: OperatorDashboard;
  connectionStatus: StreamStatus;
  lastEventAt: number | null;
  operatorName: string;
  operatorRole: string;
  operatorId: string;
  reconnecting: boolean;
  stale: boolean;
  refreshing: boolean;
}) {
  const lastEventIso = lastEventAt ? new Date(lastEventAt).toISOString() : null;
  const lastEventAge = formatSyncAge(lastEventIso);
  const snapshotAge = formatSyncAge(dashboard.generatedAt);

  return (
    <div className={styles.headerCluster}>
      <dl className={styles.meta}>
        <div>
          <dt>Park date</dt>
          <dd>{formatParkDate(now)}</dd>
        </div>
        <div>
          <dt>Park time</dt>
          <dd>
            <time dateTime={now.toISOString()}>{formatParkTime(now)}</time>
          </dd>
        </div>
        <div>
          <dt>Live connection</dt>
          <dd>
            <span className={styles.connection}>
              <span className={`${styles.connectionDot} ${styles[connectionStatus]}`} aria-hidden="true" />
              {streamStatusLabel(connectionStatus)}
            </span>
          </dd>
        </div>
        <div>
          <dt>Snapshot generated</dt>
          <dd>{formatParkDateTime(dashboard.generatedAt)}</dd>
        </div>
        <div>
          <dt>Last live event</dt>
          <dd>{lastEventAge ?? 'Waiting for live events'}</dd>
        </div>
        <div>
          <dt>Signed in</dt>
          <dd>
            {operatorName} · {operatorRole} · {operatorId}
          </dd>
        </div>
      </dl>
      {refreshing ? (
        <p className={styles.refreshing} role="status">
          Refreshing live snapshot
        </p>
      ) : null}
      {reconnecting ? (
        <div className={`${styles.banner} ${styles.bannerWarning}`} role="status">
          Live updates interrupted. Reconnecting…
        </div>
      ) : null}
      {stale ? (
        <div className={`${styles.banner} ${styles.bannerWarning}`} role="alert">
          <p>Some operational information may be out of date.</p>
          <p>{snapshotAge ?? lastEventAge ?? 'Last synchronized time is unknown.'}</p>
        </div>
      ) : null}
    </div>
  );
}

function ParkStatusMetrics({ summary }: { summary: OperatorDashboard['summary'] }) {
  return (
    <section className={styles.metrics} aria-labelledby="park-status-heading">
      <h2 id="park-status-heading">Park status</h2>
      <ul className={styles.metricGrid}>
        {dashboardMetrics.map((metric) => {
          const count = summary[metric.key];
          return (
            <li key={metric.key}>
              <Link className={styles.metric} to={metric.href} aria-label={metric.accessibleName(count)}>
                <span className={styles.metricValue}>{count}</span>
                <span className={styles.metricLabel}>{metric.label}</span>
              </Link>
            </li>
          );
        })}
      </ul>
    </section>
  );
}

function NeedsAttention({ items }: { items: OperatorDashboard['needsAttention'] }) {
  return (
    <section className={styles.attention} aria-labelledby="needs-attention-heading">
      <div className={styles.sectionHeader}>
        <h2 id="needs-attention-heading">Needs attention</h2>
        <p className={styles.count}>{items.length} open</p>
      </div>
      {items.length === 0 ? (
        <p className={styles.empty}>Nothing currently requires attention.</p>
      ) : (
        <ol className={styles.attentionList}>
          {items.map((item) => (
            <li key={`${item.kind}-${item.subjectId}`}>
              <Link className={styles.attentionItem} to={item.href}>
                <span className={styles.attentionIdentity}>
                  <StatusBadge tone={attentionTone[item.kind]} label={attentionKindLabels[item.kind]} />
                  <strong>{item.subjectLabel}</strong>
                </span>
                <span className={styles.muted}>{item.reason}</span>
                <span className={styles.muted}>
                  {item.updatedAt ? formatIncidentTime(item.updatedAt) : 'Time unknown'}
                </span>
              </Link>
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}

function ActiveIncidents({
  incidents,
  names,
}: {
  incidents: OperatorDashboard['openIncidents'];
  names: Map<string, string>;
}) {
  return (
    <section className={styles.panel} aria-labelledby="active-incidents-heading">
      <h2 id="active-incidents-heading">Active incidents</h2>
      {incidents.length === 0 ? (
        <p className={styles.empty}>No open incidents.</p>
      ) : (
        <ul className={styles.cardList}>
          {incidents.map((incident) => (
            <li key={incident.id}>
              <Link className={styles.cardLink} to={`/incidents/${incident.id}`}>
                <span className={styles.cardTitle}>{incident.title}</span>
                <span className={styles.badgeRow}>
                  <StatusBadge tone={severityTone(incident.severity)} label={formatIncidentSeverity(incident.severity)} />
                  <StatusBadge tone={incidentStatusTone(incident.status)} label={formatIncidentStatus(incident.status)} />
                  <StatusBadge
                    tone={incident.guestAdvisoryPublished ? 'warning' : 'closed'}
                    label={advisoryStateLabel(incident.guestAdvisoryPublished)}
                  />
                </span>
                <span className={styles.muted}>Assigned to {incident.assignedTo ?? 'Unassigned'}</span>
                <span className={styles.muted}>
                  Affected: {labels(incident.attractionIds, names) || 'None linked'}
                </span>
                <span className={styles.muted}>{formatIncidentTime(incident.updatedAt)}</span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

function AttractionConditions({
  attractions,
}: {
  attractions: OperatorDashboard['attractionsNeedingAttention'];
}) {
  return (
    <section className={styles.panel} aria-labelledby="attraction-conditions-heading">
      <h2 id="attraction-conditions-heading">Attraction conditions</h2>
      {attractions.length === 0 ? (
        <p className={styles.empty}>All attractions are operating normally.</p>
      ) : (
        <ul className={styles.cardList}>
          {attractions.map((attraction) => (
            <li key={attraction.id}>
              <Link className={styles.cardLink} to={`/attractions/${attraction.id}`}>
                <span className={styles.cardTitle}>{attraction.name}</span>
                <span className={styles.muted}>{attraction.area}</span>
                <span className={styles.badgeRow}>
                  <StatusBadge tone={statusTone(attraction.status)} label={formatStatus(attraction.status)} />
                  <StatusBadge tone="operating" label={formatCapacity(attraction.capacityMode)} />
                </span>
                {attraction.waitMinutes !== null ? (
                  <span className={styles.muted}>Posted wait {formatWaitTime(attraction.waitMinutes)}</span>
                ) : null}
                {attraction.statusMessage ? <span className={styles.muted}>{attraction.statusMessage}</span> : null}
                <span className={styles.muted}>
                  {attraction.relatedOpenIncidentCount} related open{' '}
                  {attraction.relatedOpenIncidentCount === 1 ? 'incident' : 'incidents'}
                </span>
                <span className={styles.muted}>{formatIncidentTime(attraction.updatedAt)}</span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

function WeatherRecommendations({
  items,
  names,
}: {
  items: OperatorDashboard['pendingWeatherRecommendations'];
  names: Map<string, string>;
}) {
  return (
    <section className={styles.panel} aria-labelledby="weather-heading">
      <h2 id="weather-heading">Weather recommendations</h2>
      <p className={styles.disclaimer}>
        Recommendations do not automatically change attractions or incidents. Review them in the
        weather workspace before taking action.
      </p>
      {items.length === 0 ? (
        <p className={styles.empty}>No pending weather recommendations.</p>
      ) : (
        <ul className={styles.cardList}>
          {items.map((item) => (
            <li key={item.id}>
              <Link className={styles.cardLink} to="/attractions#weather-recommendations-heading">
                <span className={styles.badgeRow}>
                  <StatusBadge tone={weatherSeverityTone(item.severity)} label={formatLabel(item.severity)} />
                  <StatusBadge tone="warning" label={formatLabel(item.operatorStatus)} />
                  {item.simulated ? <StatusBadge tone="closed" label="Simulated data" /> : null}
                </span>
                <span className={styles.cardTitle}>{item.recommendedAction}</span>
                <span className={styles.muted}>{item.evidence}</span>
                <span className={styles.muted}>Affected: {labels(item.affectedAttractionIds, names)}</span>
                {item.linkedIncidentId ? (
                  <span className={styles.muted}>Linked incident {item.linkedIncidentId}</span>
                ) : (
                  <span className={styles.muted}>No linked incident</span>
                )}
                <span className={styles.muted}>Data age {formatDataAge(item.observedAt)}</span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

function PublishedAdvisories({
  advisories,
  names,
}: {
  advisories: OperatorDashboard['publishedGuestAdvisories'];
  names: Map<string, string>;
}) {
  return (
    <section className={styles.panel} aria-labelledby="advisories-heading">
      <h2 id="advisories-heading">Published guest advisories</h2>
      {advisories.length === 0 ? (
        <p className={styles.empty}>No guest advisories are currently published.</p>
      ) : (
        <ul className={styles.cardList}>
          {advisories.map((advisory) => (
            <li key={advisory.id}>
              <Link className={styles.cardLink} to={`/incidents/${advisory.incidentId}`}>
                <span className={styles.cardTitle}>{advisory.title}</span>
                <span className={styles.muted}>{advisory.message}</span>
                <StatusBadge tone={severityTone(advisory.severity)} label={formatIncidentSeverity(advisory.severity)} />
                <span className={styles.muted}>Affected: {labels(advisory.affectedAttractionIds, names)}</span>
                <span className={styles.muted}>{formatIncidentTime(advisory.updatedAt)}</span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

function RecentActivity({ events }: { events: OperatorDashboard['recentActivity'] }) {
  return (
    <section className={styles.panel} aria-labelledby="recent-activity-heading">
      <h2 id="recent-activity-heading">Recent operational activity</h2>
      {events.length === 0 ? (
        <p className={styles.empty}>No recent operational activity.</p>
      ) : (
        <ul className={styles.activityList}>
          {events.map((event, index) => (
            <li key={`${event.href}-${event.resultingVersion}-${event.action}-${index}`}>
              <Link className={styles.activityItem} to={event.href}>
                <span>{formatIncidentTime(event.occurredAt)}</span>
                <span>{event.actor}</span>
                <span>{activityDomainLabels[event.domain]}</span>
                <span>{formatLabel(event.action)}</span>
                <span>{event.subject}</span>
                <span className={styles.muted}>{event.reason ?? '—'}</span>
                <span className={styles.muted}>v{event.resultingVersion}</span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

function LoadingState() {
  return (
    <div aria-busy="true">
      <p className={styles.muted} role="status">
        Loading operations dashboard
      </p>
      <div className={styles.skeleton} aria-hidden="true">
        <div className={styles.skeletonRow} />
        <div className={styles.skeletonRow} />
        <div className={styles.skeletonRow} />
      </div>
    </div>
  );
}

function UnavailableState({
  error,
  onRetry,
  retrying,
}: {
  error: Error;
  onRetry: () => void;
  retrying: boolean;
}) {
  const message =
    error instanceof NetworkError
      ? 'The console could not reach VenueOps API. Confirm the service is running on port 8080.'
      : error.message;

  return (
    <div className={`${styles.panel} ${styles.panelError}`} role="alert">
      <h2>Unable to load the operations dashboard</h2>
      <p>{message}</p>
      <p className={styles.muted}>
        Existing workspaces remain available while the dashboard is unreachable.
      </p>
      <div className={styles.dialogActions}>
        <Button onClick={onRetry} disabled={retrying}>
          {retrying ? 'Retrying…' : 'Retry'}
        </Button>
        <Link className={styles.textLink} to="/attractions">
          Open attractions
        </Link>
        <Link className={styles.textLink} to="/incidents">
          Open incidents
        </Link>
      </div>
    </div>
  );
}

function labels(ids: string[], names: Map<string, string>): string {
  if (ids.length === 0) {
    return '';
  }
  return ids.map((id) => names.get(id) ?? id).join(', ');
}

function useImportantChangeAnnouncement(snapshot: OperatorDashboard | undefined): string {
  const previous = useRef<string | null>(null);
  const [announcement, setAnnouncement] = useState('');

  useEffect(() => {
    if (!snapshot) {
      return;
    }
    const next = `${snapshot.summary.majorOrCriticalIncidents} major or critical incidents. ${snapshot.needsAttention.length} items need attention.`;
    if (previous.current && previous.current !== next) {
      setAnnouncement(next);
    }
    previous.current = next;
  }, [snapshot]);

  return announcement;
}
