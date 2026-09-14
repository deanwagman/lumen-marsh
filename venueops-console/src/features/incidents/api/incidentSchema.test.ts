import { describe, expect, it } from 'vitest';

import {
  incidentActivitySchema,
  incidentListSchema,
  incidentSchema,
} from '@/features/incidents/api/incidentSchema';
import { lightningIncident, lightningIncidentActivity } from '@/test/fixtures';

describe('incident schemas', () => {
  it('parses a valid incident response', () => {
    const parsed = incidentSchema.parse(lightningIncident);
    expect(parsed.id).toBe(lightningIncident.id);
    expect(parsed.assignedTo).toBeNull();
    expect(parsed.attractionIds).toEqual(['mangrove-run']);
  });

  it('parses nullable fields and defaults missing attraction arrays', () => {
    const parsed = incidentSchema.parse({
      ...lightningIncident,
      internalDescription: null,
      assignedTo: null,
      guestTitle: null,
      guestMessage: null,
      createdAt: null,
      attractionIds: undefined,
    });
    expect(parsed.internalDescription).toBeNull();
    expect(parsed.createdAt).toBeNull();
    expect(parsed.attractionIds).toEqual([]);
  });

  it('rejects invalid status, type, or severity', () => {
    expect(() =>
      incidentSchema.parse({ ...lightningIncident, status: 'OPEN' }),
    ).toThrow();
    expect(() =>
      incidentSchema.parse({ ...lightningIncident, type: 'STORM' }),
    ).toThrow();
    expect(() =>
      incidentSchema.parse({ ...lightningIncident, severity: 'HIGH' }),
    ).toThrow();
  });

  it('parses incident lists', () => {
    expect(incidentListSchema.parse([lightningIncident])).toHaveLength(1);
  });

  it('parses activity variants and rejects missing required fields', () => {
    const reported = incidentActivitySchema.parse({
      ...lightningIncidentActivity,
      incidentType: 'WEATHER',
      severity: 'MAJOR',
      attractionIds: ['mangrove-run'],
    });
    expect(reported.type).toBe('INCIDENT_REPORTED');
    expect(reported.severity).toBe('MAJOR');

    const severityChanged = incidentActivitySchema.parse({
      id: 'act-2',
      incidentId: 'inc-lightning-1',
      type: 'SEVERITY_CHANGED',
      actor: 'supervisor-1',
      reason: 'Escalation',
      occurredAt: '2026-09-01T16:20:00Z',
      previousVersion: 1,
      resultingVersion: 2,
      previousSeverity: 'MAJOR',
      newSeverity: 'CRITICAL',
    });
    expect(severityChanged.previousSeverity).toBe('MAJOR');
    expect(severityChanged.newSeverity).toBe('CRITICAL');

    expect(() =>
      incidentActivitySchema.parse({
        id: 'act-bad',
        type: 'INCIDENT_REPORTED',
      }),
    ).toThrow();
  });
});
