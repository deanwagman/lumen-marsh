import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { venueOpsScopes, type AuthSession } from '@/auth/session';
import { draftCorrectiveWorkOrder } from '@/features/maintenance/test/fixtures';
import { renderApp } from '@/test/renderApp';
import { maintenanceRecommendationStore, maintenanceWorkOrderStore } from '@/test/maintenanceStore';

const readOnlySession: AuthSession = {
  accessToken: 'reader',
  operatorId: 'reader-1',
  displayName: 'Read Only',
  role: 'operator',
  scopes: [venueOpsScopes.operatorRead, venueOpsScopes.maintenanceRead],
};

describe('MaintenancePage', () => {
  it('shows Maintenance in the console rail with a pending badge', async () => {
    renderApp({ initialEntries: ['/maintenance'] });
    const nav = await screen.findByRole('navigation', { name: 'Console' });
    expect(within(nav).getByRole('link', { name: /Maintenance/ })).toHaveAttribute('aria-current', 'page');
    expect(await screen.findByLabelText('1 pending maintenance items')).toBeInTheDocument();
  });

  it('renders the Cypress Coil vibration recommendation and empty work-order list', async () => {
    const user = userEvent.setup();
    renderApp({ initialEntries: ['/maintenance'] });
    expect(await screen.findByRole('heading', { name: 'Maintenance' })).toBeInTheDocument();
    await user.click(screen.getByRole('tab', { name: 'Reliability inbox' }));
    expect(await screen.findByRole('heading', { name: 'CC-TRAIN-01-WHEEL-A' })).toBeInTheDocument();
    expect(screen.getByText(/Simulated vibration peak/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Accept recommendation' })).toBeInTheDocument();
    expect(screen.getByText('Pending recommendations').closest('a')).toHaveTextContent('1');
  });

  it('hides recommendation commands for read-only users', async () => {
    renderApp({ initialEntries: ['/maintenance?section=inbox'], session: readOnlySession });
    expect(await screen.findByRole('heading', { name: 'CC-TRAIN-01-WHEEL-A' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Accept recommendation' })).not.toBeInTheDocument();
    expect(screen.getByText(/Read-only/i)).toBeInTheDocument();
  });

  it('accepts a recommendation and exposes the created work order', async () => {
    const user = userEvent.setup();
    renderApp({ initialEntries: ['/maintenance?section=inbox'] });
    expect(await screen.findByRole('button', { name: 'Accept recommendation' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Accept recommendation' }));
    await user.click(screen.getByRole('button', { name: 'Create work order' }));
    expect(await screen.findByRole('link', { name: 'Open work order' })).toBeInTheDocument();
    expect(maintenanceWorkOrderStore[0]?.priority).toBe('P1');
    expect(maintenanceRecommendationStore[0]?.status).toBe('WORK_ORDER_CREATED');
  });

  it('lists P1 work orders in the priority queue', async () => {
    maintenanceWorkOrderStore.push(
      draftCorrectiveWorkOrder({ status: 'IN_PROGRESS', assignedTeam: 'Ride Systems' }),
    );
    renderApp({ initialEntries: ['/maintenance'] });
    expect(await screen.findAllByRole('link', { name: /LM-2026-0001/i })).not.toHaveLength(0);
    expect(screen.getByText('Complete required checklist items')).toBeInTheDocument();
  });

  it('shows an empty overview when nothing is pending', async () => {
    maintenanceRecommendationStore.splice(0);
    renderApp({ initialEntries: ['/maintenance'] });
    expect(await screen.findByRole('heading', { name: 'Maintenance' })).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.queryByText('Loading maintenance overview')).not.toBeInTheDocument();
      expect(screen.getByText('No active maintenance items require attention.')).toBeInTheDocument();
    });
  });

  it('retries the work-order list after a failure', async () => {
    const user = userEvent.setup();
    const { http, HttpResponse } = await import('msw');
    const { server } = await import('@/test/server');
    server.use(
      http.get('/api/v1/operator/maintenance/work-orders', () =>
        HttpResponse.json({ title: 'Unavailable', status: 500, detail: 'Maintenance list failed' }, { status: 500 }),
      ),
    );
    renderApp({ initialEntries: ['/maintenance'] });
    expect(await screen.findByText('Unable to load the maintenance workspace.')).toBeInTheDocument();
    server.resetHandlers();
    await user.click(screen.getAllByRole('button', { name: 'Retry' })[0]);
    await waitFor(() => {
      expect(screen.queryByText('Unable to load the maintenance workspace.')).not.toBeInTheDocument();
    });
  });

  it('pages work orders with Next and Previous', async () => {
    const user = userEvent.setup();
    maintenanceWorkOrderStore.push(
      draftCorrectiveWorkOrder({ status: 'IN_PROGRESS', assignedTeam: 'Ride Systems' }),
      draftCorrectiveWorkOrder({
        id: 'd2222222-2222-4222-8222-222222222222',
        workOrderNumber: 'LM-2026-0002',
        status: 'IN_PROGRESS',
        priority: 'P2',
      }),
    );
    renderApp({ initialEntries: ['/maintenance?size=1'] });
    expect(await screen.findByText(/Showing 1–1 of 2/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled();
    const table = screen.getByRole('table', { name: 'Maintenance work orders' });
    expect(within(table).getByText('LM-2026-0001')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Next' }));
    expect(await screen.findByText(/Showing 2–2 of 2/)).toBeInTheDocument();
    expect(within(screen.getByRole('table', { name: 'Maintenance work orders' })).getByText('LM-2026-0002')).toBeInTheDocument();
    expect(within(screen.getByRole('table', { name: 'Maintenance work orders' })).queryByText('LM-2026-0001')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Previous' }));
    expect(await screen.findByText(/Showing 1–1 of 2/)).toBeInTheDocument();
  });

  it('filters active work orders on the server before paging', async () => {
    maintenanceWorkOrderStore.push(
      draftCorrectiveWorkOrder({ status: 'COMPLETED', version: 11 }),
      draftCorrectiveWorkOrder({
        id: 'd2222222-2222-4222-8222-222222222222',
        workOrderNumber: 'LM-2026-0002',
        status: 'DRAFT',
        priority: 'P2',
      }),
    );
    renderApp({ initialEntries: ['/maintenance?size=1'] });
    const table = await screen.findByRole('table', { name: 'Maintenance work orders' });
    expect(within(table).getByText('LM-2026-0002')).toBeInTheDocument();
    expect(within(table).queryByText('LM-2026-0001')).not.toBeInTheDocument();
    expect(screen.getByText(/Showing 1–1 of 1/)).toBeInTheDocument();
  });
});
