import { describe, expect, it } from 'vitest';

import {
  maintenanceActivitySchema,
  maintenanceSseUpdateSchema,
  maintenanceWorkOrderPageSchema,
  maintenanceWorkOrderSchema,
  maintenanceWorkOrderSnapshotListSchema,
  reliabilityRecommendationSchema,
} from '@/features/maintenance/api/maintenanceSchemas';
import { draftCorrectiveWorkOrder, vibrationRecommendation } from '@/features/maintenance/test/fixtures';

describe('maintenance schemas', () => {
  it('parses a work order with nullable assignment and empty collections', () => {
    const parsed = maintenanceWorkOrderSchema.parse({
      ...draftCorrectiveWorkOrder(),
      assignedTeam: null,
      assignedActorSubject: null,
      incident: null,
      estimatedRestoreAt: null,
      checklist: [],
      evidence: [],
    });
    expect(parsed.incident).toBeNull();
    expect(parsed.assignedTeam).toBeNull();
    expect(parsed.checklist).toEqual([]);
    expect(parsed.evidence).toEqual([]);
  });

  it('rejects unknown status and priority values', () => {
    expect(() =>
      maintenanceWorkOrderSchema.parse({ ...draftCorrectiveWorkOrder(), status: 'OPENED' }),
    ).toThrow();
    expect(() =>
      maintenanceWorkOrderSchema.parse({ ...draftCorrectiveWorkOrder(), priority: 'P0' }),
    ).toThrow();
  });

  it('parses paginated work-order responses', () => {
    const parsed = maintenanceWorkOrderPageSchema.parse({
      items: [draftCorrectiveWorkOrder()],
      page: 0,
      size: 50,
      total: 1,
    });
    expect(parsed.total).toBe(1);
    expect(parsed.items[0].workOrderNumber).toBe('LM-2026-0001');
  });

  it('parses a recommendation with nullable measurement fields', () => {
    const parsed = reliabilityRecommendationSchema.parse({
      ...vibrationRecommendation,
      value: null,
      unit: null,
      workOrderId: null,
    });
    expect(parsed.value).toBeNull();
    expect(parsed.workOrderId).toBeNull();
  });

  it('parses SSE snapshots and updates', () => {
    const snapshots = maintenanceWorkOrderSnapshotListSchema.parse([
      {
        id: 'd1111111-1111-4111-8111-111111111111',
        workOrderNumber: 'LM-2026-0001',
        status: 'OPEN',
        priority: 'P1',
        version: 2,
        updatedAt: '2026-09-14T18:40:00Z',
      },
    ]);
    expect(snapshots[0].status).toBe('OPEN');

    const update = maintenanceSseUpdateSchema.parse({
      eventId: 'evt-1',
      eventType: 'WORK_ORDER_OPENED',
      aggregateType: 'MAINTENANCE_WORK_ORDER',
      aggregateId: snapshots[0].id,
      aggregateVersion: 2,
      attractionId: 'cypress-coil',
      incidentId: null,
      actorSubject: 'local-operator',
      actorDisplayName: 'Local Operator',
      correlationId: 'corr-1',
      occurredAt: '2026-09-14T18:40:00Z',
      workOrder: snapshots[0],
    });
    expect(update.workOrder.version).toBe(2);
  });

  it('parses activity with correlation details', () => {
    const parsed = maintenanceActivitySchema.parse({
      id: 'act-1',
      sequence: 1,
      eventType: 'NOTE_ADDED',
      fromStatus: 'IN_PROGRESS',
      toStatus: 'IN_PROGRESS',
      actorSubject: 'local-operator',
      actorDisplayName: 'Local Operator',
      reason: 'Inspection in progress',
      details: { note: 'Bearing play observed' },
      commandId: '11111111-1111-4111-8111-111111111111',
      correlationId: 'corr-1',
      occurredAt: '2026-09-14T18:40:00Z',
      resultingVersion: 8,
    });
    expect(parsed.reason).toBe('Inspection in progress');
  });
});
