import { useSearchParams } from 'react-router-dom';

import {
  defaultWorkOrderFilter,
  maintenanceClassifications,
  maintenancePriorities,
  maintenanceWorkOrderStatuses,
  type MaintenanceSection,
  type WorkOrderListFilter,
  type WorkOrderSort,
} from '@/features/maintenance/domain/maintenance';

const statuses = new Set<string>(maintenanceWorkOrderStatuses);
const priorities = new Set<string>(maintenancePriorities);
const classifications = new Set<string>(maintenanceClassifications);
const sorts = new Set<string>(['operational', 'updated', 'estimatedRestore', 'workOrderNumber']);

export function useMaintenanceParams() {
  const [params, setParams] = useSearchParams();

  const section = parseSection(params.get('section'));
  const recommendationView = params.get('recommendations') === 'all' ? 'all' : 'pending';
  const filter: WorkOrderListFilter = {
    status: parseStatus(params.get('status')),
    priority: parsePriority(params.get('priority')),
    classification: parseClassification(params.get('classification')),
    attractionId: params.get('attraction') || 'all',
    assetId: params.get('asset') || 'all',
    incidentId: params.get('incident') || '',
    assignedTeam: params.get('team') || '',
    lifecycle: parseLifecycle(params.get('lifecycle')),
    sort: parseSort(params.get('sort')),
    page: parsePage(params.get('page')),
    size: parseSize(params.get('size')),
  };

  function update(next: Partial<WorkOrderListFilter> & { section?: MaintenanceSection; recommendationView?: 'pending' | 'all' }) {
    const merged: WorkOrderListFilter = {
      ...filter,
      ...next,
      page: next.page ?? (next.status || next.priority || next.classification || next.attractionId || next.assetId || next.lifecycle || next.sort ? 0 : filter.page),
    };
    const nextParams = new URLSearchParams(params);
    setOrDelete(nextParams, 'section', next.section ?? section, 'work-orders');
    setOrDelete(
      nextParams,
      'recommendations',
      next.recommendationView ?? recommendationView,
      'pending',
    );
    setOrDelete(nextParams, 'status', merged.status, defaultWorkOrderFilter.status);
    setOrDelete(nextParams, 'priority', merged.priority, 'all');
    setOrDelete(nextParams, 'classification', merged.classification, 'all');
    setOrDelete(nextParams, 'attraction', merged.attractionId, 'all');
    setOrDelete(nextParams, 'asset', merged.assetId, 'all');
    setOrDelete(nextParams, 'incident', merged.incidentId, '');
    setOrDelete(nextParams, 'team', merged.assignedTeam, '');
    setOrDelete(nextParams, 'lifecycle', merged.lifecycle, defaultWorkOrderFilter.lifecycle);
    setOrDelete(nextParams, 'sort', merged.sort, defaultWorkOrderFilter.sort);
    setOrDelete(nextParams, 'page', String(merged.page), '0');
    setOrDelete(nextParams, 'size', String(merged.size), String(defaultWorkOrderFilter.size));
    setParams(nextParams, { replace: true });
  }

  return {
    section,
    recommendationView,
    filter,
    setFilter: update,
  };
}

function setOrDelete(params: URLSearchParams, key: string, value: string, defaultValue: string) {
  if (value === defaultValue) {
    params.delete(key);
  } else {
    params.set(key, value);
  }
}

function parseSection(value: string | null): MaintenanceSection {
  return value === 'inbox' ? 'inbox' : 'work-orders';
}

function parseStatus(value: string | null): WorkOrderListFilter['status'] {
  if (value && statuses.has(value)) {
    return value as WorkOrderListFilter['status'];
  }
  return 'all';
}

function parsePriority(value: string | null): WorkOrderListFilter['priority'] {
  if (value && priorities.has(value)) {
    return value as WorkOrderListFilter['priority'];
  }
  return 'all';
}

function parseClassification(value: string | null): WorkOrderListFilter['classification'] {
  if (value && classifications.has(value)) {
    return value as WorkOrderListFilter['classification'];
  }
  return 'all';
}

function parseLifecycle(value: string | null): WorkOrderListFilter['lifecycle'] {
  if (value === 'terminal' || value === 'all' || value === 'active') {
    return value;
  }
  return 'active';
}

function parseSort(value: string | null): WorkOrderSort {
  if (value && sorts.has(value)) {
    return value as WorkOrderSort;
  }
  return 'operational';
}

function parsePage(value: string | null): number {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed >= 0 ? parsed : 0;
}

function parseSize(value: string | null): number {
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed <= 0) {
    return 50;
  }
  return Math.min(parsed, 100);
}
