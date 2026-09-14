import { Link, useLocation, useParams } from 'react-router-dom';
import { useEffect } from 'react';

import { useAuth } from '@/auth/AuthContext';
import {
  ActivityTimeline,
  CommandPanel,
  CommandWorkspaceLayout,
  VersionConflictBanner,
  WaitTimeForm,
} from '@/features/attractions/components/CommandWorkspace';
import type { AttractionActivity } from '@/features/attractions/domain/activity';
import type { OperatorAttraction } from '@/features/attractions/domain/attraction';
import type { AttractionCommandType } from '@/features/attractions/domain/commands';
import { useAttractionActivity } from '@/features/attractions/hooks/useAttractionActivity';
import { useAttractionCommand } from '@/features/attractions/hooks/useAttractionCommand';
import { useAttractionDetail } from '@/features/attractions/hooks/useAttractionDetail';
import { statusTone } from '@/features/attractions/domain/attraction';
import {
  formatCapacity,
  formatStatus,
  formatType,
  formatUpdatedAt,
  formatVersion,
  formatWaitTime,
} from '@/features/attractions/domain/formatters';
import { NotFoundError, VersionConflictError } from '@/shared/api/errors';
import { Button } from '@/shared/ui/Button';
import { StatusBadge } from '@/shared/ui/StatusBadge';

import styles from './AttractionDetailPage.module.css';

export default function AttractionDetailPage() {
  const { session } = useAuth();
  const { id } = useParams();
  const location = useLocation();
  const detail = useAttractionDetail(id);
  const activity = useAttractionActivity(id);
  const command = useAttractionCommand(id ?? '');

  useEffect(() => {
    if (!activity.data) {
      return;
    }

    const targetId = location.hash.replace('#', '');
    if (!targetId) {
      return;
    }

    document.getElementById(targetId)?.scrollIntoView({ block: 'center' });
  }, [activity.data, location.hash]);

  return (
    <section className={styles.page}>
      <Link className={styles.back} to="/attractions">
        ← Attractions
      </Link>
      {detail.isPending ? (
        <p className={styles.muted} role="status">
          Loading attraction
        </p>
      ) : null}
      {detail.isError ? (
        <div className={styles.panel} role="alert">
          <h1>
            {detail.error instanceof NotFoundError
              ? 'Attraction not found'
              : 'Unable to load attraction'}
          </h1>
          <p className={styles.muted}>{detail.error.message}</p>
          {detail.error instanceof NotFoundError ? (
            <Link className={styles.back} to="/attractions">
              Return to attractions
            </Link>
          ) : (
            <Button
              onClick={() => {
                void detail.refetch();
              }}
              disabled={detail.isFetching}
            >
              Retry
            </Button>
          )}
        </div>
      ) : null}
      {detail.data ? (
        <LoadedWorkspace
          canSupervise={session?.role === 'supervisor'}
          attraction={detail.data}
          activity={{
            data: activity.data,
            isPending: activity.isPending,
            isError: activity.isError,
            error: activity.error ?? null,
            isFetching: activity.isFetching,
            refetch: activity.refetch,
          }}
          command={{
            isPending: command.isPending,
            error: command.error ?? null,
            mutate: command.mutate,
            reset: command.reset,
          }}
          onReload={() => {
            command.reset();
            void detail.refetch();
            void activity.refetch();
          }}
          reloading={detail.isFetching || activity.isFetching}
        />
      ) : null}
    </section>
  );
}

function LoadedWorkspace({
  canSupervise,
  attraction,
  activity,
  command,
  onReload,
  reloading,
}: {
  canSupervise: boolean;
  attraction: OperatorAttraction;
  activity: {
    data: AttractionActivity[] | undefined;
    isPending: boolean;
    isError: boolean;
    error: Error | null;
    isFetching: boolean;
    refetch: () => Promise<unknown>;
  };
  command: {
    isPending: boolean;
    error: Error | null;
    mutate: (input: {
      type: AttractionCommandType;
      expectedVersion: number;
      reason?: string;
      waitMinutes?: number;
    }) => void;
    reset: () => void;
  };
  onReload: () => void;
  reloading: boolean;
}) {
  const versionConflict =
    command.error instanceof VersionConflictError ? command.error : null;

  return (
    <>
      <header className={styles.header}>
        <div>
          <p className={styles.kicker}>Command workspace</p>
          <h1>{attraction.name}</h1>
          <p className={styles.area}>
            {attraction.area} · {formatType(attraction.type)}
          </p>
        </div>
        <StatusBadge
          tone={statusTone(attraction.status)}
          label={formatStatus(attraction.status)}
        />
      </header>
      <dl className={styles.facts}>
        <div>
          <dt>Status</dt>
          <dd>{formatStatus(attraction.status)}</dd>
        </div>
        <div>
          <dt>Capacity</dt>
          <dd>{formatCapacity(attraction.capacityMode)}</dd>
        </div>
        <div>
          <dt>Wait time</dt>
          <dd>{formatWaitTime(attraction.waitMinutes)}</dd>
        </div>
        <div>
          <dt>Last updated</dt>
          <dd>{formatUpdatedAt(attraction.updatedAt)}</dd>
        </div>
        <div>
          <dt>Version</dt>
          <dd>{formatVersion(attraction.version)}</dd>
        </div>
      </dl>
      {versionConflict ? (
        <VersionConflictBanner
          error={versionConflict}
          reloading={reloading}
          onReload={onReload}
        />
      ) : null}
      <CommandWorkspaceLayout>
        <div className={styles.stack}>
          {canSupervise ? (
            <CommandPanel
              attraction={attraction}
              pending={command.isPending}
              error={versionConflict ? null : (command.error ?? null)}
              onSubmit={({ type, reason }) => {
                command.mutate({
                  type,
                  expectedVersion: attraction.version,
                  reason,
                });
              }}
            />
          ) : null}
          <WaitTimeForm
            key={`${attraction.id}-${attraction.version}`}
            attraction={attraction}
            pending={command.isPending}
            onSubmit={(waitMinutes) => {
              command.mutate({
                type: 'UPDATE_WAIT_TIME',
                expectedVersion: attraction.version,
                waitMinutes,
              });
            }}
          />
        </div>
        <ActivityTimeline
          activity={activity.data}
          isPending={activity.isPending}
          isError={activity.isError}
          error={activity.error ?? null}
          retrying={activity.isFetching}
          onRetry={() => {
            void activity.refetch();
          }}
        />
      </CommandWorkspaceLayout>
    </>
  );
}
