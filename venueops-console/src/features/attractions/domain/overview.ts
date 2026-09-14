import type { AttractionStatus, AttractionSummary } from './attraction';
import { formatStatus, formatWaitTime } from './formatters';

export const overviewFilters = [
  'all',
  'attention',
  'operating',
  'holds',
  'closed',
  'reduced',
] as const;

export type OverviewFilter = (typeof overviewFilters)[number];

export const overviewSorts = ['severity', 'recent', 'wait', 'name'] as const;

export type OverviewSort = (typeof overviewSorts)[number];

export const overviewFilterLabels: Record<OverviewFilter, string> = {
  all: 'All',
  attention: 'Attention required',
  operating: 'Operating',
  holds: 'Holds',
  closed: 'Closed',
  reduced: 'Reduced capacity',
};

export const overviewSortLabels: Record<OverviewSort, string> = {
  severity: 'Operational severity',
  recent: 'Most recently changed',
  wait: 'Highest wait',
  name: 'Attraction name',
};

export function parseOverviewFilter(value: string | null): OverviewFilter {
  return overviewFilters.find((filter) => filter === value) ?? 'all';
}

export function parseOverviewSort(value: string | null): OverviewSort {
  return overviewSorts.find((sort) => sort === value) ?? 'severity';
}

export function isHoldStatus(status: AttractionStatus): boolean {
  return status === 'WEATHER_HOLD' || status === 'TECHNICAL_DELAY';
}

export function needsAttention(attraction: AttractionSummary): boolean {
  if (isHoldStatus(attraction.status)) {
    return true;
  }

  if (attraction.status === 'TESTING' || attraction.status === 'RETURNING_TO_SERVICE') {
    return true;
  }

  if (attraction.status === 'OPERATING' && attraction.capacityMode === 'REDUCED') {
    return true;
  }

  return attraction.status === 'CLOSED' && attraction.version > 0;
}

export function attentionSeverity(attraction: AttractionSummary): number {
  switch (attraction.status) {
    case 'TECHNICAL_DELAY':
      return 0;
    case 'WEATHER_HOLD':
      return 1;
    case 'CLOSED':
      return attraction.version > 0 ? 2 : 7;
    case 'TESTING':
      return 3;
    case 'RETURNING_TO_SERVICE':
      return 4;
    case 'OPERATING':
      return attraction.capacityMode === 'REDUCED' ? 5 : 6;
  }
}

export function attentionReason(attraction: AttractionSummary): string {
  if (attraction.status === 'OPERATING' && attraction.capacityMode === 'REDUCED') {
    return 'Operating at reduced capacity';
  }

  if (attraction.status === 'CLOSED' && attraction.version > 0) {
    return 'Closed unexpectedly';
  }

  if (attraction.statusMessage) {
    return attraction.statusMessage;
  }

  return formatStatus(attraction.status);
}

export function matchesOverviewFilter(
  attraction: AttractionSummary,
  filter: OverviewFilter,
): boolean {
  switch (filter) {
    case 'all':
      return true;
    case 'attention':
      return needsAttention(attraction);
    case 'operating':
      return attraction.status === 'OPERATING';
    case 'holds':
      return isHoldStatus(attraction.status);
    case 'closed':
      return attraction.status === 'CLOSED';
    case 'reduced':
      return attraction.capacityMode === 'REDUCED';
  }
}

function compareByName(a: AttractionSummary, b: AttractionSummary): number {
  return a.name.localeCompare(b.name);
}

function compareByRecent(a: AttractionSummary, b: AttractionSummary): number {
  return Date.parse(b.updatedAt) - Date.parse(a.updatedAt) || compareByName(a, b);
}

function compareByWait(a: AttractionSummary, b: AttractionSummary): number {
  const waitA = a.waitMinutes;
  const waitB = b.waitMinutes;
  if (waitA === null && waitB === null) {
    return compareByName(a, b);
  }
  if (waitA === null) {
    return 1;
  }
  if (waitB === null) {
    return -1;
  }
  return waitB - waitA || compareByName(a, b);
}

function compareBySeverity(a: AttractionSummary, b: AttractionSummary): number {
  return attentionSeverity(a) - attentionSeverity(b) || compareByRecent(a, b);
}

export function sortAttractions(
  attractions: AttractionSummary[],
  sort: OverviewSort,
): AttractionSummary[] {
  const sorted = [...attractions];
  switch (sort) {
    case 'severity':
      sorted.sort(compareBySeverity);
      break;
    case 'recent':
      sorted.sort(compareByRecent);
      break;
    case 'wait':
      sorted.sort(compareByWait);
      break;
    case 'name':
      sorted.sort(compareByName);
      break;
  }
  return sorted;
}

export function presentAttractions(
  attractions: AttractionSummary[],
  filter: OverviewFilter,
  sort: OverviewSort,
): AttractionSummary[] {
  return sortAttractions(
    attractions.filter((attraction) => matchesOverviewFilter(attraction, filter)),
    sort,
  );
}

export function attentionQueue(attractions: AttractionSummary[]): AttractionSummary[] {
  return sortAttractions(attractions.filter(needsAttention), 'severity');
}

export type ShiftSummary = {
  operatingCount: number;
  attentionCount: number;
  reducedCount: number;
  averageWaitMinutes: number | null;
};

export function shiftSummary(attractions: AttractionSummary[]): ShiftSummary {
  const operating = attractions.filter((attraction) => attraction.status === 'OPERATING');
  const waits = operating
    .map((attraction) => attraction.waitMinutes)
    .filter((wait): wait is number => wait !== null);
  const averageWaitMinutes =
    waits.length === 0
      ? null
      : Math.round(waits.reduce((total, wait) => total + wait, 0) / waits.length);

  return {
    operatingCount: operating.length,
    attentionCount: attractions.filter(needsAttention).length,
    reducedCount: attractions.filter((attraction) => attraction.capacityMode === 'REDUCED')
      .length,
    averageWaitMinutes,
  };
}

export function formatAverageWait(minutes: number | null): string {
  return formatWaitTime(minutes);
}
