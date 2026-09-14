import { describe, expect, it } from 'vitest';

import {
  compareIncidents,
  filterIncidents,
  canPublishAdvisory,
  canResolve,
  type Incident,
  defaultIncidentFilter,
} from './incident';
import {
  formatActivityDetails,
  formatActivityType,
  resolveAttractionLabels,
} from './formatters';
import { localDevelopmentSession } from '@/auth/session';

const reported: Incident = {
  id: 'inc-1',
  title: 'Lightning',
  type: 'WEATHER',
  severity: 'MAJOR',
  status: 'REPORTED',
  internalDescription: null,
  assignedTo: null,
  guestAdvisoryPublished: false,
  guestTitle: null,
  guestMessage: null,
  attractionIds: ['mangrove-run'],
  createdAt: '2026-09-01T16:00:00Z',
  updatedAt: '2026-09-01T16:10:00Z',
  version: 1,
};

describe('incident ordering and filtering', () => {
  it('orders open incidents before resolved, then by severity', () => {
    const resolved = {
      ...reported,
      id: 'inc-2',
      status: 'RESOLVED' as const,
      severity: 'CRITICAL' as const,
    };
    const minor = {
      ...reported,
      id: 'inc-3',
      severity: 'MINOR' as const,
      updatedAt: '2026-09-01T16:20:00Z',
    };
    expect([resolved, minor, reported].sort(compareIncidents).map((item) => item.id)).toEqual([
      'inc-1',
      'inc-3',
      'inc-2',
    ]);
  });

  it('orders by updated time then stable id', () => {
    const older = { ...reported, id: 'inc-b', updatedAt: '2026-09-01T16:00:00Z' };
    const newer = { ...reported, id: 'inc-a', updatedAt: '2026-09-01T17:00:00Z' };
    const sameTimeEarlierId = {
      ...reported,
      id: 'inc-a',
      updatedAt: '2026-09-01T16:00:00Z',
    };
    const sameTimeLaterId = {
      ...reported,
      id: 'inc-b',
      updatedAt: '2026-09-01T16:00:00Z',
    };

    expect([older, newer].sort(compareIncidents).map((item) => item.id)).toEqual([
      'inc-a',
      'inc-b',
    ]);
    expect(
      [sameTimeLaterId, sameTimeEarlierId].sort(compareIncidents).map((item) => item.id),
    ).toEqual(['inc-a', 'inc-b']);
  });

  it('filters by open status and assignment', () => {
    const assigned = { ...reported, id: 'inc-assigned', assignedTo: 'Basin team' };
    const visible = filterIncidents([reported, assigned], {
      ...defaultIncidentFilter,
      assignment: 'unassigned',
    });
    expect(visible.map((item) => item.id)).toEqual(['inc-1']);
  });

  it('filters by severity, type, and status independently', () => {
    const technical = {
      ...reported,
      id: 'inc-tech',
      type: 'TECHNICAL' as const,
      severity: 'MINOR' as const,
      status: 'ACKNOWLEDGED' as const,
    };

    expect(
      filterIncidents([reported, technical], {
        ...defaultIncidentFilter,
        severity: 'MAJOR',
      }).map((item) => item.id),
    ).toEqual(['inc-1']);

    expect(
      filterIncidents([reported, technical], {
        ...defaultIncidentFilter,
        type: 'TECHNICAL',
      }).map((item) => item.id),
    ).toEqual(['inc-tech']);

    expect(
      filterIncidents([reported, technical], {
        ...defaultIncidentFilter,
        status: 'ACKNOWLEDGED',
      }).map((item) => item.id),
    ).toEqual(['inc-tech']);
  });

  it('applies combined filters', () => {
    const matching = {
      ...reported,
      id: 'inc-match',
      type: 'WEATHER' as const,
      severity: 'MAJOR' as const,
      assignedTo: null,
    };
    const filteredOut = {
      ...reported,
      id: 'inc-out',
      type: 'WEATHER' as const,
      severity: 'MAJOR' as const,
      assignedTo: 'Ops',
    };

    expect(
      filterIncidents([matching, filteredOut], {
        status: 'open',
        severity: 'MAJOR',
        type: 'WEATHER',
        assignment: 'unassigned',
        attractionId: 'all',
      }).map((item) => item.id),
    ).toEqual(['inc-match']);
  });

  it('falls back to raw attraction ids when names are unknown', () => {
    expect(resolveAttractionLabels(['mangrove-run', 'missing-id'], { 'mangrove-run': 'Mangrove Run' })).toEqual([
      'Mangrove Run',
      'missing-id',
    ]);
  });

  it('formats activity labels from the Phase 1 table', () => {
    expect(formatActivityType('INCIDENT_REPORTED')).toBe('Incident reported');
    expect(formatActivityType('GUEST_ADVISORY_PUBLISHED')).toBe('Guest advisory published');
  });

  it('formats event-specific activity details', () => {
    expect(
      formatActivityDetails(
        {
          id: 'a1',
          incidentId: 'inc-1',
          type: 'INCIDENT_REPORTED',
          actor: 'ops',
          reason: null,
          occurredAt: '2026-09-01T16:00:00Z',
          previousVersion: 0,
          resultingVersion: 1,
          incidentType: 'WEATHER',
          severity: 'MAJOR',
          attractionIds: ['mangrove-run', 'unknown'],
        },
        new Map([['mangrove-run', 'Mangrove Run']]),
      ),
    ).toEqual(['Weather · Major', 'Mangrove Run, unknown']);
  });

  it('requires supervisor role and advisory scope to publish', () => {
    expect(canPublishAdvisory(localDevelopmentSession)).toBe(true);
    expect(
      canPublishAdvisory({
        ...localDevelopmentSession,
        role: 'operator',
        scopes: localDevelopmentSession.scopes.filter(
          (scope) => scope !== 'venueops/advisories.publish',
        ),
      }),
    ).toBe(false);
  });

  it('blocks operators from resolving major incidents', () => {
    const operator = { ...localDevelopmentSession, role: 'operator' as const };
    expect(canResolve(operator, 'MAJOR')).toBe(false);
    expect(canResolve(operator, 'MINOR')).toBe(true);
    expect(canResolve(localDevelopmentSession, 'CRITICAL')).toBe(true);
  });
});
