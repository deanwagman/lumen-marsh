import type { QueryClient } from '@tanstack/react-query';

import { IncidentQueries } from '@/features/incidents/api/IncidentQueries';
import type {
  MaintenanceWorkOrder,
  MaintenanceWorkOrderSummary,
  PageResult,
  ReliabilityRecommendation,
} from '@/features/maintenance/domain/maintenance';
import { applyNewerWorkOrder } from '@/features/maintenance/domain/maintenance';

import { MaintenanceQueries, patchWorkOrderInPages } from './MaintenanceQueries';

export function applyRecommendation(
  queryClient: QueryClient,
  recommendation: ReliabilityRecommendation,
): void {
  queryClient.setQueryData(MaintenanceQueries.recommendation(recommendation.recommendationId), recommendation);
  queryClient.setQueryData<ReliabilityRecommendation[]>(
    MaintenanceQueries.recommendations(),
    (current) => {
      const list = current ?? [];
      const existing = list.find((item) => item.recommendationId === recommendation.recommendationId);
      if (existing && recommendation.version < existing.version) {
        return list;
      }
      return [
        ...list.filter((item) => item.recommendationId !== recommendation.recommendationId),
        recommendation,
      ].sort((left, right) => Date.parse(right.updatedAt) - Date.parse(left.updatedAt));
    },
  );
}

export function applyWorkOrderDetail(
  queryClient: QueryClient,
  workOrder: MaintenanceWorkOrder,
): void {
  const current = queryClient.getQueryData<MaintenanceWorkOrder>(
    MaintenanceQueries.workOrder(workOrder.id),
  );
  const next = applyNewerWorkOrder(current, workOrder);
  if (!next) {
    return;
  }
  queryClient.setQueryData(MaintenanceQueries.workOrder(workOrder.id), next);
  patchWorkOrderInPages(queryClient, next);
  patchSummaries(queryClient, {
    id: next.id,
    workOrderNumber: next.workOrderNumber,
    status: next.status,
    priority: next.priority,
    version: next.version,
    updatedAt: next.updatedAt,
  });
}

export function replaceWorkOrderSummaries(
  queryClient: QueryClient,
  snapshots: MaintenanceWorkOrderSummary[],
): void {
  const previous =
    queryClient.getQueryData<MaintenanceWorkOrderSummary[]>(MaintenanceQueries.summaries()) ?? [];
  const previousById = new Map(previous.map((item) => [item.id, item]));
  const next = snapshots.map((snapshot) => {
    const current = previousById.get(snapshot.id);
    if (current && snapshot.version < current.version) {
      return current;
    }
    return snapshot;
  });
  queryClient.setQueryData(MaintenanceQueries.summaries(), next);

  for (const snapshot of next) {
    patchListPagesWithSummary(queryClient, snapshot);
    const detail = queryClient.getQueryData<MaintenanceWorkOrder>(
      MaintenanceQueries.workOrder(snapshot.id),
    );
    if (detail && snapshot.version > detail.version) {
      void queryClient.invalidateQueries({ queryKey: MaintenanceQueries.workOrder(snapshot.id) });
      void queryClient.invalidateQueries({ queryKey: MaintenanceQueries.activity(snapshot.id) });
    }
  }
}

export function applyWorkOrderSummaryUpdate(
  queryClient: QueryClient,
  snapshot: MaintenanceWorkOrderSummary,
  extras?: { incidentId?: string | null },
): void {
  const summaries =
    queryClient.getQueryData<MaintenanceWorkOrderSummary[]>(MaintenanceQueries.summaries()) ?? [];
  const current = summaries.find((item) => item.id === snapshot.id);
  if (current && snapshot.version < current.version) {
    return;
  }
  const without = summaries.filter((item) => item.id !== snapshot.id);
  queryClient.setQueryData(MaintenanceQueries.summaries(), [...without, snapshot]);
  patchListPagesWithSummary(queryClient, snapshot);

  const detail = queryClient.getQueryData<MaintenanceWorkOrder>(
    MaintenanceQueries.workOrder(snapshot.id),
  );
  if (!detail || snapshot.version > detail.version) {
    void queryClient.invalidateQueries({ queryKey: MaintenanceQueries.workOrder(snapshot.id) });
    void queryClient.invalidateQueries({ queryKey: MaintenanceQueries.activity(snapshot.id) });
  }

  void queryClient.invalidateQueries({ queryKey: [...MaintenanceQueries.all, 'assets'] });
  void queryClient.invalidateQueries({ queryKey: MaintenanceQueries.recommendations() });
  if (extras?.incidentId) {
    void queryClient.invalidateQueries({ queryKey: IncidentQueries.detail(extras.incidentId) });
  }
}

export async function refreshAfterWorkOrderCommand(
  queryClient: QueryClient,
  workOrderId: string,
): Promise<void> {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: MaintenanceQueries.workOrder(workOrderId) }),
    queryClient.invalidateQueries({ queryKey: MaintenanceQueries.activity(workOrderId) }),
    queryClient.invalidateQueries({ queryKey: [...MaintenanceQueries.all, 'work-orders'] }),
    queryClient.invalidateQueries({ queryKey: [...MaintenanceQueries.all, 'assets'] }),
    queryClient.invalidateQueries({ queryKey: MaintenanceQueries.recommendations() }),
    queryClient.invalidateQueries({ queryKey: MaintenanceQueries.summaries() }),
  ]);
}

export async function refreshAfterRecommendationCommand(
  queryClient: QueryClient,
  recommendation: ReliabilityRecommendation,
): Promise<void> {
  applyRecommendation(queryClient, recommendation);
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: [...MaintenanceQueries.all, 'work-orders'] }),
    queryClient.invalidateQueries({ queryKey: MaintenanceQueries.summaries() }),
    recommendation.workOrderId
      ? queryClient.invalidateQueries({
          queryKey: MaintenanceQueries.workOrder(recommendation.workOrderId),
        })
      : Promise.resolve(),
    queryClient.invalidateQueries({ queryKey: [...MaintenanceQueries.all, 'assets'] }),
  ]);
}

function patchSummaries(queryClient: QueryClient, snapshot: MaintenanceWorkOrderSummary): void {
  const summaries =
    queryClient.getQueryData<MaintenanceWorkOrderSummary[]>(MaintenanceQueries.summaries()) ?? [];
  const current = summaries.find((item) => item.id === snapshot.id);
  if (current && snapshot.version < current.version) {
    return;
  }
  queryClient.setQueryData(MaintenanceQueries.summaries(), [
    ...summaries.filter((item) => item.id !== snapshot.id),
    snapshot,
  ]);
}

function patchListPagesWithSummary(
  queryClient: QueryClient,
  snapshot: MaintenanceWorkOrderSummary,
): void {
  const pages = queryClient.getQueriesData<PageResult<MaintenanceWorkOrder>>({
    queryKey: [...MaintenanceQueries.all, 'work-orders'],
  });
  for (const [key, page] of pages) {
    if (!page || !('items' in page)) {
      continue;
    }
    const index = page.items.findIndex((item) => item.id === snapshot.id);
    if (index < 0) {
      continue;
    }
    const current = page.items[index];
    if (snapshot.version < current.version) {
      continue;
    }
    const items = [...page.items];
    items[index] = {
      ...current,
      workOrderNumber: snapshot.workOrderNumber,
      status: snapshot.status,
      priority: snapshot.priority,
      version: snapshot.version,
      updatedAt: snapshot.updatedAt,
    };
    queryClient.setQueryData(key, { ...page, items });
  }
}
