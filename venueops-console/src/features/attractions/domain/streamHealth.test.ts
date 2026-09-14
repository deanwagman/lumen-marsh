import { describe, expect, it } from 'vitest';

import { defaultStreamHealth, reduceStreamHealth, streamStatusLabel } from './streamHealth';

describe('reduceStreamHealth', () => {
  it('starts connecting and becomes live on open', () => {
    const connecting = reduceStreamHealth(defaultStreamHealth, { type: 'connect' });
    expect(connecting.status).toBe('connecting');
    expect(reduceStreamHealth(connecting, { type: 'open' }).status).toBe('live');
  });

  it('records the last successful snapshot time from a message', () => {
    const live = reduceStreamHealth(defaultStreamHealth, { type: 'message', at: 1_700_000_000_000 });
    expect(live).toEqual({ status: 'live', lastEventAt: 1_700_000_000_000 });
  });

  it('moves through reconnecting then stale without dropping lastEventAt', () => {
    const live = reduceStreamHealth(defaultStreamHealth, { type: 'message', at: 42 });
    const reconnecting = reduceStreamHealth(live, { type: 'error' });
    expect(reconnecting).toEqual({ status: 'reconnecting', lastEventAt: 42 });
    expect(reduceStreamHealth(reconnecting, { type: 'stale' })).toEqual({
      status: 'stale',
      lastEventAt: 42,
    });
    expect(streamStatusLabel('stale')).toBe('Showing last-known conditions');
  });

  it('labels a manual reconnect as reconnecting when a snapshot was already seen', () => {
    const live = reduceStreamHealth(defaultStreamHealth, { type: 'message', at: 42 });
    expect(reduceStreamHealth(live, { type: 'connect' }).status).toBe('reconnecting');
  });
});
