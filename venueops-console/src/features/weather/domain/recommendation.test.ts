import { describe, expect, it } from 'vitest';

import { formatDataAge } from './formatters';
import { canHandleRecommendation, incidentPrefill } from './recommendation';
import { lightningHoldRecommendation } from '@/test/fixtures';

describe('weather recommendation helpers', () => {
  it('prefills a weather incident from a warning recommendation', () => {
    expect(incidentPrefill(lightningHoldRecommendation)).toEqual({
      title: lightningHoldRecommendation.summary,
      type: 'WEATHER',
      severity: 'MAJOR',
      internalDescription: `${lightningHoldRecommendation.evidence}\n\nRecommended action: ${lightningHoldRecommendation.recommendedAction}`,
      attractionIds: ['mangrove-run', 'cypress-coil'],
    });
  });

  it('stops operator actions once the source has cleared', () => {
    expect(canHandleRecommendation(lightningHoldRecommendation)).toBe(true);
    expect(
      canHandleRecommendation({
        ...lightningHoldRecommendation,
        status: 'CLEARED',
        sourceVersion: 2,
      }),
    ).toBe(false);
  });

  it('formats data age from the source observation time', () => {
    expect(formatDataAge('2026-09-01T16:00:00Z', Date.parse('2026-09-01T16:04:00Z'))).toBe(
      '4 min ago',
    );
  });
});
