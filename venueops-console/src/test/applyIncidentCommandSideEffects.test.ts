import { describe, expect, it } from 'vitest';

import { applyIncidentCommandSideEffects } from './applyIncidentCommandSideEffects';
import { lightningIncident } from './fixtures';

describe('applyIncidentCommandSideEffects', () => {
  it('publishes guest title and message', () => {
    const next = applyIncidentCommandSideEffects(lightningIncident, {
      type: 'PUBLISH_GUEST_ADVISORY',
      expectedVersion: 1,
      guestTitle: 'Weather advisory',
      guestMessage: 'Outdoor holds in effect.',
    });
    expect(next.guestAdvisoryPublished).toBe(true);
    expect(next.guestTitle).toBe('Weather advisory');
    expect(next.guestMessage).toBe('Outdoor holds in effect.');
    expect(next.version).toBe(lightningIncident.version + 1);
  });

  it('withdraws guest advisory copy', () => {
    const published = {
      ...lightningIncident,
      guestAdvisoryPublished: true,
      guestTitle: 'Weather advisory',
      guestMessage: 'Paused.',
      version: 2,
    };
    const next = applyIncidentCommandSideEffects(published, {
      type: 'WITHDRAW_GUEST_ADVISORY',
      expectedVersion: 2,
      reason: 'Cleared',
    });
    expect(next.guestAdvisoryPublished).toBe(false);
    expect(next.guestTitle).toBeNull();
    expect(next.guestMessage).toBeNull();
  });

  it('clears advisory when resolving', () => {
    const published = {
      ...lightningIncident,
      guestAdvisoryPublished: true,
      guestTitle: 'Weather advisory',
      guestMessage: 'Paused.',
      status: 'MITIGATING' as const,
      version: 3,
    };
    const next = applyIncidentCommandSideEffects(published, {
      type: 'RESOLVE',
      expectedVersion: 3,
      reason: 'Complete',
    });
    expect(next.status).toBe('RESOLVED');
    expect(next.guestAdvisoryPublished).toBe(false);
    expect(next.guestTitle).toBeNull();
  });
});
