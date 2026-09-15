import { Link } from 'react-router-dom';

import { useAuth } from '@/auth/AuthContext';
import { useAttractions } from '@/features/attractions/hooks/useAttractions';
import { formatUpdatedAt } from '@/features/attractions/domain/formatters';
import { useAttractionStreamHealth } from '@/features/attractions/hooks/useAttractionStream';
import { streamStatusLabel } from '@/features/attractions/domain/streamHealth';
import { MaintenanceSummary } from '@/features/maintenance/components/MaintenanceSummary';
import { MaintenancePriorityBadge } from '@/features/maintenance/components/MaintenancePriorityBadge';
import { ReliabilityRecommendationCard } from '@/features/maintenance/components/ReliabilityRecommendationCard';
import { WorkOrderStatusBadge } from '@/features/maintenance/components/WorkOrderStatusBadge';
import { WorkOrderTable } from '@/features/maintenance/components/WorkOrderTable';
import {
  canReadMaintenance,
  compareQueueItems,
  defaultAssetFilter,
  defaultWorkOrderFilter,
  isActiveWorkOrderStatus,
  sortWorkOrders,
  summarizeMaintenance,
  type MaintenanceQueueItem,
} from '@/features/maintenance/domain/maintenance';
import { nextActionLabel } from '@/features/maintenance/domain/maintenanceActions';
import {
  assignmentLabel,
  formatEstimatedRestore,
  formatMaintenanceTime,
  formatObservationAge,
} from '@/features/maintenance/domain/maintenancePresentation';
import { useMaintenanceAssets } from '@/features/maintenance/hooks/useMaintenanceAssets';
import { useMaintenanceRecommendationCommand } from '@/features/maintenance/hooks/useMaintenanceCommand';
import { useMaintenanceParams } from '@/features/maintenance/hooks/useMaintenanceParams';
import { useMaintenanceRecommendations } from '@/features/maintenance/hooks/useMaintenanceRecommendations';
import { useMaintenanceWorkOrders } from '@/features/maintenance/hooks/useMaintenanceWorkOrders';
import { formatLabel } from '@/features/attractions/domain/formatters';
import { Button } from '@/shared/ui/Button';
import { HelpHeading } from '@/shared/ui/HelpLink';
import { DuplicateCommandError, ForbiddenError, VersionConflictError } from '@/shared/api/errors';
import { VersionConflictBanner } from '@/features/attractions/components/CommandWorkspace';

import styles from './MaintenancePage.module.css';

export default function MaintenancePage() {
  const { session } = useAuth();
  const { section, recommendationView, filter, setFilter } = useMaintenanceParams();
  const attractions = useAttractions();
  const stream = useAttractionStreamHealth();
  const overview = useMaintenanceWorkOrders({ ...defaultWorkOrderFilter, lifecycle: 'all', size: 100, page: 0 });
  const workOrders = useMaintenanceWorkOrders(filter);
  const recommendations = useMaintenanceRecommendations();
  const assets = useMaintenanceAssets({ ...defaultAssetFilter, size: 100 });
  const recCommand = useMaintenanceRecommendationCommand();

  if (!canReadMaintenance(session)) {
    return (
      <section className={styles.page}>
        <h1>Maintenance</h1>
        <p>This workspace requires the venueops/maintenance.read scope.</p>
      </section>
    );
  }

  const catalog = attractions.data ?? [];
  const names = new Map(catalog.map((attraction) => [attraction.id, attraction.name]));
  const assetAttractionIds = new Map(
    (assets.data?.items ?? []).map((asset) => [asset.id, asset.attractionId]),
  );
  const overviewItems = overview.data?.items ?? [];
  const recs = recommendations.data ?? [];
  const metrics = summarizeMaintenance({
    workOrders: overviewItems,
    recommendations: recs,
    assets: assets.data?.items ?? [],
  });
  const queue = buildQueue(overviewItems, recs, recommendationView);
  const listed = sortWorkOrders(workOrders.data?.items ?? [], filter.sort);
  const total = workOrders.data?.total ?? 0;
  const pageSize = workOrders.data?.size ?? filter.size;
  const currentPage = workOrders.data?.page ?? filter.page;
  const totalPages = Math.max(1, Math.ceil(total / pageSize) || 1);
  const from = total === 0 ? 0 : currentPage * pageSize + 1;
  const to = Math.min(total, (currentPage + 1) * pageSize);
  const pendingRecs = recs.filter((item) => item.status === 'PENDING_REVIEW');
  const visibleRecs = recommendationView === 'all' ? recs : pendingRecs;
  const lastSynchronized =
    workOrders.dataUpdatedAt > 0
      ? formatUpdatedAt(new Date(workOrders.dataUpdatedAt).toISOString())
      : null;

  return (
    <section className={styles.page}>
      <header className={styles.header}>
        <div>
          <p className={styles.kicker}>Control Tower</p>
          <h1>Maintenance</h1>
          <p className={styles.lede}>
            Reliability recommendations and work orders for ride systems. Attraction operations stay a
            separate workflow — this workspace never reopens a ride.
          </p>
        </div>
        <div className={styles.meta}>
          <p>{streamStatusLabel(stream.data?.status ?? 'connecting')}</p>
          <p className={styles.muted}>
            Last synchronized {lastSynchronized ?? 'waiting'}
          </p>
          <Button onClick={() => void Promise.all([overview.refetch(), workOrders.refetch(), recommendations.refetch(), assets.refetch()])}>
            Refresh
          </Button>
        </div>
      </header>

      <MaintenanceSummary metrics={metrics} />
      {overview.isPending || recommendations.isPending || assets.isPending ? (
        <p className={styles.muted} role="status">
          Loading maintenance overview
        </p>
      ) : null}
      {overview.isError || recommendations.isError || assets.isError ? (
        <div className={styles.error} role="alert">
          <p>Unable to load the maintenance workspace.</p>
          <Button onClick={() => void Promise.all([overview.refetch(), workOrders.refetch(), recommendations.refetch(), assets.refetch()])}>
            Retry
          </Button>
        </div>
      ) : null}

      <section className={styles.queue} aria-labelledby="priority-queue-heading">
        <h2 id="priority-queue-heading">Priority queue</h2>
        {queue.length === 0 ? (
          <p className={styles.muted}>No active maintenance items require attention.</p>
        ) : (
          <ol className={styles.queueList}>
            {queue.map((item) =>
              item.kind === 'work-order' ? (
                <li key={item.workOrder.id}>
                  <Link className={styles.queueLink} to={`/maintenance/work-orders/${item.workOrder.id}`}>
                    <span className={styles.mono}>{item.workOrder.workOrderNumber}</span>
                    <MaintenancePriorityBadge compact priority={item.workOrder.priority} />
                    <WorkOrderStatusBadge status={item.workOrder.status} />
                    <span>{names.get(item.workOrder.attractionId) ?? item.workOrder.attractionId}</span>
                    <span className={styles.mono}>{item.workOrder.asset.assetCode}</span>
                    <span>{assignmentLabel(item.workOrder)}</span>
                    <span>{formatEstimatedRestore(item.workOrder.estimatedRestoreAt)}</span>
                    <span>{formatMaintenanceTime(item.workOrder.updatedAt)}</span>
                    <span>{nextActionLabel(item.workOrder)}</span>
                  </Link>
                </li>
              ) : (
                <li key={item.recommendation.recommendationId}>
                  <button
                    type="button"
                    className={styles.queueLink}
                    onClick={() => setFilter({ section: 'inbox' })}
                  >
                    <span>{formatLabel(item.recommendation.signalType)}</span>
                    <span>{formatLabel(item.recommendation.severity)}</span>
                    <span>
                      {names.get(assetAttractionIds.get(item.recommendation.assetId) ?? '') ??
                        assetAttractionIds.get(item.recommendation.assetId) ??
                        'Attraction'}
                    </span>
                    <span className={styles.mono}>{item.recommendation.assetCode}</span>
                    <span>Pending review</span>
                    <span>Unassigned</span>
                    <span>{formatObservationAge(item.recommendation.updatedAt)}</span>
                    <span>Review reliability evidence</span>
                  </button>
                </li>
              ),
            )}
          </ol>
        )}
      </section>

      <div className={styles.tabs} role="tablist" aria-label="Maintenance sections">
        <button
          type="button"
          role="tab"
          aria-selected={section === 'work-orders'}
          onClick={() => setFilter({ section: 'work-orders' })}
        >
          Work orders
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={section === 'inbox'}
          onClick={() => setFilter({ section: 'inbox' })}
        >
          Reliability inbox
        </button>
      </div>

      {section === 'work-orders' ? (
        <section aria-labelledby="work-orders-heading">
          <h2 id="work-orders-heading">Work orders</h2>
          <div className={styles.filters}>
            <label>
              Status
              <select
                value={filter.status}
                onChange={(event) => setFilter({ status: event.target.value as typeof filter.status })}
              >
                <option value="all">All statuses</option>
                <option value="DRAFT">Draft</option>
                <option value="OPEN">Open</option>
                <option value="ASSIGNED">Assigned</option>
                <option value="IN_PROGRESS">In progress</option>
                <option value="AWAITING_INSPECTION">Awaiting inspection</option>
                <option value="READY_FOR_TESTING">Ready for testing</option>
                <option value="COMPLETED">Completed</option>
                <option value="CANCELED">Canceled</option>
              </select>
            </label>
            <label>
              Priority
              <select
                value={filter.priority}
                onChange={(event) => setFilter({ priority: event.target.value as typeof filter.priority })}
              >
                <option value="all">All priorities</option>
                <option value="P1">P1</option>
                <option value="P2">P2</option>
                <option value="P3">P3</option>
                <option value="P4">P4</option>
              </select>
            </label>
            <label>
              Classification
              <select
                value={filter.classification}
                onChange={(event) =>
                  setFilter({ classification: event.target.value as typeof filter.classification })
                }
              >
                <option value="all">All classifications</option>
                <option value="CORRECTIVE">Corrective</option>
                <option value="PREVENTIVE">Preventive</option>
                <option value="INSPECTION">Inspection</option>
                <option value="CALIBRATION">Calibration</option>
              </select>
            </label>
            <label>
              Attraction
              <select
                value={filter.attractionId}
                onChange={(event) => setFilter({ attractionId: event.target.value })}
              >
                <option value="all">All attractions</option>
                {catalog.map((attraction) => (
                  <option key={attraction.id} value={attraction.id}>
                    {attraction.name}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Asset
              <select
                value={filter.assetId}
                onChange={(event) => setFilter({ assetId: event.target.value })}
              >
                <option value="all">All assets</option>
                {(assets.data?.items ?? []).map((asset) => (
                  <option key={asset.id} value={asset.id}>
                    {asset.assetCode}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Assigned team
              <input
                value={filter.assignedTeam}
                placeholder="Any team"
                onChange={(event) => setFilter({ assignedTeam: event.target.value })}
              />
            </label>
            <label>
              Incident
              <input
                value={filter.incidentId}
                placeholder="Incident ID"
                onChange={(event) => setFilter({ incidentId: event.target.value })}
              />
            </label>
            <label>
              Lifecycle
              <select
                value={filter.lifecycle}
                onChange={(event) => setFilter({ lifecycle: event.target.value as typeof filter.lifecycle })}
              >
                <option value="active">Active</option>
                <option value="terminal">Terminal</option>
                <option value="all">All</option>
              </select>
            </label>
            <label>
              Sort
              <select
                value={filter.sort}
                onChange={(event) => setFilter({ sort: event.target.value as typeof filter.sort })}
              >
                <option value="operational">Operational severity</option>
                <option value="updated">Last updated</option>
                <option value="estimatedRestore">Estimated restoration</option>
                <option value="workOrderNumber">Work-order number</option>
              </select>
            </label>
          </div>
          {workOrders.isPending ? (
            <p className={styles.muted} role="status">
              Loading work orders
            </p>
          ) : null}
          {workOrders.isError ? (
            <div className={styles.error} role="alert">
              <p>{workOrders.error.message}</p>
              <Button onClick={() => void workOrders.refetch()}>Retry</Button>
            </div>
          ) : (
            <WorkOrderTable workOrders={listed} attractionNames={names} />
          )}
          {workOrders.data ? (
            <nav className={styles.pager} aria-label="Work-order pages">
              <p className={styles.muted}>
                Showing {from}–{to} of {total}
              </p>
              <Button
                variant="ghost"
                disabled={currentPage <= 0}
                onClick={() => setFilter({ page: currentPage - 1 })}
              >
                Previous
              </Button>
              <Button
                variant="ghost"
                disabled={currentPage >= totalPages - 1 || total === 0}
                onClick={() => setFilter({ page: currentPage + 1 })}
              >
                Next
              </Button>
            </nav>
          ) : null}
        </section>
      ) : (
        <section aria-labelledby="inbox-heading">
          <div className={styles.inboxHeader}>
            <HelpHeading
              id="inbox-heading"
              article="maintenance"
              section="reliability"
              label="Help: reliability recommendation review"
            >
              Reliability inbox
            </HelpHeading>
            <label>
              Show
              <select
                value={recommendationView}
                onChange={(event) =>
                  setFilter({ recommendationView: event.target.value as 'pending' | 'all' })
                }
              >
                <option value="pending">Pending review</option>
                <option value="all">All recommendations</option>
              </select>
            </label>
          </div>
          {recCommand.error instanceof VersionConflictError ? (
            <VersionConflictBanner
              error={recCommand.error}
              reloading={recommendations.isFetching}
              onReload={() => {
                recCommand.reset();
                recCommand.beginNewIntent();
                void recommendations.refetch();
              }}
            />
          ) : null}
          {recCommand.error instanceof ForbiddenError ? (
            <p role="alert">Supervisor or maintenance command authorization is required. Form entries were kept.</p>
          ) : null}
          {recCommand.error instanceof DuplicateCommandError ? (
            <p role="alert">
              That command identifier belongs to another resource. Retry as a new action if you still
              want the change.
            </p>
          ) : null}
          {recommendations.isPending ? (
            <p className={styles.muted} role="status">
              Loading recommendations
            </p>
          ) : null}
          {recommendations.isError ? (
            <div className={styles.error} role="alert">
              <p>{recommendations.error.message}</p>
              <Button onClick={() => void recommendations.refetch()}>Retry</Button>
            </div>
          ) : null}
          {!recommendations.isPending && visibleRecs.length === 0 ? (
            <p className={styles.muted}>No reliability recommendations in this view.</p>
          ) : null}
          <ul className={styles.inbox}>
            {visibleRecs.map((recommendation) => (
              <li key={recommendation.recommendationId}>
                <ReliabilityRecommendationCard
                  recommendation={recommendation}
                  pending={recCommand.isPending}
                  error={
                    recCommand.variables?.recommendationId === recommendation.recommendationId
                      ? recCommand.error
                      : null
                  }
                  onAccept={(reason) =>
                    recCommand.mutate({
                      recommendationId: recommendation.recommendationId,
                      type: 'ACCEPT',
                      expectedVersion: recommendation.version,
                      reason,
                    })
                  }
                  onDismiss={(reason) =>
                    recCommand.mutate({
                      recommendationId: recommendation.recommendationId,
                      type: 'DISMISS',
                      expectedVersion: recommendation.version,
                      reason,
                    })
                  }
                />
              </li>
            ))}
          </ul>
        </section>
      )}
    </section>
  );
}

function buildQueue(
  workOrders: ReturnType<typeof sortWorkOrders>,
  recommendations: ReturnType<typeof useMaintenanceRecommendations>['data'],
  recommendationView: string,
): MaintenanceQueueItem[] {
  const items: MaintenanceQueueItem[] = workOrders
    .filter((workOrder) => isActiveWorkOrderStatus(workOrder.status))
    .map((workOrder) => ({ kind: 'work-order', workOrder }));
  const recs = recommendationView === 'all'
    ? (recommendations ?? []).filter((item) => item.status === 'PENDING_REVIEW')
    : (recommendations ?? []).filter((item) => item.status === 'PENDING_REVIEW');
  for (const recommendation of recs) {
    items.push({ kind: 'recommendation', recommendation });
  }
  return items.sort(compareQueueItems).slice(0, 12);
}
