import { useId, useState, type FormEvent } from 'react';

import type { AttractionSummary } from '@/features/attractions/domain/attraction';
import type { AuthSession } from '@/auth/session';
import {
  availableIncidentActions,
  lifecycleHint,
  nextSeverityOptions,
  resolveBlockedReason,
  type IncidentAction,
} from '@/features/incidents/domain/commands';
import type { Incident, IncidentSeverity } from '@/features/incidents/domain/incident';
import { formatIncidentSeverity } from '@/features/incidents/domain/formatters';
import {
  ForbiddenError,
  InvalidTransitionError,
  VersionConflictError,
} from '@/shared/api/errors';
import { Button } from '@/shared/ui/Button';
import { HelpHeading } from '@/shared/ui/HelpLink';

import { GuestAdvisoryComposer } from './GuestAdvisoryComposer';
import styles from './IncidentCommandPanel.module.css';

type IncidentCommandPanelProps = {
  session: AuthSession | null;
  incident: Incident;
  attractions: AttractionSummary[];
  pending: boolean;
  error: Error | null;
  onCommand: (input: {
    type: IncidentAction['type'];
    reason?: string;
    assignee?: string;
    severity?: IncidentSeverity;
    attractionId?: string;
    guestTitle?: string;
    guestMessage?: string;
  }) => void;
};

export function IncidentCommandPanel(props: IncidentCommandPanelProps) {
  return (
    <IncidentCommandPanelContent
      key={`${props.incident.id}:${props.incident.version}`}
      {...props}
    />
  );
}

function IncidentCommandPanelContent({
  session,
  incident,
  attractions,
  pending,
  error,
  onCommand,
}: IncidentCommandPanelProps) {
  const actions = availableIncidentActions(session, incident);
  const hint = lifecycleHint(incident);
  const resolveHint = resolveBlockedReason(session, incident);
  const formId = useId();
  const [reason, setReason] = useState('');
  const [assignee, setAssignee] = useState(incident.assignedTo ?? '');
  const [severity, setSeverity] = useState<IncidentSeverity>(
    nextSeverityOptions(incident.severity)[0] ?? 'MINOR',
  );
  const [attractionId, setAttractionId] = useState('');
  const [active, setActive] = useState<IncidentAction['type'] | null>(null);

  const unlinked = attractions.filter((attraction) => !incident.attractionIds.includes(attraction.id));
  const linked = attractions.filter((attraction) => incident.attractionIds.includes(attraction.id));

  function submit(event: FormEvent) {
    event.preventDefault();
    if (!active) {
      return;
    }
    onCommand({
      type: active,
      reason: reason.trim() || undefined,
      assignee: assignee.trim() || undefined,
      severity,
      attractionId: attractionId || undefined,
    });
  }

  return (
    <section className={styles.panel} aria-labelledby="incident-commands-heading">
      <HelpHeading
        id="incident-commands-heading"
        article="incidents"
        section="lifecycle"
        label="Help: incident lifecycle"
      >
        Commands
      </HelpHeading>
      {hint ? <p>{hint}</p> : null}
      {resolveHint ? <p>{resolveHint}</p> : null}
      {incident.status === 'RESOLVED' ? <p>Resolved incidents are read-only.</p> : null}
      {error instanceof ForbiddenError ? (
        <p role="alert">Access denied. The loaded incident is unchanged.</p>
      ) : null}
      {error instanceof InvalidTransitionError ? (
        <p role="alert">That transition is no longer valid. Reload to see the current state.</p>
      ) : null}
      {error && !(error instanceof VersionConflictError) && !(error instanceof ForbiddenError) ? (
        <p role="alert">{error.message}</p>
      ) : null}
      <div className={styles.actions}>
        {actions
          .filter((action) => action.type !== 'PUBLISH_GUEST_ADVISORY' && action.type !== 'WITHDRAW_GUEST_ADVISORY')
          .map((action) => (
            <Button
              key={action.type}
              variant={action.type === 'RESOLVE' ? 'danger' : 'default'}
              disabled={pending}
              aria-pressed={active === action.type}
              onClick={() => setActive(action.type)}
            >
              {action.label}
            </Button>
          ))}
      </div>
      {active ? (
        <form id={formId} className={styles.form} onSubmit={submit}>
          {active === 'ASSIGN' ? (
            <label>
              Assignee
              <input
                required
                value={assignee}
                onChange={(event) => setAssignee(event.target.value)}
              />
            </label>
          ) : null}
          {active === 'CHANGE_SEVERITY' ? (
            <>
              <p>
                Change severity from {formatIncidentSeverity(incident.severity)} to{' '}
                {formatIncidentSeverity(severity)}.
              </p>
              <label>
                New severity
                <select
                  value={severity}
                  onChange={(event) => setSeverity(event.target.value as IncidentSeverity)}
                >
                  {nextSeverityOptions(incident.severity).map((option) => (
                    <option key={option} value={option}>
                      {formatIncidentSeverity(option)}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Reason
                <input required value={reason} onChange={(event) => setReason(event.target.value)} />
              </label>
            </>
          ) : null}
          {active === 'LINK_ATTRACTION' ? (
            <>
              <p>Linking does not change attraction operational state.</p>
              <label>
                Attraction
                <select
                  required
                  value={attractionId}
                  onChange={(event) => setAttractionId(event.target.value)}
                >
                  <option value="">Select attraction</option>
                  {unlinked.map((attraction) => (
                    <option key={attraction.id} value={attraction.id}>
                      {attraction.name}
                    </option>
                  ))}
                </select>
              </label>
            </>
          ) : null}
          {active === 'UNLINK_ATTRACTION' ? (
            <>
              <label>
                Attraction
                <select
                  required
                  value={attractionId}
                  onChange={(event) => setAttractionId(event.target.value)}
                >
                  <option value="">Select attraction</option>
                  {linked.map((attraction) => (
                    <option key={attraction.id} value={attraction.id}>
                      {attraction.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Reason
                <input required value={reason} onChange={(event) => setReason(event.target.value)} />
              </label>
            </>
          ) : null}
          {active === 'RESOLVE' ? (
            <>
              <p>
                Resolve will close this incident
                {incident.attractionIds.length > 0
                  ? ` linked to ${incident.attractionIds.length} attraction(s)`
                  : ''}
                {incident.guestAdvisoryPublished ? ' and remove the published guest advisory.' : '.'}
              </p>
              <label>
                Reason
                <input required value={reason} onChange={(event) => setReason(event.target.value)} />
              </label>
            </>
          ) : null}
          {active === 'ACKNOWLEDGE' || active === 'START_MITIGATION' ? (
            <label>
              Reason
              <input value={reason} onChange={(event) => setReason(event.target.value)} />
            </label>
          ) : null}
          <Button type="submit" disabled={pending}>
            {pending ? 'Sending…' : 'Confirm command'}
          </Button>
        </form>
      ) : null}
      {actions.some((action) => action.type === 'PUBLISH_GUEST_ADVISORY' || action.type === 'WITHDRAW_GUEST_ADVISORY') ? (
        <GuestAdvisoryComposer
          incident={incident}
          pending={pending}
          onPublish={(input) =>
            onCommand({
              type: 'PUBLISH_GUEST_ADVISORY',
              guestTitle: input.guestTitle,
              guestMessage: input.guestMessage,
            })
          }
          onWithdraw={(withdrawReason) =>
            onCommand({ type: 'WITHDRAW_GUEST_ADVISORY', reason: withdrawReason })
          }
        />
      ) : null}
    </section>
  );
}
