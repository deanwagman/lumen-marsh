import { screen, waitFor, within, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';

import { localDevelopmentSession, venueOpsScopes } from '@/auth/session';
import { AttractionQueries } from '@/features/attractions/api/AttractionQueries';
import { draftCorrectiveWorkOrder } from '@/features/maintenance/test/fixtures';
import { cypressCoil, toOperatorAttraction } from '@/test/fixtures';
import { renderApp } from '@/test/renderApp';
import { maintenanceWorkOrderStore } from '@/test/maintenanceStore';
import { server } from '@/test/server';

describe('MaintenanceWorkOrderPage', () => {
  it('renders checklist, timeline, and supervisor inspection actions', async () => {
    const workOrder = draftCorrectiveWorkOrder({
      status: 'AWAITING_INSPECTION',
      version: 6,
      checklist: draftCorrectiveWorkOrder().checklist.map((item) => ({
        ...item,
        result: 'PASSED',
        completedByDisplayName: 'Tech',
        completedAt: '2026-09-14T18:35:00Z',
      })),
    });
    maintenanceWorkOrderStore.push(workOrder);
    renderApp({ initialEntries: [`/maintenance/work-orders/${workOrder.id}`] });

    expect(await screen.findByText(workOrder.workOrderNumber)).toBeInTheDocument();
    expect(screen.getByText('Inspect wheel assembly')).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: 'Approve inspection' }).length).toBeGreaterThan(0);
  });

  it('does not offer inspection approval to operators', async () => {
    const workOrder = draftCorrectiveWorkOrder({ status: 'AWAITING_INSPECTION', version: 6 });
    maintenanceWorkOrderStore.push(workOrder);
    renderApp({
      initialEntries: [`/maintenance/work-orders/${workOrder.id}`],
      session: {
        ...localDevelopmentSession,
        role: 'operator',
        scopes: [venueOpsScopes.maintenanceRead, venueOpsScopes.maintenanceCommand],
      },
    });
    expect(await screen.findByText(workOrder.workOrderNumber)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Approve inspection' })).not.toBeInTheDocument();
    expect(screen.getByText(/Supervisor approval/i)).toBeInTheDocument();
  });

  it('shows a stale-version warning and does not resubmit automatically', async () => {
    const user = userEvent.setup();
    const workOrder = draftCorrectiveWorkOrder({ status: 'DRAFT', version: 1 });
    maintenanceWorkOrderStore.push(workOrder);
    const { queryClient } = renderApp({
      initialEntries: [`/maintenance/work-orders/${workOrder.id}`],
    });
    expect(await screen.findAllByRole('button', { name: 'Open work order' })).not.toHaveLength(0);
    maintenanceWorkOrderStore[0] = { ...workOrder, version: 4, status: 'OPEN' };
    await user.click(screen.getAllByRole('button', { name: 'Open work order' })[0]);
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Open work order' }));
    expect(
      await screen.findAllByText(/This work order changed while you were reviewing it/i),
    ).not.toHaveLength(0);
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(queryClient.isMutating()).toBe(0);
  });

  it('keeps the command dialog and reason after a command 403', async () => {
    const user = userEvent.setup();
    const workOrder = draftCorrectiveWorkOrder({ status: 'DRAFT', version: 1 });
    maintenanceWorkOrderStore.push(workOrder);
    server.use(
      http.post('/api/v1/operator/maintenance/work-orders/:id/commands', () =>
        HttpResponse.json({ title: 'Forbidden', status: 403, detail: 'Need inspect' }, { status: 403 }),
      ),
    );
    renderApp({ initialEntries: [`/maintenance/work-orders/${workOrder.id}`] });
    await user.click((await screen.findAllByRole('button', { name: 'Open work order' }))[0]);
    const dialog = screen.getByRole('dialog');
    await user.type(within(dialog).getByLabelText(/Reason \(optional\)/i), 'Ready to start.');
    await user.click(within(dialog).getByRole('button', { name: 'Open work order' }));
    expect(await screen.findAllByText(/Form entries were kept/i)).not.toHaveLength(0);
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(within(screen.getByRole('dialog')).getByLabelText(/Reason \(optional\)/i)).toHaveValue(
      'Ready to start.',
    );
    expect(screen.getByText(workOrder.workOrderNumber)).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Access denied' })).not.toBeInTheDocument();
  });

  it('does not submit a newer attraction version than the operator reviewed', async () => {
    const user = userEvent.setup();
    const workOrder = draftCorrectiveWorkOrder({
      status: 'READY_FOR_TESTING',
      version: 8,
      recommendedAttractionAction: {
        command: 'START_TESTING',
        reason: 'Work order LM-2026-0001 is ready for operational testing.',
      },
    });
    maintenanceWorkOrderStore.push(workOrder);
    const posted = vi.fn();
    server.use(
      http.post('/api/v1/operator/attractions/cypress-coil/commands', async ({ request }) => {
        posted(await request.json());
        return HttpResponse.json(toOperatorAttraction({ ...cypressCoil, status: 'TESTING', version: 1 }));
      }),
    );
    const { queryClient } = renderApp({ initialEntries: [`/maintenance/work-orders/${workOrder.id}`] });
    await user.click(await screen.findByRole('button', { name: 'Start attraction testing' }));
    await act(async () => {
      queryClient.setQueryData(
        AttractionQueries.detail('cypress-coil'),
        toOperatorAttraction({ ...cypressCoil, version: 9 }),
      );
    });
    expect(await screen.findByText(/changed while you were reviewing it/i)).toBeInTheDocument();
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Start testing' }));
    expect(posted).not.toHaveBeenCalled();
  });

  it('keeps the user on the page for a prerequisite failure', async () => {
    const user = userEvent.setup();
    const workOrder = draftCorrectiveWorkOrder({ status: 'IN_PROGRESS', version: 4 });
    maintenanceWorkOrderStore.push(workOrder);
    renderApp({ initialEntries: [`/maintenance/work-orders/${workOrder.id}`] });
    expect((await screen.findAllByRole('button', { name: 'Request inspection' }))[0]).toBeDisabled();
    expect(screen.getByText(/Complete required checklist items first/i)).toBeInTheDocument();
    await user.click(screen.getAllByRole('button', { name: 'Save checklist result' })[0]);
    expect(screen.getByText(workOrder.workOrderNumber)).toBeInTheDocument();
  });

  it('renders an attraction testing handoff when ready for testing', async () => {
    const workOrder = draftCorrectiveWorkOrder({
      status: 'READY_FOR_TESTING',
      version: 8,
      recommendedAttractionAction: {
        command: 'START_TESTING',
        reason: 'Work order LM-2026-0001 is ready for operational testing.',
      },
    });
    maintenanceWorkOrderStore.push(workOrder);
    renderApp({ initialEntries: [`/maintenance/work-orders/${workOrder.id}`] });
    expect(await screen.findByRole('heading', { name: 'Ready for operational testing' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Start attraction testing' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Cypress Coil workspace/i })).toHaveAttribute(
      'href',
      '/attractions/cypress-coil',
    );
  });

  it('does not offer start testing when the attraction is already testing', async () => {
    const workOrder = draftCorrectiveWorkOrder({
      status: 'READY_FOR_TESTING',
      version: 8,
      recommendedAttractionAction: {
        command: 'START_TESTING',
        reason: 'Work order LM-2026-0001 is ready for operational testing.',
      },
    });
    maintenanceWorkOrderStore.push(workOrder);
    server.use(
      http.get('/api/v1/operator/attractions/cypress-coil', () =>
        HttpResponse.json(toOperatorAttraction({ ...cypressCoil, status: 'TESTING' })),
      ),
    );
    renderApp({ initialEntries: [`/maintenance/work-orders/${workOrder.id}`] });
    expect(
      await screen.findByText(/Complete testing, then approve return to service/i),
    ).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Start attraction testing' })).not.toBeInTheDocument();
  });

  it('hides the attraction handoff after the work order is completed', async () => {
    const workOrder = draftCorrectiveWorkOrder({
      status: 'COMPLETED',
      version: 11,
      recommendedAttractionAction: {
        command: 'START_TESTING',
        reason: 'Work order LM-2026-0001 is ready for operational testing.',
      },
    });
    maintenanceWorkOrderStore.push(workOrder);
    renderApp({ initialEntries: [`/maintenance/work-orders/${workOrder.id}`] });
    expect(await screen.findByText(workOrder.workOrderNumber)).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Ready for operational testing' })).not.toBeInTheDocument();
  });

  it('does not prefix command dialogs with Request', async () => {
    const user = userEvent.setup();
    const workOrder = draftCorrectiveWorkOrder({ status: 'DRAFT', version: 1 });
    maintenanceWorkOrderStore.push(workOrder);
    renderApp({ initialEntries: [`/maintenance/work-orders/${workOrder.id}`] });
    await user.click((await screen.findAllByRole('button', { name: 'Open work order' }))[0]);
    expect(screen.getByRole('dialog')).toHaveTextContent(`Open work order for ${workOrder.workOrderNumber}.`);
    expect(screen.getByRole('dialog')).not.toHaveTextContent(/Request open work order/i);
  });

  it('clears the note form after the server accepts it', async () => {
    const user = userEvent.setup();
    const workOrder = draftCorrectiveWorkOrder({ status: 'IN_PROGRESS', version: 4 });
    maintenanceWorkOrderStore.push(workOrder);
    renderApp({ initialEntries: [`/maintenance/work-orders/${workOrder.id}`] });
    const note = await screen.findByLabelText(/Add a timestamped note/i);
    await user.type(note, 'Wheel fasteners retightened.');
    await user.click(screen.getByRole('button', { name: 'Add note' }));
    await waitFor(() => {
      expect(screen.getByLabelText(/Add a timestamped note/i)).toHaveValue('');
    });
    expect(await screen.findByText('Note added')).toBeInTheDocument();
  });
});
