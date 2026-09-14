import { describe, expect, it } from 'vitest';

import { statusCommands, validCommands } from './commands';

describe('validCommands', () => {
  it('offers operating commands for Mangrove Run seed state', () => {
    expect(validCommands('OPERATING', 'NORMAL')).toEqual([
      'PLACE_WEATHER_HOLD',
      'REPORT_TECHNICAL_FAULT',
      'CLOSE_FOR_DAY',
      'REDUCE_CAPACITY',
      'UPDATE_WAIT_TIME',
    ]);
    expect(statusCommands('OPERATING', 'NORMAL')).not.toContain('UPDATE_WAIT_TIME');
  });

  it('offers start testing when closed', () => {
    expect(validCommands('CLOSED', 'NOT_APPLICABLE')).toEqual(['START_TESTING']);
  });

  it('offers restore capacity when operating reduced', () => {
    expect(validCommands('OPERATING', 'REDUCED')).toContain('RESTORE_CAPACITY');
    expect(validCommands('OPERATING', 'REDUCED')).not.toContain('REDUCE_CAPACITY');
  });

  it('returns weather hold to testing, not directly to operating', () => {
    expect(validCommands('WEATHER_HOLD', 'NOT_APPLICABLE')).toEqual([
      'CLEAR_WEATHER_HOLD',
      'CLOSE_FOR_DAY',
    ]);
  });
});
