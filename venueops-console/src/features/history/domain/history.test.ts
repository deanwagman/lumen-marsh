import { describe, expect, it } from 'vitest';

import {
  availabilityClassifications,
  classificationSymbols,
} from '@/features/history/domain/history';

describe('availability classification symbols', () => {
  it('gives every status a distinct non-color symbol', () => {
    const symbols = availabilityClassifications.map(
      (classification) => classificationSymbols[classification],
    );
    expect(new Set(symbols).size).toBe(symbols.length);
    expect(symbols.every((symbol) => symbol.trim().length > 0)).toBe(true);
  });
});
