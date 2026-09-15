import { describe, expect, it } from 'vitest';

import { localDevelopmentSession, venueOpsScopes } from '@/auth/session';
import {
  attractionHandoffOwnershipMessage,
  availableMaintenanceActions,
  canStartAttractionTesting,
  nextActionLabel,
} from '@/features/maintenance/domain/maintenanceActions';
import {
  checklistProgress,
  compareWorkOrdersOperational,
  maintenanceNavBadgeCount,
  queueRank,
  summarizeMaintenance,
} from '@/features/maintenance/domain/maintenance';
import {
  formatEstimatedRestore,
  formatObservationAge,
} from '@/features/maintenance/domain/maintenancePresentation';
import { draftCorrectiveWorkOrder, vibrationRecommendation } from '@/features/maintenance/test/fixtures';
import { cypressCoil } from '@/test/fixtures';

describe('maintenance domain', () => {
  it('orders P1 active work ahead of P2 and recommendations', () => {
    const p1 = draftCorrectiveWorkOrder({ status: 'IN_PROGRESS', workOrderNumber: 'LM-2026-0001' });
    const p2 = draftCorrectiveWorkOrder({
      id: 'wo-p2',
      status: 'IN_PROGRESS',
      priority: 'P2',
      workOrderNumber: 'LM-2026-0002',
    });
    const testing = draftCorrectiveWorkOrder({
      id: 'wo-test',
      status: 'READY_FOR_TESTING',
      workOrderNumber: 'LM-2026-0003',
    });
    expect(queueRank({ kind: 'work-order', workOrder: p1 })).toBe(1);
    expect(queueRank({ kind: 'work-order', workOrder: p2 })).toBe(2);
    expect(queueRank({ kind: 'work-order', workOrder: testing })).toBe(3);
    expect(queueRank({ kind: 'recommendation', recommendation: vibrationRecommendation })).toBe(5);
    expect(compareWorkOrdersOperational(p1, p2)).toBeLessThan(0);
  });

  it('summarizes checklist completion and nav badges', () => {
    const workOrder = draftCorrectiveWorkOrder();
    expect(checklistProgress(workOrder.checklist)).toEqual({ resolved: 0, total: 2 });
    workOrder.checklist[0].result = 'PASSED';
    expect(checklistProgress(workOrder.checklist).resolved).toBe(1);
    expect(
      maintenanceNavBadgeCount({
        workOrders: [workOrder],
        recommendations: [vibrationRecommendation],
      }),
    ).toBe(2);
    expect(
      summarizeMaintenance({
        workOrders: [workOrder],
        recommendations: [vibrationRecommendation],
        assets: [],
      }).pendingRecommendations,
    ).toBe(1);
  });

  it('formats data age and estimated restoration', () => {
    const now = Date.parse('2026-09-14T18:30:00Z');
    expect(formatObservationAge('2026-09-14T18:25:00Z', now)).toBe('5 min ago');
    expect(formatEstimatedRestore('2026-09-14T19:00:00Z', now)).toBe('in 30 min');
  });

  it('offers lifecycle actions by status, scope, and role', () => {
    const draft = draftCorrectiveWorkOrder();
    const operator = {
      ...localDevelopmentSession,
      role: 'operator' as const,
      scopes: [venueOpsScopes.maintenanceRead, venueOpsScopes.maintenanceCommand],
    };
    expect(availableMaintenanceActions({ workOrder: draft, session: operator }).map((item) => item.type)).toEqual(
      expect.arrayContaining(['OPEN']),
    );
    expect(availableMaintenanceActions({ workOrder: draft, session: operator }).map((item) => item.type)).not.toContain(
      'CANCEL',
    );

    const awaiting = draftCorrectiveWorkOrder({ status: 'AWAITING_INSPECTION', version: 6 });
    expect(availableMaintenanceActions({ workOrder: awaiting, session: operator }).map((item) => item.type)).not.toContain(
      'APPROVE_INSPECTION',
    );
    expect(
      availableMaintenanceActions({ workOrder: awaiting, session: localDevelopmentSession }).map((item) => item.type),
    ).toContain('APPROVE_INSPECTION');
    expect(nextActionLabel(awaiting)).toBe('Supervisor inspection approval');
  });

  it('presents START_TESTING only when the attraction can actually start testing', () => {
    const ready = draftCorrectiveWorkOrder({
      status: 'READY_FOR_TESTING',
      recommendedAttractionAction: {
        command: 'START_TESTING',
        reason: 'Work order LM-2026-0001 is ready for operational testing.',
      },
    });
    expect(
      canStartAttractionTesting({
        workOrder: ready,
        session: localDevelopmentSession,
        attraction: cypressCoil,
      }),
    ).toBe(true);
    expect(
      canStartAttractionTesting({
        workOrder: ready,
        session: { ...localDevelopmentSession, role: 'operator' },
        attraction: cypressCoil,
      }),
    ).toBe(false);
    expect(
      canStartAttractionTesting({
        workOrder: ready,
        session: localDevelopmentSession,
        attraction: { ...cypressCoil, status: 'TESTING' },
      }),
    ).toBe(false);
    expect(nextActionLabel(ready, { ...cypressCoil, status: 'TESTING' })).toBe(
      'Wait for Operations to finish testing',
    );
    expect(nextActionLabel(ready, { ...cypressCoil, status: 'OPERATING' })).toBe(
      'Complete the work order',
    );
    expect(attractionHandoffOwnershipMessage({ ...cypressCoil, status: 'TESTING' })).toMatch(
      /Complete testing, then approve return to service/i,
    );
    expect(attractionHandoffOwnershipMessage({ ...cypressCoil, status: 'OPERATING' })).toMatch(
      /returned the attraction to service/i,
    );
  });
});
