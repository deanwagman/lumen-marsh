import { describe, expect, it } from 'vitest';

import { operatorDashboardSchema } from './dashboardSchema';
import { calmDashboard, stormDashboard } from '@/features/dashboard/test/fixtures';

describe('operatorDashboardSchema', () => {
  it('accepts the calm and storm fixtures', () => {
    expect(operatorDashboardSchema.parse(calmDashboard).generatedAt).toBe(calmDashboard.generatedAt);
    const storm = operatorDashboardSchema.parse(stormDashboard);
    expect(storm.openIncidents).toHaveLength(3);
    expect(storm.pendingWeatherRecommendations[0]?.action).toBe('PLACE_WEATHER_HOLD');
    expect(storm.activeWeatherWorkflow?.affectedAttractionIds).toEqual([
      'mangrove-run',
      'cypress-coil',
    ]);
  });
});
