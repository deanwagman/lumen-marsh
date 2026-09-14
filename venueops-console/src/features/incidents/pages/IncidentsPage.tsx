import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';

import { useAuth } from '@/auth/AuthContext';
import { useAttractions } from '@/features/attractions/hooks/useAttractions';
import { IncidentFilters } from '@/features/incidents/components/IncidentFilters';
import { IncidentList } from '@/features/incidents/components/IncidentList';
import {
  canCommandIncidents,
  compareIncidents,
  filterIncidents,
  incidentSeverities,
  incidentTypes,
  summarizeIncidents,
  type IncidentSeverity,
  type IncidentType,
} from '@/features/incidents/domain/incident';
import { formatIncidentTime } from '@/features/incidents/domain/formatters';
import { useIncidentParams } from '@/features/incidents/hooks/useIncidentParams';
import { useIncidents } from '@/features/incidents/hooks/useIncidents';
import { useReportIncident } from '@/features/incidents/hooks/useIncidentCommand';
import { Button } from '@/shared/ui/Button';

import styles from './IncidentsPage.module.css';

export default function IncidentsPage() {
  const { session } = useAuth();
  const { filter, setFilter } = useIncidentParams();
  const incidents = useIncidents();
  const attractions = useAttractions();
  const report = useReportIncident();
  const navigate = useNavigate();
  const [openReport, setOpenReport] = useState(false);
  const [title, setTitle] = useState('');
  const [type, setType] = useState<IncidentType>('WEATHER');
  const [severity, setSeverity] = useState<IncidentSeverity>('MODERATE');
  const [description, setDescription] = useState('');
  const [linked, setLinked] = useState<string[]>([]);

  const catalog = attractions.data ?? [];
  const records = [...(incidents.data ?? [])].sort(compareIncidents);
  const visible = filterIncidents(records, filter);
  const summary = summarizeIncidents(records);
  const canReport = canCommandIncidents(session);
  const lastSynchronized =
    incidents.dataUpdatedAt > 0
      ? formatIncidentTime(new Date(incidents.dataUpdatedAt).toISOString())
      : null;

  function submitReport(event: FormEvent) {
    event.preventDefault();
    report.mutate(
      {
        title: title.trim(),
        type,
        severity,
        internalDescription: description.trim() || undefined,
        attractionIds: linked,
      },
      {
        onSuccess: (incident) => {
          navigate(`/incidents/${incident.id}`);
        },
      },
    );
  }

  return (
    <section className={styles.page}>
      <header className={styles.header}>
        <div>
          <p className={styles.kicker}>Live shift</p>
          <h1>Incident Center</h1>
          <p className={styles.lede}>
            Current operational incidents and recent resolutions.
          </p>
        </div>
        {canReport ? (
          <Button onClick={() => setOpenReport((value) => !value)}>Report incident</Button>
        ) : null}
      </header>
      <dl className={styles.summary}>
        <div>
          <dt>Open</dt>
          <dd>{summary.open}</dd>
        </div>
        <div>
          <dt>High severity</dt>
          <dd>{summary.criticalOrMajor}</dd>
        </div>
        <div>
          <dt>Unassigned</dt>
          <dd>{summary.unassigned}</dd>
        </div>
        <div>
          <dt>Guest advisories</dt>
          <dd>{summary.publishedAdvisories}</dd>
        </div>
        <div>
          <dt>Last synchronized</dt>
          <dd>{lastSynchronized ?? 'Not yet'}</dd>
        </div>
      </dl>
      {openReport && canReport ? (
        <form className={styles.report} onSubmit={submitReport}>
          <h2>Report incident</h2>
          <label>
            Title
            <input required value={title} onChange={(event) => setTitle(event.target.value)} />
          </label>
          <label>
            Type
            <select value={type} onChange={(event) => setType(event.target.value as IncidentType)}>
              {incidentTypes.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
          </label>
          <label>
            Severity
            <select
              value={severity}
              onChange={(event) => setSeverity(event.target.value as IncidentSeverity)}
            >
              {incidentSeverities.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
          </label>
          <label>
            Internal description
            <textarea value={description} onChange={(event) => setDescription(event.target.value)} />
          </label>
          <fieldset>
            <legend>Linked attractions</legend>
            {catalog.map((attraction) => (
              <label key={attraction.id}>
                <input
                  type="checkbox"
                  checked={linked.includes(attraction.id)}
                  onChange={(event) => {
                    setLinked((current) =>
                      event.target.checked
                        ? [...current, attraction.id]
                        : current.filter((id) => id !== attraction.id),
                    );
                  }}
                />
                {attraction.name}
              </label>
            ))}
          </fieldset>
          {report.isError ? <p role="alert">{report.error.message}</p> : null}
          <Button type="submit" disabled={report.isPending}>
            {report.isPending ? 'Reporting…' : 'Create incident'}
          </Button>
        </form>
      ) : null}
      <IncidentFilters filter={filter} attractions={catalog} onChange={setFilter} />
      {incidents.isPending && !incidents.data ? (
        <div className={styles.skeleton} role="status" aria-label="Loading incidents">
          <div className={styles.skeletonRow} />
          <div className={styles.skeletonRow} />
          <div className={styles.skeletonRow} />
        </div>
      ) : null}
      {incidents.isError ? (
        <div className={styles.error} role="alert">
          <h2>Unable to load incidents</h2>
          <p>The console could not reach VenueOps API.</p>
          <Button onClick={() => void incidents.refetch()} disabled={incidents.isFetching}>
            Retry
          </Button>
        </div>
      ) : null}
      {!incidents.isPending && !incidents.isError && records.length === 0 ? (
        <div className={styles.empty}>
          <h2>No incidents reported</h2>
          <p>Operational incidents will appear here when they are reported by Control Tower.</p>
        </div>
      ) : null}
      {!incidents.isPending && !incidents.isError && records.length > 0 ? (
        <IncidentList incidents={visible} attractions={catalog} />
      ) : null}
    </section>
  );
}
