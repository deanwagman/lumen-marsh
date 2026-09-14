import { describe, expect, it } from 'vitest';

import { operatorDashboardSchema } from './dashboardSchema';
import { calmDashboard, stormDashboard } from '@/features/dashboard/test/fixtures';

describe('operatorDashboardSchema', () => {
  it('accepts the calm and storm fixtures', () => {
    expect(operatorDashboardSchema.parse(calmDashboard).generatedAt).toBe(calmDashboard.generatedAt);
    expect(operatorDashboardSchema.parse(stormDashboard).openIncidents).toHaveLength(3);
  });
});
