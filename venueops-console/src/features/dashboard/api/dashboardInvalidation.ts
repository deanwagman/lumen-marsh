import type { QueryClient } from '@tanstack/react-query';

import { DashboardQueries } from './DashboardQueries';

export const DASHBOARD_INVALIDATE_DEBOUNCE_MS = 300;

export const dashboardStreamEvents = new Set([
  'attractions.snapshot',
  'attraction.updated',
  'weather.recommendations.snapshot',
  'weather.recommendation.updated',
  'weather.recommendation.cleared',
  'incidents.snapshot',
  'incident.reported',
  'incident.updated',
  'incident.resolved',
  'maintenance.work-orders.snapshot',
  'maintenance.work-order.updated',
  'flow.snapshot',
  'flow.queue.updated',
  'flow.forecast.updated',
  'flow.recommendation.created',
  'flow.recommendation.updated',
]);

let invalidateTimer: ReturnType<typeof setTimeout> | null = null;

export function scheduleDashboardInvalidation(queryClient: QueryClient): void {
  if (invalidateTimer !== null) {
    clearTimeout(invalidateTimer);
  }
  invalidateTimer = setTimeout(() => {
    invalidateTimer = null;
    void queryClient.invalidateQueries({ queryKey: DashboardQueries.snapshot() });
  }, DASHBOARD_INVALIDATE_DEBOUNCE_MS);
}

export function resetDashboardInvalidation(): void {
  if (invalidateTimer !== null) {
    clearTimeout(invalidateTimer);
    invalidateTimer = null;
  }
}
