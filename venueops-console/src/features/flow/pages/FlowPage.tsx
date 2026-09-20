import { useState } from 'react';
import { Link } from 'react-router-dom';

import { useAuth } from '@/auth/AuthContext';
import { formatLabel, formatUpdatedAt } from '@/features/attractions/domain/formatters';
import { AttractionFlowCard } from '@/features/flow/components/AttractionFlowCard';
import { useFlowCommand } from '@/features/flow/hooks/useFlowCommand';
import { useFlowOverview } from '@/features/flow/hooks/useFlowOverview';
import { useFlowRecommendations } from '@/features/flow/hooks/useFlowRecommendations';
import {
  canPublishFlow,
  canReadFlow,
  nextFlowCommands,
  type FlowRecommendation,
  type FlowRecommendationCommand,
} from '@/features/flow/domain/flow';
import { useAttractionStreamHealth } from '@/features/attractions/hooks/useAttractionStream';
import { streamStatusLabel } from '@/features/attractions/domain/streamHealth';
import { MaintenanceCommandDialog } from '@/features/maintenance/components/MaintenanceCommandDialog';
import { DuplicateCommandError, ForbiddenError, NetworkError, VersionConflictError } from '@/shared/api/errors';
import { Button } from '@/shared/ui/Button';
import { HelpHeading } from '@/shared/ui/HelpLink';

import styles from './FlowPage.module.css';

export default function FlowPage() {
  const { session } = useAuth();
  const overview = useFlowOverview();
  const allInbox = useFlowRecommendations({ page: 0, size: 20 });
  const command = useFlowCommand();
  const stream = useAttractionStreamHealth();
  const [draft, setDraft] = useState<{
    recommendation: FlowRecommendation;
    type: FlowRecommendationCommand;
    reason: string;
    guestMessage: string;
  } | null>(null);

  if (!canReadFlow(session)) {
    return (
      <section className={styles.page}>
        <h1>Park Flow</h1>
        <p>This workspace requires the venueops/flow.read scope.</p>
      </section>
    );
  }

  const data = overview.data;
  const listed = allInbox.data?.items ?? [];
  const lastSynchronized =
    overview.dataUpdatedAt > 0 ? formatUpdatedAt(new Date(overview.dataUpdatedAt).toISOString()) : null;
  const currentDraft = draft
    ? listed.find((item) => item.recommendationId === draft.recommendation.recommendationId)
    : undefined;
  const staleDialog =
    draft != null && currentDraft != null && currentDraft.version !== draft.recommendation.version;

  function errorMessage(error: unknown): string | null {
    if (!error) return null;
    if (error instanceof VersionConflictError) {
      return 'This recommendation changed since it was last read.';
    }
    if (error instanceof ForbiddenError) {
      return error.message;
    }
    if (error instanceof NetworkError) {
      return error.message;
    }
    if (error instanceof DuplicateCommandError) {
      return error.message;
    }
    if (error instanceof Error) {
      return error.message;
    }
    return 'The command could not be sent.';
  }

  return (
    <section className={styles.page}>
      <header className={styles.header}>
        <div>
          <p className={styles.kicker}>Control Tower</p>
          <h1>Park Flow</h1>
          <p className={styles.lede}>
            Queue telemetry, deterministic wait forecasts, and operator-reviewed guest guidance.
            Flow recommendations do not change attraction state or capacity automatically.
          </p>
        </div>
        <div className={styles.meta}>
          <p>{streamStatusLabel(stream.data?.status ?? 'connecting')}</p>
          <p className={styles.muted}>Last synchronized {lastSynchronized ?? 'waiting'}</p>
          <Button onClick={() => void Promise.all([overview.refetch(), allInbox.refetch()])}>
            Refresh
          </Button>
        </div>
      </header>

      {overview.isPending ? (
        <p className={styles.muted} role="status">
          Loading park flow overview
        </p>
      ) : null}
      {overview.isError ? (
        <div className={styles.error} role="alert">
          <p>Unable to load the park flow workspace.</p>
          <Button onClick={() => void overview.refetch()}>Retry</Button>
        </div>
      ) : null}

      {data ? (
        <ul className={styles.metrics}>
          <li>
            <span>Guests in queues</span>
            <strong>{data.guestsInQueues}</strong>
          </li>
          <li>
            <span>Rising waits</span>
            <strong>{data.risingWaitCount}</strong>
          </li>
          <li>
            <span>Stale telemetry</span>
            <strong>{data.staleAttractionCount}</strong>
          </li>
          <li>
            <span>Pending recommendations</span>
            <strong>{data.pendingRecommendationCount}</strong>
          </li>
          <li>
            <span>Park capacity</span>
            <strong>{data.parkCapacityPercent}%</strong>
          </li>
        </ul>
      ) : null}

      {data?.simulated ? <p className={styles.badge}>Simulated demonstration data</p> : null}

      <HelpHeading article="park-flow" section="forecasts" label="Forecast presentation">
        Attraction queues
      </HelpHeading>
      <div className={styles.cards}>
        {(data?.attractions ?? []).map((card) => (
          <AttractionFlowCard key={card.attractionId} card={card} />
        ))}
      </div>

      <HelpHeading article="park-flow" section="recommendations" label="Recommendation review">
        Recommendation inbox
      </HelpHeading>
      <ul className={styles.inbox}>
        {listed.map((recommendation) => {
          const commands = nextFlowCommands(recommendation.status, session);
          return (
            <li key={recommendation.recommendationId}>
              <article>
                <h3>{recommendation.summary}</h3>
                <p>{recommendation.explanation}</p>
                <p className={styles.muted}>
                  {formatLabel(recommendation.type)} · {formatLabel(recommendation.status)} ·{' '}
                  {formatLabel(recommendation.severity)}
                  {recommendation.simulated ? ' · Simulated' : ''}
                </p>
                {recommendation.relatedIncidentId ? (
                  <p>
                    <Link to={`/incidents/${recommendation.relatedIncidentId}`}>Related incident</Link>
                  </p>
                ) : null}
                {recommendation.relatedWorkOrderId ? (
                  <p>
                    <Link to={`/maintenance/work-orders/${recommendation.relatedWorkOrderId}`}>
                      Related work order
                    </Link>
                  </p>
                ) : null}
                <div className={styles.actions}>
                {commands.map((type) => (
                  <Button
                    key={type}
                    onClick={() => {
                      command.beginNewIntent();
                      command.reset();
                      setDraft({
                        recommendation,
                        type,
                        reason: '',
                        guestMessage: recommendation.guestMessage ?? '',
                      });
                    }}
                  >
                    {formatLabel(type)}
                  </Button>
                ))}
                </div>
                {recommendation.status === 'APPROVED' && !canPublishFlow(session) ? (
                  <p className={styles.muted}>
                    Publishing guest guidance requires a supervisor with venueops/flow.publish.
                  </p>
                ) : null}
              </article>
            </li>
          );
        })}
      </ul>
      {listed.length === 0 && !allInbox.isPending ? (
        <p className={styles.muted}>No flow recommendations are waiting.</p>
      ) : null}

      {draft ? (
        <MaintenanceCommandDialog
          title={`${formatLabel(draft.type)} recommendation`}
          description="Confirm this human decision. The intelligence service cannot change attraction state."
          currentState={formatLabel(draft.recommendation.status)}
          attractionName={draft.recommendation.sourceAttractionId ?? undefined}
          stale={Boolean(staleDialog)}
          pending={command.isPending}
          error={errorMessage(command.error)}
          submitLabel={formatLabel(draft.type)}
          onClose={() => {
            if (!command.isPending) {
              setDraft(null);
            }
          }}
          onSubmit={() => {
            if (staleDialog) {
              return;
            }
            command.mutate(
              {
                recommendationId: draft.recommendation.recommendationId,
                type: draft.type,
                expectedVersion: draft.recommendation.version,
                reason: draft.reason.trim() || undefined,
                guestMessage:
                  draft.type === 'PUBLISH' ? draft.guestMessage.trim() || undefined : undefined,
              },
              {
                onSuccess: () => setDraft(null),
              },
            );
          }}
        >
          {draft.type === 'DISMISS' || draft.type === 'WITHDRAW' ? (
            <label>
              Reason
              <textarea
                required
                value={draft.reason}
                onChange={(event) => setDraft({ ...draft, reason: event.target.value })}
              />
            </label>
          ) : null}
          {draft.type === 'PUBLISH' ? (
            <label>
              Guest message
              <textarea
                required
                value={draft.guestMessage}
                onChange={(event) => setDraft({ ...draft, guestMessage: event.target.value })}
              />
            </label>
          ) : null}
        </MaintenanceCommandDialog>
      ) : null}
    </section>
  );
}
