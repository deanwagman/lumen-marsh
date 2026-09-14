import { describe, expect, it } from 'vitest';

import { localDevelopmentSession, venueOpsScopes } from '@/auth/session';

import { availableIncidentActions } from './commands';
import type { Incident } from './incident';

const incident: Incident = {
  id: 'inc-1',
  title: 'Lightning',
  type: 'WEATHER',
  severity: 'MINOR',
  status: 'REPORTED',
  internalDescription: null,
  assignedTo: null,
  guestAdvisoryPublished: false,
  guestTitle: null,
  guestMessage: null,
  attractionIds: [],
  createdAt: '2026-09-01T16:00:00Z',
  updatedAt: '2026-09-01T16:00:00Z',
  version: 1,
};

describe('availableIncidentActions', () => {
  it('offers acknowledge but not resolve while reported', () => {
    const types = availableIncidentActions(localDevelopmentSession, incident).map((action) => action.type);
    expect(types).toContain('ACKNOWLEDGE');
    expect(types).not.toContain('RESOLVE');
    expect(types).not.toContain('START_MITIGATION');
  });

  it('hides advisory publish from operators', () => {
    const operator = { ...localDevelopmentSession, role: 'operator' as const };
    const types = availableIncidentActions(operator, incident).map((action) => action.type);
    expect(types).not.toContain('PUBLISH_GUEST_ADVISORY');
  });

  it('offers advisory actions independently from incident command permission', () => {
    const advisorySupervisor = {
      ...localDevelopmentSession,
      scopes: [venueOpsScopes.operatorRead, venueOpsScopes.advisoriesPublish],
    };
    const types = availableIncidentActions(advisorySupervisor, incident).map(
      (action) => action.type,
    );

    expect(types).toContain('PUBLISH_GUEST_ADVISORY');
    expect(types).not.toContain('ACKNOWLEDGE');
    expect(types).not.toContain('ASSIGN');
  });
});
