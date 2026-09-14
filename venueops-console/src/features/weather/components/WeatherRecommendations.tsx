import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';

import { useAuth } from '@/auth/AuthContext';
import { hasVenueOpsScope, venueOpsScopes } from '@/auth/session';
import type { AttractionSummary } from '@/features/attractions/domain/attraction';
import { formatLabel } from '@/features/attractions/domain/formatters';
import { formatDataAge } from '@/features/weather/domain/formatters';
import {
  canHandleRecommendation,
  incidentPrefill,
  severityTone,
  type WeatherRecommendation,
} from '@/features/weather/domain/recommendation';
import { useWeatherRecommendationCommand } from '@/features/weather/hooks/useWeatherRecommendationCommand';
import { useWeatherRecommendations } from '@/features/weather/hooks/useWeatherRecommendations';
import { Button } from '@/shared/ui/Button';
import { HelpHeading } from '@/shared/ui/HelpLink';
import { StatusBadge } from '@/shared/ui/StatusBadge';

import styles from './WeatherRecommendations.module.css';

export function WeatherRecommendations({
  attractions,
}: {
  attractions: AttractionSummary[];
}) {
  const { session } = useAuth();
  const { data, isPending, isError, error, refetch, isFetching } =
    useWeatherRecommendations();
  const recommendations = data ?? [];
  const canReview = hasVenueOpsScope(
    session,
    venueOpsScopes.weatherRecommendationsReview,
  );
  const canCreateIncident =
    canReview && hasVenueOpsScope(session, venueOpsScopes.incidentsCommand);

  return (
    <section
      className={styles.section}
      aria-labelledby="weather-recommendations-heading"
    >
      <div className={styles.header}>
        <HelpHeading
          id="weather-recommendations-heading"
          article="weather"
          section="review"
          label="Help: weather recommendations"
        >
          Weather recommendations
        </HelpHeading>
        <p className={styles.count}>
          {recommendations.filter((item) => item.status === 'ACTIVE').length}{' '}
          active
        </p>
      </div>
      {isPending && recommendations.length === 0 ? (
        <p className={styles.muted} role="status">
          Loading weather recommendations
        </p>
      ) : null}
      {isError ? (
        <div role="alert">
          <p className={styles.error}>{error.message}</p>
          <Button onClick={() => void refetch()} disabled={isFetching}>
            {isFetching ? 'Retrying…' : 'Retry'}
          </Button>
        </div>
      ) : null}
      {!isPending && !isError && recommendations.length === 0 ? (
        <p className={styles.empty}>No weather recommendations in the inbox.</p>
      ) : null}
      {recommendations.length > 0 ? (
        <ul className={styles.list}>
          {recommendations.map((recommendation) => (
            <li key={recommendation.id}>
              <RecommendationCard
                recommendation={recommendation}
                attractions={attractions}
                canReview={canReview}
                canCreateIncident={canCreateIncident}
              />
            </li>
          ))}
        </ul>
      ) : null}
    </section>
  );
}

function RecommendationCard({
  recommendation,
  attractions,
  canReview,
  canCreateIncident,
}: {
  recommendation: WeatherRecommendation;
  attractions: AttractionSummary[];
  canReview: boolean;
  canCreateIncident: boolean;
}) {
  const { command, createIncident } = useWeatherRecommendationCommand();
  const [creating, setCreating] = useState(false);
  const names = attractionNames(
    recommendation.affectedAttractionIds,
    attractions,
  );
  const handleable = canHandleRecommendation(recommendation);
  const pending = command.isPending || createIncident.isPending;
  const error = command.error ?? createIncident.error;

  return (
    <article
      className={`${styles.card} ${recommendation.status === 'CLEARED' ? styles.cleared : ''}`}
    >
      <div className={styles.identity}>
        <StatusBadge
          tone={severityTone(recommendation.severity)}
          label={formatLabel(recommendation.severity)}
        />
        <div className={styles.meta}>
          <span className={styles.chip}>
            {recommendation.simulated ? 'Simulated' : 'Observed'}
          </span>
          <span className={styles.chip}>
            {formatLabel(recommendation.status)}
          </span>
          <span className={styles.chip}>
            {formatLabel(recommendation.operatorStatus)}
          </span>
        </div>
      </div>
      <p className={styles.action}>
        <strong>Recommended action.</strong> {recommendation.recommendedAction}
      </p>
      <p className={styles.evidence}>
        <strong>Evidence.</strong> {recommendation.evidence}
      </p>
      <p className={styles.age}>
        <strong>Data age.</strong> {formatDataAge(recommendation.observedAt)}
      </p>
      <p className={styles.attractions}>
        <strong>Affected attractions.</strong>{' '}
        {names.join(', ') || 'None listed'}
      </p>
      {recommendation.linkedIncidentId ? (
        <p className={styles.muted}>
          <Link to={`/incidents/${recommendation.linkedIncidentId}`}>
            View linked incident
          </Link>
        </p>
      ) : null}
      {error ? (
        <p className={styles.error} role="alert">
          {error.message}
        </p>
      ) : null}
      {handleable && canReview ? (
        <div className={styles.actions}>
          {recommendation.operatorStatus === 'PENDING' ? (
            <Button
              disabled={pending}
              onClick={() =>
                command.mutate({
                  recommendationId: recommendation.id,
                  type: 'ACKNOWLEDGE',
                  expectedVersion: recommendation.version,
                })
              }
            >
              Acknowledge
            </Button>
          ) : null}
          <Button
            variant="ghost"
            disabled={pending}
            onClick={() =>
              command.mutate({
                recommendationId: recommendation.id,
                type: 'DISMISS',
                expectedVersion: recommendation.version,
                reason: 'Dismissed from Control Tower',
              })
            }
          >
            Dismiss
          </Button>
          {canCreateIncident && recommendation.operatorStatus !== 'LINKED' ? (
            <Button
              variant="hold"
              disabled={pending}
              onClick={() => setCreating(true)}
            >
              Create incident
            </Button>
          ) : null}
        </div>
      ) : null}
      {creating && handleable && canCreateIncident ? (
        <CreateIncidentForm
          recommendation={recommendation}
          attractionNames={names}
          pending={createIncident.isPending}
          onCancel={() => setCreating(false)}
          onSubmit={(incident) => {
            createIncident.mutate(
              {
                recommendationId: recommendation.id,
                expectedVersion: recommendation.version,
                incident,
              },
              {
                onSuccess: () => setCreating(false),
              },
            );
          }}
        />
      ) : null}
    </article>
  );
}

function CreateIncidentForm({
  recommendation,
  attractionNames,
  pending,
  onCancel,
  onSubmit,
}: {
  recommendation: WeatherRecommendation;
  attractionNames: string[];
  pending: boolean;
  onCancel: () => void;
  onSubmit: (incident: {
    title: string;
    severity: 'MINOR' | 'MODERATE' | 'MAJOR' | 'CRITICAL';
    internalDescription: string;
    attractionIds: string[];
  }) => void;
}) {
  const prefill = incidentPrefill(recommendation);
  const [title, setTitle] = useState(prefill.title);
  const [severity, setSeverity] = useState<
    'MINOR' | 'MODERATE' | 'MAJOR' | 'CRITICAL'
  >(prefill.severity);
  const [internalDescription, setInternalDescription] = useState(
    prefill.internalDescription,
  );
  const [selectedIds, setSelectedIds] = useState(prefill.attractionIds);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onSubmit({
      title: title.trim(),
      severity,
      internalDescription: internalDescription.trim(),
      attractionIds: selectedIds,
    });
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <h3>Review weather incident</h3>
      <p className={styles.muted}>
        Type is weather. Review the prefilled fields, then submit. This does not
        place attractions on hold.
      </p>
      <label className={styles.label}>
        Title
        <input
          className={styles.input}
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          required
        />
      </label>
      <label className={styles.label}>
        Severity
        <select
          className={styles.select}
          value={severity}
          onChange={(event) =>
            setSeverity(
              event.target.value as 'MINOR' | 'MODERATE' | 'MAJOR' | 'CRITICAL',
            )
          }
        >
          <option value="MINOR">Minor</option>
          <option value="MODERATE">Moderate</option>
          <option value="MAJOR">Major</option>
          <option value="CRITICAL">Critical</option>
        </select>
      </label>
      <label className={styles.label}>
        Internal description
        <textarea
          className={styles.textarea}
          rows={5}
          value={internalDescription}
          onChange={(event) => setInternalDescription(event.target.value)}
          required
        />
      </label>
      <fieldset className={styles.checks}>
        <legend className={styles.label}>Affected attractions</legend>
        {recommendation.affectedAttractionIds.map((id, index) => (
          <label key={id} className={styles.check}>
            <input
              type="checkbox"
              checked={selectedIds.includes(id)}
              onChange={(event) => {
                setSelectedIds((current) =>
                  event.target.checked
                    ? [...current, id]
                    : current.filter((item) => item !== id),
                );
              }}
            />
            {attractionNames[index] ?? id}
          </label>
        ))}
      </fieldset>
      <div className={styles.actions}>
        <Button type="submit" disabled={pending || !title.trim()}>
          {pending ? 'Creating…' : 'Submit incident'}
        </Button>
        <Button
          type="button"
          variant="ghost"
          onClick={onCancel}
          disabled={pending}
        >
          Cancel
        </Button>
      </div>
    </form>
  );
}

function attractionNames(
  ids: string[],
  attractions: AttractionSummary[],
): string[] {
  return ids.map(
    (id) => attractions.find((attraction) => attraction.id === id)?.name ?? id,
  );
}
