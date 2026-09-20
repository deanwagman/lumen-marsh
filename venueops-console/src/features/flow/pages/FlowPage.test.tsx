import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { venueOpsScopes, type AuthSession } from '@/auth/session';
import { flowRecommendationStore } from '@/test/flowStore';
import { renderApp } from '@/test/renderApp';

const readOnlySession: AuthSession = {
  accessToken: 'reader',
  operatorId: 'reader-1',
  displayName: 'Read Only',
  role: 'operator',
  scopes: [venueOpsScopes.operatorRead, venueOpsScopes.flowRead],
};

const operatorSession: AuthSession = {
  accessToken: 'operator',
  operatorId: 'operator-1',
  displayName: 'Park Operator',
  role: 'operator',
  scopes: [
    venueOpsScopes.operatorRead,
    venueOpsScopes.flowRead,
    venueOpsScopes.flowCommand,
  ],
};

describe('FlowPage', () => {
  it('shows Park Flow in the console rail with a pending badge', async () => {
    renderApp({ initialEntries: ['/park-flow'] });
    const nav = await screen.findByRole('navigation', { name: 'Console' });
    expect(within(nav).getByRole('link', { name: /Park Flow/ })).toHaveAttribute('aria-current', 'page');
    expect(await screen.findByLabelText('1 pending flow recommendations')).toBeInTheDocument();
  });

  it('renders overview metrics, forecast text, and the safety statement', async () => {
    renderApp({ initialEntries: ['/park-flow'] });
    expect(await screen.findByRole('heading', { name: 'Park Flow' })).toBeInTheDocument();
    expect(screen.getByText(/do not change attraction state or capacity automatically/i)).toBeInTheDocument();
    expect(await screen.findByText('Guests in queues')).toBeInTheDocument();
    expect(screen.getByText('355')).toBeInTheDocument();
    expect(
      screen.getByRole('img', { name: /Mangrove Run: 25 minutes posted, 20 minutes calculated/i }),
    ).toBeInTheDocument();
  });

  it('hides review commands for read-only users', async () => {
    renderApp({ initialEntries: ['/park-flow'], session: readOnlySession });
    expect(await screen.findByRole('heading', { name: /Mangrove Run capacity is down/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Approve' })).not.toBeInTheDocument();
  });

  it('hides publish for operators after approval is the only remaining supervisor action', async () => {
    flowRecommendationStore[0] = { ...flowRecommendationStore[0]!, status: 'APPROVED', version: 2 };
    renderApp({ initialEntries: ['/park-flow'], session: operatorSession });
    expect(await screen.findByText(/Publishing guest guidance requires a supervisor/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Publish' })).not.toBeInTheDocument();
  });

  it('approves a pending recommendation', async () => {
    const user = userEvent.setup();
    renderApp({ initialEntries: ['/park-flow'] });
    await user.click(await screen.findByRole('button', { name: 'Approve' }));
    const dialog = await screen.findByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: 'Approve' }));
    await waitFor(() => {
      expect(flowRecommendationStore[0]?.status).toBe('APPROVED');
    });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('blocks a stale confirmation and keeps the form', async () => {
    const user = userEvent.setup();
    renderApp({ initialEntries: ['/park-flow'] });
    await user.click(await screen.findByRole('button', { name: 'Dismiss' }));
    const dialog = await screen.findByRole('dialog');
    const reason = within(dialog).getByLabelText('Reason');
    await user.type(reason, 'Queue recovered');
    flowRecommendationStore[0] = { ...flowRecommendationStore[0]!, version: 9 };
    await user.click(within(dialog).getByRole('button', { name: 'Dismiss' }));
    expect(await screen.findByText(/This recommendation changed since it was last read/i)).toBeInTheDocument();
    expect(within(dialog).getByLabelText('Reason')).toHaveValue('Queue recovered');
    expect(flowRecommendationStore[0]?.status).toBe('PENDING_REVIEW');
  });

  it('keeps the dialog open after a forbidden command', async () => {
    const user = userEvent.setup();
    const { http, HttpResponse } = await import('msw');
    const { server } = await import('@/test/server');
    server.use(
      http.post('/api/v1/operator/flow/recommendations/:id/commands', () =>
        HttpResponse.json(
          { title: 'Forbidden', status: 403, detail: 'Publishing requires a supervisor.' },
          { status: 403 },
        ),
      ),
    );
    renderApp({ initialEntries: ['/park-flow'] });
    await user.click(await screen.findByRole('button', { name: 'Approve' }));
    const dialog = await screen.findByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: 'Approve' }));
    expect(await screen.findByRole('dialog')).toBeInTheDocument();
    expect(within(dialog).getByRole('alert')).toBeInTheDocument();
  });
});
