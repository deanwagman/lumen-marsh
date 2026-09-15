import type { QueryClient } from '@tanstack/react-query';

import type { ApiClient } from '@/shared/api/client';
import type {
  AssetListFilter,
  MaintenanceWorkOrder,
  PageResult,
  WorkOrderListFilter,
} from '@/features/maintenance/domain/maintenance';

import {
  maintenanceActivityListSchema,
  maintenanceAssetPageSchema,
  maintenanceAssetSchema,
  maintenanceWorkOrderPageSchema,
  maintenanceWorkOrderSchema,
  reliabilityRecommendationListSchema,
  reliabilityRecommendationSchema,
} from './maintenanceSchemas';

export type WorkOrderQueryFilters = {
  status?: string;
  priority?: string;
  classification?: string;
  attractionId?: string;
  assetId?: string;
  incidentId?: string;
  assignedTeam?: string;
  lifecycle?: string;
  page: number;
  size: number;
};

export type AssetQueryFilters = {
  attractionId?: string;
  assetType?: string;
  criticality?: string;
  serviceStatus?: string;
  page: number;
  size: number;
};

export const MaintenanceQueries = {
  all: ['maintenance'] as const,
  summary: () => [...MaintenanceQueries.all, 'summary'] as const,
  workOrders: (filters: WorkOrderQueryFilters) =>
    [...MaintenanceQueries.all, 'work-orders', filters] as const,
  workOrder: (id: string) => [...MaintenanceQueries.all, 'work-orders', 'detail', id] as const,
  activity: (id: string) => [...MaintenanceQueries.all, 'work-orders', id, 'activity'] as const,
  assets: (filters: AssetQueryFilters) => [...MaintenanceQueries.all, 'assets', filters] as const,
  asset: (id: string) => [...MaintenanceQueries.all, 'assets', 'detail', id] as const,
  assetWorkOrders: (id: string, page = 0, size = 50) =>
    [...MaintenanceQueries.all, 'assets', id, 'work-orders', page, size] as const,
  recommendations: () => [...MaintenanceQueries.all, 'recommendations'] as const,
  recommendation: (id: string) => [...MaintenanceQueries.all, 'recommendations', id] as const,
  summaries: () => [...MaintenanceQueries.all, 'work-order-summaries'] as const,
};

export function toWorkOrderQueryFilters(filter: WorkOrderListFilter): WorkOrderQueryFilters {
  return {
    status: filter.status === 'all' ? undefined : filter.status,
    priority: filter.priority === 'all' ? undefined : filter.priority,
    classification: filter.classification === 'all' ? undefined : filter.classification,
    attractionId: filter.attractionId === 'all' ? undefined : filter.attractionId,
    assetId: filter.assetId === 'all' ? undefined : filter.assetId,
    incidentId: filter.incidentId || undefined,
    assignedTeam: filter.assignedTeam || undefined,
    lifecycle: filter.lifecycle,
    page: filter.page,
    size: filter.size,
  };
}

export function toAssetQueryFilters(filter: AssetListFilter): AssetQueryFilters {
  return {
    attractionId: filter.attractionId === 'all' ? undefined : filter.attractionId,
    assetType: filter.assetType === 'all' ? undefined : filter.assetType,
    criticality: filter.criticality === 'all' ? undefined : filter.criticality,
    serviceStatus: filter.serviceStatus === 'all' ? undefined : filter.serviceStatus,
    page: filter.page,
    size: filter.size,
  };
}

function searchParams(filters: Record<string, string | number | undefined>): string {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(filters)) {
    if (value === undefined || value === '') {
      continue;
    }
    params.set(key, String(value));
  }
  const encoded = params.toString();
  return encoded ? `?${encoded}` : '';
}

export function maintenanceWorkOrderListQuery(client: ApiClient, filters: WorkOrderQueryFilters) {
  return {
    queryKey: MaintenanceQueries.workOrders(filters),
    queryFn: ({ signal }: { signal: AbortSignal }) =>
      client.get(
        `/api/v1/operator/maintenance/work-orders${searchParams(filters)}`,
        maintenanceWorkOrderPageSchema,
        { signal },
      ),
  };
}

export function maintenanceWorkOrderQuery(client: ApiClient, workOrderId: string) {
  return {
    queryKey: MaintenanceQueries.workOrder(workOrderId),
    queryFn: ({ signal }: { signal: AbortSignal }) =>
      client.get(
        `/api/v1/operator/maintenance/work-orders/${encodeURIComponent(workOrderId)}`,
        maintenanceWorkOrderSchema,
        { signal },
      ),
  };
}

export function maintenanceActivityQuery(client: ApiClient, workOrderId: string) {
  return {
    queryKey: MaintenanceQueries.activity(workOrderId),
    queryFn: ({ signal }: { signal: AbortSignal }) =>
      client.get(
        `/api/v1/operator/maintenance/work-orders/${encodeURIComponent(workOrderId)}/activity`,
        maintenanceActivityListSchema,
        { signal },
      ),
  };
}

export function maintenanceAssetListQuery(client: ApiClient, filters: AssetQueryFilters) {
  return {
    queryKey: MaintenanceQueries.assets(filters),
    queryFn: ({ signal }: { signal: AbortSignal }) =>
      client.get(
        `/api/v1/operator/maintenance/assets${searchParams(filters)}`,
        maintenanceAssetPageSchema,
        { signal },
      ),
  };
}

export function maintenanceAssetQuery(client: ApiClient, assetId: string) {
  return {
    queryKey: MaintenanceQueries.asset(assetId),
    queryFn: ({ signal }: { signal: AbortSignal }) =>
      client.get(
        `/api/v1/operator/maintenance/assets/${encodeURIComponent(assetId)}`,
        maintenanceAssetSchema,
        { signal },
      ),
  };
}

export function maintenanceAssetWorkOrdersQuery(
  client: ApiClient,
  assetId: string,
  page = 0,
  size = 50,
) {
  return {
    queryKey: MaintenanceQueries.assetWorkOrders(assetId, page, size),
    queryFn: ({ signal }: { signal: AbortSignal }) =>
      client.get(
        `/api/v1/operator/maintenance/assets/${encodeURIComponent(assetId)}/work-orders${searchParams({ page, size })}`,
        maintenanceWorkOrderPageSchema,
        { signal },
      ),
  };
}

export function maintenanceRecommendationListQuery(client: ApiClient) {
  return {
    queryKey: MaintenanceQueries.recommendations(),
    queryFn: ({ signal }: { signal: AbortSignal }) =>
      client.get(
        '/api/v1/operator/maintenance/recommendations',
        reliabilityRecommendationListSchema,
        { signal },
      ),
  };
}

export function maintenanceRecommendationQuery(client: ApiClient, recommendationId: string) {
  return {
    queryKey: MaintenanceQueries.recommendation(recommendationId),
    queryFn: ({ signal }: { signal: AbortSignal }) =>
      client.get(
        `/api/v1/operator/maintenance/recommendations/${encodeURIComponent(recommendationId)}`,
        reliabilityRecommendationSchema,
        { signal },
      ),
  };
}

export function patchWorkOrderInPages(
  queryClient: QueryClient,
  workOrder: MaintenanceWorkOrder,
): void {
  const pages = queryClient.getQueriesData<PageResult<MaintenanceWorkOrder>>({
    queryKey: [...MaintenanceQueries.all, 'work-orders'],
  });
  for (const [key, page] of pages) {
    if (!page || !('items' in page)) {
      continue;
    }
    const index = page.items.findIndex((item) => item.id === workOrder.id);
    if (index < 0) {
      continue;
    }
    const current = page.items[index];
    if (workOrder.version < current.version) {
      continue;
    }
    const items = [...page.items];
    items[index] = workOrder;
    queryClient.setQueryData(key, { ...page, items });
  }
}
