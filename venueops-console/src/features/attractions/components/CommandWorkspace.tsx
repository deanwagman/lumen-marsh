import { useState, type FormEvent, type ReactNode } from 'react';

import type { AttractionActivity } from '@/features/attractions/domain/activity';
import type { OperatorAttraction } from '@/features/attractions/domain/attraction';
import {
  canUpdateWaitTime,
  commandDefinitions,
  MAX_WAIT_MINUTES,
  statusCommands,
  type AttractionCommandType,
} from '@/features/attractions/domain/commands';
import {
  formatActivityTitle,
  formatActivityType,
  formatUpdatedAt,
  formatWaitTime,
} from '@/features/attractions/domain/formatters';
import { InvalidTransitionError, VersionConflictError } from '@/shared/api/errors';
import { Button } from '@/shared/ui/Button';
import { HelpHeading } from '@/shared/ui/HelpLink';

import styles from './CommandWorkspace.module.css';

export function VersionConflictBanner({
  error,
  onReload,
  reloading,
}: {
  error: VersionConflictError;
  onReload: () => void;
  reloading: boolean;
}) {
  const expected = error.expectedVersion !== undefined ? `v${error.expectedVersion}` : 'the last read';
  const actual = error.actualVersion !== undefined ? `v${error.actualVersion}` : 'a newer version';

  return (
    <div className={styles.banner} role="alert">
      <strong>Version conflict</strong>
      <p>
        This attraction changed since it was last read ({expected} → {actual}). Reload current
        state, then send the command again.
      </p>
      <Button onClick={onReload} disabled={reloading}>
        {reloading ? 'Reloading…' : 'Reload current state'}
      </Button>
    </div>
  );
}

export function CommandPanel({
  attraction,
  pending,
  error,
  onSubmit,
}: {
  attraction: OperatorAttraction;
  pending: boolean;
  error: Error | null;
  onSubmit: (input: { type: AttractionCommandType; reason?: string }) => void;
}) {
  const [selected, setSelected] = useState<AttractionCommandType | null>(null);
  const [reason, setReason] = useState('');
  const commands = statusCommands(attraction.status, attraction.capacityMode);
  const activeSelection = selected && commands.includes(selected) ? selected : null;
  const definition = activeSelection ? commandDefinitions[activeSelection] : null;

  function handleConfirm(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!activeSelection || !definition) {
      return;
    }

    const trimmed = reason.trim();
    if (definition.requiresReason && !trimmed) {
      return;
    }

    onSubmit({ type: activeSelection, reason: trimmed || undefined });
  }

  return (
    <div className={styles.card}>
      <HelpHeading article="attractions" section="commands" label="Help: attraction commands">
        Commands
      </HelpHeading>
      <p className={styles.description}>
        Only transitions valid for the current state are offered. Clients cannot pick a destination
        status directly.
      </p>
      <div className={styles.commands}>
        {commands.map((type) => {
          const command = commandDefinitions[type];
          return (
            <Button
              key={type}
              variant={command.tone}
              aria-pressed={activeSelection === type}
              disabled={pending}
              onClick={() => {
                setSelected(type);
                setReason('');
              }}
            >
              {command.label}
            </Button>
          );
        })}
      </div>
      {definition ? (
        <form className={styles.confirm} onSubmit={handleConfirm}>
          <h3>{definition.label}</h3>
          <p className={styles.description}>{definition.description}</p>
          <label className={styles.label} htmlFor="command-reason">
            {definition.requiresReason ? 'Reason (required)' : 'Reason (optional)'}
            <textarea
              id="command-reason"
              className={styles.textarea}
              rows={3}
              value={reason}
              required={definition.requiresReason}
              onChange={(event) => setReason(event.target.value)}
            />
          </label>
          {error && !(error instanceof VersionConflictError) ? (
            <p role="alert">{commandErrorMessage(error)}</p>
          ) : null}
          <div className={styles.actions}>
            <Button type="submit" variant={definition.tone} disabled={pending}>
              {pending ? 'Sending…' : 'Confirm command'}
            </Button>
            <Button
              type="button"
              variant="ghost"
              onClick={() => {
                setSelected(null);
                setReason('');
              }}
            >
              Cancel
            </Button>
          </div>
        </form>
      ) : null}
    </div>
  );
}

export function WaitTimeForm({
  attraction,
  pending,
  onSubmit,
}: {
  attraction: OperatorAttraction;
  pending: boolean;
  onSubmit: (waitMinutes: number) => void;
}) {
  const [waitMinutes, setWaitMinutes] = useState(String(attraction.waitMinutes ?? 0));

  if (!canUpdateWaitTime(attraction.status)) {
    return null;
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const parsed = Number.parseInt(waitMinutes, 10);
    if (Number.isNaN(parsed) || parsed < 0 || parsed > MAX_WAIT_MINUTES) {
      return;
    }
    onSubmit(parsed);
  }

  return (
    <form className={`${styles.card} ${styles.wait}`} onSubmit={handleSubmit}>
      <div>
        <HelpHeading article="attractions" section="wait-times" label="Help: wait times">
          Wait time
        </HelpHeading>
        <p className={styles.description}>
          Posted wait for operating attractions. Current: {formatWaitTime(attraction.waitMinutes)}.
        </p>
      </div>
      <label className={styles.label} htmlFor="wait-minutes">
        Minutes
        <input
          id="wait-minutes"
          className={styles.input}
          type="number"
          min={0}
          max={MAX_WAIT_MINUTES}
          value={waitMinutes}
          onChange={(event) => setWaitMinutes(event.target.value)}
        />
      </label>
      <Button type="submit" disabled={pending}>
        {pending ? 'Updating…' : 'Update wait time'}
      </Button>
    </form>
  );
}

export function ActivityTimeline({
  activity,
  isPending,
  isError,
  error,
  onRetry,
  retrying,
}: {
  activity: AttractionActivity[] | undefined;
  isPending: boolean;
  isError: boolean;
  error: Error | null;
  onRetry: () => void;
  retrying: boolean;
}) {
  const items = [...(activity ?? [])].reverse();

  return (
    <div className={styles.card} id="activity">
      <h2>Activity</h2>
      {isPending ? (
        <p className={styles.description} role="status">
          Loading activity
        </p>
      ) : null}
      {isError ? (
        <div role="alert">
          <p>{error?.message ?? 'Unable to load activity.'}</p>
          <Button onClick={onRetry} disabled={retrying}>
            Retry
          </Button>
        </div>
      ) : null}
      {activity && activity.length === 0 ? (
        <p className={styles.description}>No operator activity yet.</p>
      ) : null}
      {items.length > 0 ? (
        <ol className={styles.timeline}>
          {items.map((item) => (
            <li key={item.id} id={`activity-${item.id}`} className={styles.item}>
              <div className={styles.itemHeader}>
                <strong>{formatActivityTitle(item)}</strong>
                <span className={styles.actor}>{formatUpdatedAt(item.occurredAt)}</span>
              </div>
              <p className={styles.actor}>
                {formatActivityType(item)} · {item.actor} · v{item.previousVersion} → v
                {item.resultingVersion}
              </p>
              {item.reason ? <p className={styles.reason}>{item.reason}</p> : null}
            </li>
          ))}
        </ol>
      ) : null}
    </div>
  );
}

export function CommandWorkspaceLayout({ children }: { children: ReactNode }) {
  return <div className={styles.workspace}>{children}</div>;
}

function commandErrorMessage(error: Error): string {
  if (error instanceof InvalidTransitionError) {
    return error.message;
  }
  return error.message;
}
