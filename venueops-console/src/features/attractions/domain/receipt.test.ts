import { describe, expect, it } from 'vitest';

import { mangroveRun, toOperatorAttraction } from '@/test/fixtures';

import { commandReceiptSummary } from './receipt';

describe('commandReceiptSummary', () => {
  it('describes status, capacity, and wait-time commands', () => {
    const hold = {
      ...toOperatorAttraction(mangroveRun),
      status: 'WEATHER_HOLD' as const,
      version: 8,
    };
    expect(commandReceiptSummary(hold, 'PLACE_WEATHER_HOLD')).toBe(
      'Mangrove Run updated to Weather Hold · version 8',
    );
    expect(
      commandReceiptSummary(
        { ...toOperatorAttraction(mangroveRun), capacityMode: 'REDUCED', version: 3 },
        'REDUCE_CAPACITY',
      ),
    ).toBe('Mangrove Run updated to Reduced capacity · version 3');
    expect(
      commandReceiptSummary(
        { ...toOperatorAttraction(mangroveRun), waitMinutes: 32, version: 1 },
        'UPDATE_WAIT_TIME',
      ),
    ).toBe('Mangrove Run wait time set to 32 min · version 1');
  });
});
