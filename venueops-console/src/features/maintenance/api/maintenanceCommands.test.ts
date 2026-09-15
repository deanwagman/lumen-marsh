import { QueryClient } from '@tanstack/react-query';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { handleAttractionStreamEvent } from '@/features/attractions/api/AttractionEventSource';
import { AttractionQueries } from '@/features/attractions/api/AttractionQueries';
import { MaintenanceQueries } from '@/features/maintenance/api/MaintenanceQueries';
import { catalogAttractions } from '@/test/fixtures';
import { draftCorrectiveWorkOrder } from '@/features/maintenance/test/fixtures';
import { maintenanceWorkOrderStore } from '@/test/maintenanceStore';
import { server } from '@/test/server';
import { MaintenanceCommands } from '@/features/maintenance/api/MaintenanceCommands';
import { ApiClient } from '@/shared/api/client';

describe('maintenance commands', () => {
  it('sends the current version and reuses a command id on retry', async () => {
    const workOrder = draftCorrectiveWorkOrder({ status: 'DRAFT', version: 3 });
    maintenanceWorkOrderStore.push(workOrder);
    const commandIds: string[] = [];
    const versions: number[] = [];
    server.use(
      http.post('/api/v1/operator/maintenance/work-orders/:id/commands', async ({ request }) => {
        const body = (await request.json()) as { commandId: string; expectedVersion: number };
        commandIds.push(body.commandId);
        versions.push(body.expectedVersion);
        if (commandIds.length === 1) {
          return HttpResponse.error();
        }
        return HttpResponse.json({
          workOrder: {
            id: workOrder.id,
            workOrderNumber: workOrder.workOrderNumber,
            status: 'OPEN',
            priority: 'P1',
            version: 4,
            updatedAt: '2026-09-14T18:40:00Z',
          },
          activity: {
            eventType: 'WORK_ORDER_OPENED',
            actorDisplayName: 'Local Operator',
            occurredAt: '2026-09-14T18:40:00Z',
            resultingVersion: 4,
            details: {},
          },
          replay: commandIds.length > 2,
        });
      }),
    );

    const client = new ApiClient({ baseUrl: '' });
    const commandId = 'aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee';
    await expect(
      MaintenanceCommands.executeWorkOrder(client, {
        workOrderId: workOrder.id,
        commandId,
        type: 'OPEN',
        expectedVersion: 3,
      }),
    ).rejects.toThrow();
    const retry = await MaintenanceCommands.executeWorkOrder(client, {
      workOrderId: workOrder.id,
      commandId,
      type: 'OPEN',
      expectedVersion: 3,
    });
    expect(commandIds[0]).toBe(commandId);
    expect(commandIds[1]).toBe(commandId);
    expect(versions).toEqual([3, 3]);
    expect(retry.workOrder.status).toBe('OPEN');
  });
});

describe('maintenance SSE cache', () => {
  it('replaces summaries from a snapshot and ignores older versions', () => {
    const queryClient = new QueryClient();
    const workOrder = draftCorrectiveWorkOrder({ version: 2, status: 'OPEN' });
    queryClient.setQueryData(MaintenanceQueries.workOrders({ page: 0, size: 50 }), {
      items: [workOrder],
      page: 0,
      size: 50,
      total: 1,
    });
    queryClient.setQueryData(AttractionQueries.list(), catalogAttractions);

    handleAttractionStreamEvent(
      queryClient,
      'maintenance.work-orders.snapshot',
      JSON.stringify([
        {
          id: workOrder.id,
          workOrderNumber: workOrder.workOrderNumber,
          status: 'ASSIGNED',
          priority: 'P1',
          version: 3,
          updatedAt: '2026-09-14T18:50:00Z',
        },
      ]),
    );

    expect(queryClient.getQueryData(MaintenanceQueries.summaries())).toEqual([
      expect.objectContaining({ status: 'ASSIGNED', version: 3 }),
    ]);

    handleAttractionStreamEvent(
      queryClient,
      'maintenance.work-order.updated',
      JSON.stringify({
        eventId: 'evt-old',
        eventType: 'WORK_STARTED',
        aggregateId: workOrder.id,
        aggregateVersion: 2,
        occurredAt: '2026-09-14T18:41:00Z',
        workOrder: {
          id: workOrder.id,
          workOrderNumber: workOrder.workOrderNumber,
          status: 'IN_PROGRESS',
          priority: 'P1',
          version: 2,
          updatedAt: '2026-09-14T18:41:00Z',
        },
      }),
    );

    expect(queryClient.getQueryData(MaintenanceQueries.summaries())).toEqual([
      expect.objectContaining({ status: 'ASSIGNED', version: 3 }),
    ]);

    handleAttractionStreamEvent(
      queryClient,
      'maintenance.work-order.updated',
      JSON.stringify({
        eventId: 'evt-new',
        eventType: 'WORK_STARTED',
        aggregateId: workOrder.id,
        aggregateVersion: 4,
        occurredAt: '2026-09-14T18:55:00Z',
        workOrder: {
          id: workOrder.id,
          workOrderNumber: workOrder.workOrderNumber,
          status: 'IN_PROGRESS',
          priority: 'P1',
          version: 4,
          updatedAt: '2026-09-14T18:55:00Z',
        },
      }),
    );

    expect(queryClient.getQueryData(MaintenanceQueries.summaries())).toEqual([
      expect.objectContaining({ status: 'IN_PROGRESS', version: 4 }),
    ]);
    expect(queryClient.getQueryData(AttractionQueries.list())).toEqual(catalogAttractions);
    expect(queryClient.getQueryData(['guest', 'events'])).toBeUndefined();
  });
});
