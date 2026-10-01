import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { mangroveRun, mangroveWaitTimeActivity, toOperatorAttraction } from '@/test/fixtures';
import { renderApp } from '@/test/renderApp';
import { server } from '@/test/server';

describe('Mangrove Run command workspace', () => {
  it('shows current state, valid operating commands, and wait-time controls', async () => {
    renderApp({ initialEntries: ['/attractions/mangrove-run'] });

    expect(await screen.findByRole('heading', { name: 'Mangrove Run' })).toBeInTheDocument();
    expect(screen.getByText('Command workspace')).toBeInTheDocument();
    expect(screen.getByText('Luminous Wetlands · Boat Expedition')).toBeInTheDocument();
    expect(screen.getByText('25 min')).toBeInTheDocument();
    expect(screen.getByText('v0')).toBeInTheDocument();

    expect(screen.getByRole('button', { name: 'Place weather hold' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Report technical fault' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Close for day' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Reduce capacity' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Start testing' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Update wait time' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Activity' })).toBeInTheDocument();
    expect(screen.getByText('No operator activity yet.')).toBeInTheDocument();
  });

  it('does not offer wait-time or weather-hold commands when the attraction is closed', async () => {
    renderApp({ initialEntries: ['/attractions/stormglass-station'] });

    expect(await screen.findByRole('heading', { name: 'Stormglass Station' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Start testing' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Place weather hold' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Update wait time' })).not.toBeInTheDocument();
  });

  it('updates wait time and refreshes current state', async () => {
    const user = userEvent.setup();
    const updated = {
      ...toOperatorAttraction(mangroveRun),
      waitMinutes: 35,
      version: 1,
      updatedAt: '2026-08-28T16:05:00Z',
    };
    let posted = false;

    server.use(
      http.post('/api/v1/operator/attractions/mangrove-run/commands', async ({ request }) => {
        expect(request.headers.get('Authorization')).toBe('Bearer local-development-token');
        expect(request.headers.has('X-Actor')).toBe(false);
        const body = (await request.json()) as {
          commandId: string;
          type: string;
          expectedVersion: number;
          data?: { waitMinutes?: number };
        };
        expect(body.commandId).toEqual(expect.any(String));
        expect(body.type).toBe('UPDATE_WAIT_TIME');
        expect(body.expectedVersion).toBe(0);
        expect(body.data).toEqual({ waitMinutes: 35 });
        posted = true;
        return HttpResponse.json(updated);
      }),
      http.get('/api/v1/operator/attractions/mangrove-run/activity', () =>
        HttpResponse.json(posted ? [mangroveWaitTimeActivity] : []),
      ),
    );

    renderApp({ initialEntries: ['/attractions/mangrove-run'] });
    await screen.findByRole('heading', { name: 'Mangrove Run' });

    const waitInput = screen.getByLabelText('Minutes');
    await user.clear(waitInput);
    await user.type(waitInput, '35');
    await user.click(screen.getByRole('button', { name: 'Update wait time' }));

    await waitFor(() => {
      expect(screen.getByText('35 min')).toBeInTheDocument();
      expect(screen.getByText('v1')).toBeInTheDocument();
    });
    expect(
      await screen.findByText(/wait time 25 min → 35 min/i),
    ).toBeInTheDocument();
    expect(screen.getByRole('status', { name: 'Command accepted' })).toHaveTextContent(
      'Mangrove Run wait time set to 35 min · version 1',
    );
    expect(screen.getByRole('link', { name: 'View activity' })).toHaveAttribute(
      'href',
      '/attractions/mangrove-run#activity-activity-wait-1',
    );
  });

  it('shows version conflict recovery when expectedVersion is stale', async () => {
    const user = userEvent.setup();
    const current = {
      ...toOperatorAttraction(mangroveRun),
      version: 3,
      waitMinutes: 40,
      updatedAt: '2026-08-28T16:10:00Z',
    };
    let stale = true;

    server.use(
      http.post('/api/v1/operator/attractions/mangrove-run/commands', () => {
        stale = false;
        return HttpResponse.json(
          {
            title: 'Stale attraction version',
            status: 409,
            detail: 'Attraction mangrove-run is at version 3',
            code: 'STALE_VERSION',
            expectedVersion: 0,
            actualVersion: 3,
          },
          { status: 409 },
        );
      }),
      http.get('/api/v1/operator/attractions/mangrove-run', () =>
        HttpResponse.json(stale ? toOperatorAttraction(mangroveRun) : current),
      ),
    );

    renderApp({ initialEntries: ['/attractions/mangrove-run'] });
    await screen.findByRole('heading', { name: 'Mangrove Run' });

    await user.click(screen.getByRole('button', { name: 'Place weather hold' }));
    await user.type(screen.getByLabelText(/reason/i), 'Lightning nearby');
    await user.click(screen.getByRole('button', { name: 'Confirm command' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/version conflict/i);
    expect(screen.getByRole('alert')).toHaveTextContent('v0');
    expect(screen.getByRole('alert')).toHaveTextContent('v3');

    await user.click(screen.getByRole('button', { name: 'Reload current state' }));

    await waitFor(() => {
      expect(screen.getByText('v3')).toBeInTheDocument();
      expect(screen.getByText('40 min')).toBeInTheDocument();
    });
  });

  it('renders the activity timeline from operator history', async () => {
    server.use(
      http.get('/api/v1/operator/attractions/mangrove-run/activity', () =>
        HttpResponse.json([mangroveWaitTimeActivity]),
      ),
    );

    renderApp({ initialEntries: ['/attractions/mangrove-run'] });

    const timeline = await screen.findByRole('list');
    expect(within(timeline).getByText(/wait time 25 min → 35 min/i)).toBeInTheDocument();
    expect(within(timeline).getByText(/control-tower/i)).toBeInTheDocument();
  });
});
