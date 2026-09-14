import { describe, expect, it } from 'vitest';

import { catalogAttractions, mangroveRun, stormglassStation } from '@/test/fixtures';

import {
  attentionQueue,
  attentionReason,
  matchesOverviewFilter,
  needsAttention,
  parseOverviewFilter,
  parseOverviewSort,
  presentAttractions,
  shiftSummary,
} from './overview';

const weatherHold = {
  ...mangroveRun,
  status: 'WEATHER_HOLD' as const,
  waitMinutes: null,
  statusMessage: 'Temporarily unavailable due to nearby weather.',
  version: 8,
  updatedAt: '2026-09-01T15:00:00Z',
};

const reduced = {
  ...mangroveRun,
  capacityMode: 'REDUCED' as const,
  version: 3,
  updatedAt: '2026-09-01T14:50:00Z',
};

const unexpectedClose = {
  ...mangroveRun,
  status: 'CLOSED' as const,
  capacityMode: 'NOT_APPLICABLE' as const,
  waitMinutes: null,
  version: 4,
  updatedAt: '2026-09-01T14:40:00Z',
};

const testing = {
  ...stormglassStation,
  status: 'TESTING' as const,
  version: 1,
  updatedAt: '2026-09-01T14:30:00Z',
};

describe('shift overview domain', () => {
  it('treats holds, testing, reduced capacity, and commanded closes as attention', () => {
    expect(needsAttention(mangroveRun)).toBe(false);
    expect(needsAttention(stormglassStation)).toBe(false);
    expect(needsAttention(weatherHold)).toBe(true);
    expect(needsAttention(reduced)).toBe(true);
    expect(needsAttention(unexpectedClose)).toBe(true);
    expect(needsAttention(testing)).toBe(true);
  });

  it('summarizes operating, attention, reduced, and average wait from the catalog', () => {
    expect(shiftSummary([mangroveRun, stormglassStation, reduced])).toEqual({
      operatingCount: 2,
      attentionCount: 1,
      reducedCount: 1,
      averageWaitMinutes: 25,
    });
  });

  it('sorts the attention queue by severity then recency', () => {
    const technical = {
      ...testing,
      id: 'coil',
      name: 'Cypress Coil',
      status: 'TECHNICAL_DELAY' as const,
      updatedAt: '2026-09-01T13:00:00Z',
    };
    const queue = attentionQueue([reduced, weatherHold, technical]);
    expect(queue.map((item) => item.status)).toEqual([
      'TECHNICAL_DELAY',
      'WEATHER_HOLD',
      'OPERATING',
    ]);
    expect(attentionReason(reduced)).toBe('Operating at reduced capacity');
    expect(attentionReason(unexpectedClose)).toBe('Closed unexpectedly');
  });

  it('filters and sorts for bookmarkable overview views', () => {
    const catalog = [...catalogAttractions, weatherHold, reduced];
    expect(presentAttractions(catalog, 'holds', 'name').map((item) => item.id)).toEqual([
      'mangrove-run',
    ]);
    expect(presentAttractions(catalog, 'reduced', 'wait')[0]?.capacityMode).toBe('REDUCED');
    expect(parseOverviewFilter('attention')).toBe('attention');
    expect(parseOverviewFilter('nope')).toBe('all');
    expect(parseOverviewSort('recent')).toBe('recent');
    expect(matchesOverviewFilter(stormglassStation, 'closed')).toBe(true);
    expect(presentAttractions(catalogAttractions, 'all', 'severity')[0]?.id).toBe(
      'mangrove-run',
    );
  });
});
