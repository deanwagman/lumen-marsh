import type { AttractionActivity } from './activity';
import type { AttractionStatus, AttractionType, CapacityMode } from './attraction';

export function formatLabel(value: string): string {
  return value
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

export function formatStatus(status: AttractionStatus): string {
  return formatLabel(status);
}

export function formatCapacity(capacityMode: CapacityMode): string {
  if (capacityMode === 'NOT_APPLICABLE') {
    return 'N/A';
  }
  return formatLabel(capacityMode);
}

export function formatType(type: AttractionType): string {
  return formatLabel(type);
}

export function formatWaitTime(waitMinutes: number | null): string {
  if (waitMinutes === null) {
    return '—';
  }
  return `${waitMinutes} min`;
}

export function formatUpdatedAt(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return iso;
  }

  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

export function formatVersion(version: number): string {
  return `v${version}`;
}

export function formatActivityTitle(activity: AttractionActivity): string {
  if (activity.type === 'WAIT_TIME_UPDATED') {
    return `Wait time ${formatWaitTime(activity.previousMinutes)} → ${formatWaitTime(activity.newMinutes)}`;
  }

  if (activity.type === 'CAPACITY_REDUCED' || activity.type === 'CAPACITY_RESTORED') {
    return `Capacity ${formatCapacity(activity.previousMode ?? 'NOT_APPLICABLE')} → ${formatCapacity(activity.newMode ?? 'NOT_APPLICABLE')}`;
  }

  if (activity.previousStatus && activity.newStatus) {
    return `${formatStatus(activity.previousStatus)} → ${formatStatus(activity.newStatus)}`;
  }

  return formatLabel(activity.type);
}

export function formatActivityType(activity: AttractionActivity): string {
  return formatLabel(activity.type);
}
