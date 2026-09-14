import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse, delay } from 'msw';
import { describe, expect, it, vi } from 'vitest';

import { handleAttractionStreamEvent } from '@/features/attractions/api/AttractionEventSource';
import { setStreamHealth } from '@/features/attractions/api/AttractionQueries';
import { DashboardQueries } from '@/features/dashboard/api/DashboardQueries';
import { catalogAttractions } from '@/test/fixtures';
import { stormDashboard } from '@/features/dashboard/test/fixtures';
import { renderApp } from '@/test/renderApp';
import { server } from '@/test/server';

function renderDashboard() {
  server.use(http.get('/api/v1/operator/dashboard', () => HttpResponse.json(stormDashboard)));
  return renderApp({ initialEntries: ['/dashboard'] });
}

describe('Operations dashboard', () => {
  it('renders a loading state while the snapshot is in flight', async () => {
    server.use(
      http.get('/api/v1/operator/dashboard', async () => {
        await delay('infinite');
        return HttpResponse.json(stormDashboard);
      }),
    );

    renderApp({ initialEntries: ['/dashboard'] });

    expect(await screen.findByRole('status')).toHaveTextContent(/loading operations dashboard/i);
  });

  it('renders the calm empty states from the default snapshot', async () => {
    renderApp({ initialEntries: ['/dashboard'] });

    expect(await screen.findByText('All attractions are operating normally.')).toBeInTheDocument();
    expect(screen.getByText('Nothing currently requires attention.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /1 attraction operating/i })).toHaveAttribute(
      'href',
      '/attractions?filter=operating',
    );
  });

  it('orders needs-attention items and links each to a workspace', async () => {
    renderDashboard();

    const region = await screen.findByRole('region', { name: 'Needs attention' });
    const items = within(region).getAllByRole('link');
    expect(items.map((item) => item.getAttribute('href'))).toEqual([
      '/incidents/inc-critical-1',
      '/incidents/inc-lightning-1',
      '/attractions',
      '/attractions/cypress-coil',
      '/incidents/inc-queue-1?assignment=unassigned',
    ]);
    expect(within(region).getByText(/critical incident is open/i)).toBeInTheDocument();
    expect(within(region).getByText(/cypress coil is on weather hold/i)).toBeInTheDocument();
  });

  it('navigates summary metrics to filtered workspaces', async () => {
    const user = userEvent.setup();
    renderDashboard();

    await user.click(await screen.findByRole('link', { name: /1 attraction on weather hold/i }));

    expect(await screen.findByRole('heading', { name: 'Shift overview' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Holds' })).toHaveAttribute('aria-pressed', 'true');
  });

  it('links open incidents and abnormal attractions to their workspaces', async () => {
    renderDashboard();
    const incidents = await screen.findByRole('region', { name: 'Active incidents' });
    expect(
      within(incidents).getByRole('link', { name: /lightning near western basin/i }),
    ).toHaveAttribute('href', '/incidents/inc-lightning-1');
    const attractions = screen.getByRole('region', { name: 'Attraction conditions' });
    expect(within(attractions).getByRole('link', { name: /cypress coil/i })).toHaveAttribute(
      'href',
      '/attractions/cypress-coil',
    );
  });

  it('shows guest advisory copy without internal incident content', async () => {
    renderDashboard();

    const advisories = await screen.findByRole('region', { name: 'Published guest advisories' });
    expect(within(advisories).getByText('Weather conditions affecting outdoor attractions')).toBeInTheDocument();
    expect(within(advisories).getByText('Some outdoor attractions are temporarily paused.')).toBeInTheDocument();
    expect(screen.queryByText(/internal only/i)).not.toBeInTheDocument();
    expect(within(advisories).queryByText('operator@lumen-marsh.dev')).not.toBeInTheDocument();
  });

  it('keeps the last snapshot visible while reconnecting or stale', async () => {
    const { queryClient } = renderDashboard();
    expect(await screen.findByRole('heading', { name: 'Needs attention' })).toBeInTheDocument();

    setStreamHealth(queryClient, { type: 'error' });
    expect(await screen.findByText('Live updates interrupted. Reconnecting…')).toBeInTheDocument();
    expect(screen.getAllByText('Lightning near western basin').length).toBeGreaterThan(0);

    setStreamHealth(queryClient, { type: 'stale' });
    expect(await screen.findByText(/some operational information may be out of date/i)).toBeInTheDocument();
    expect(screen.getAllByText('Lightning near western basin').length).toBeGreaterThan(0);
  });

  it('shows unavailable state with retry and workspace links when no snapshot exists', async () => {
    const user = userEvent.setup();
    let attempts = 0;
    server.use(
      http.get('/api/v1/operator/dashboard', () => {
        attempts += 1;
        if (attempts === 1) {
          return HttpResponse.error();
        }
        return HttpResponse.json(stormDashboard);
      }),
    );

    renderApp({ initialEntries: ['/dashboard'] });

    expect(await screen.findByRole('alert')).toHaveTextContent(/unable to load the operations dashboard/i);
    expect(screen.queryByText('0')).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Open attractions' })).toHaveAttribute('href', '/attractions');
    await user.click(screen.getByRole('button', { name: 'Retry' }));
    expect(await screen.findByRole('heading', { name: 'Needs attention' })).toBeInTheDocument();
  });

  it('preserves current content during a background refresh', async () => {
    let requests = 0;
    server.use(
      http.get('/api/v1/operator/dashboard', async () => {
        requests += 1;
        if (requests > 1) {
          await delay(250);
        }
        return HttpResponse.json(stormDashboard);
      }),
    );

    const { queryClient } = renderApp({ initialEntries: ['/dashboard'] });
    expect(await screen.findAllByText('Lightning near western basin')).not.toHaveLength(0);

    handleAttractionStreamEvent(
      queryClient,
      'incident.updated',
      JSON.stringify({
        eventId: 'activity-incident-1',
        eventType: 'INCIDENT_UPDATED',
        occurredAt: '2026-09-10T20:31:00Z',
        incident: {
          id: 'inc-lightning-1',
          title: 'Lightning near western basin',
          type: 'WEATHER',
          severity: 'MAJOR',
          status: 'MITIGATING',
          assignedTo: 'Control Tower',
          guestAdvisoryPublished: true,
          guestTitle: 'Weather conditions affecting outdoor attractions',
          guestMessage: 'Some outdoor attractions are temporarily paused.',
          affectedAttractionIds: ['mangrove-run', 'cypress-coil'],
          updatedAt: '2026-09-10T20:31:00Z',
          version: 5,
        },
      }),
    );

    await waitFor(() => {
      expect(requests).toBeGreaterThan(1);
    });
    expect(screen.getByRole('heading', { name: 'Needs attention' })).toBeInTheDocument();
    expect(screen.getAllByText('Lightning near western basin').length).toBeGreaterThan(0);
  });

  it('does not replace a newer snapshot with an older one', async () => {
    let generatedAt = '2026-09-10T20:30:00Z';
    server.use(
      http.get('/api/v1/operator/dashboard', () =>
        HttpResponse.json({
          ...stormDashboard,
          generatedAt,
          summary: {
            ...stormDashboard.summary,
            openIncidents: generatedAt === '2026-09-10T20:31:00Z' ? 4 : 3,
          },
        }),
      ),
    );

    const { queryClient } = renderApp({ initialEntries: ['/dashboard'] });
    expect(await screen.findByRole('link', { name: /3 open incidents/i })).toBeInTheDocument();

    generatedAt = '2026-09-10T20:31:00Z';
    await queryClient.invalidateQueries({ queryKey: DashboardQueries.snapshot() });
    expect(await screen.findByRole('link', { name: /4 open incidents/i })).toBeInTheDocument();

    generatedAt = '2026-09-10T20:29:00Z';
    await queryClient.invalidateQueries({ queryKey: DashboardQueries.snapshot() });
    await waitFor(() => {
      expect(screen.getByRole('link', { name: /4 open incidents/i })).toBeInTheDocument();
    });
  });

  it('fetches a fresh snapshot after the live stream reconnects', async () => {
    let requests = 0;
    server.use(
      http.get('/api/v1/operator/dashboard', () => {
        requests += 1;
        return HttpResponse.json(stormDashboard);
      }),
    );

    const { queryClient } = renderApp({ initialEntries: ['/dashboard'] });
    await screen.findByRole('heading', { name: 'Needs attention' });
    const initialRequests = requests;

    setStreamHealth(queryClient, { type: 'error' });
    expect(await screen.findByText('Live updates interrupted. Reconnecting…')).toBeInTheDocument();
    setStreamHealth(queryClient, { type: 'message', at: Date.now() });

    await waitFor(() => {
      expect(requests).toBeGreaterThan(initialRequests);
    });
  });

  it('does not expose supervisor command controls on the dashboard', async () => {
    renderDashboard();
    await screen.findByRole('button', { name: 'Generate shift handoff' });
    expect(screen.queryByRole('button', { name: /publish guest advisory/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /report incident/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /place weather hold/i })).not.toBeInTheDocument();
  });

  it('generates a copyable and printable shift handoff summary', async () => {
    const user = userEvent.setup();
    const writeText = vi.fn().mockResolvedValue(undefined);
    const print = vi.fn();
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: { writeText },
    });
    window.print = print;

    renderDashboard();
    await user.click(await screen.findByRole('button', { name: 'Generate shift handoff' }));

    const dialog = await screen.findByRole('dialog', { name: 'Shift handoff summary' });
    expect(within(dialog).getByText(/lightning near western basin/i)).toBeInTheDocument();
    expect(within(dialog).getByText(/weather conditions affecting outdoor attractions/i)).toBeInTheDocument();
    expect(within(dialog).queryByText(/internal only/i)).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Copy summary' }));
    await waitFor(() => {
      expect(writeText).toHaveBeenCalled();
    });
    expect(String(writeText.mock.calls[0]?.[0])).toContain('Lumen Marsh Shift Handoff');

    await user.click(screen.getByRole('button', { name: 'Print summary' }));
    expect(print).toHaveBeenCalled();

    await user.click(screen.getByRole('button', { name: 'Close' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('supports keyboard access for the shift-handoff dialog', async () => {
    const user = userEvent.setup();
    renderDashboard();
    await user.click(await screen.findByRole('button', { name: 'Generate shift handoff' }));
    expect(await screen.findByRole('dialog')).toBeInTheDocument();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});

describe('dashboard catalog lookup', () => {
  it('uses attraction names when the catalog is available', async () => {
    server.use(
      http.get('/api/v1/operator/dashboard', () => HttpResponse.json(stormDashboard)),
      http.get('/api/v1/attractions', () => HttpResponse.json(catalogAttractions)),
    );
    renderApp({ initialEntries: ['/dashboard'] });
    expect(await screen.findAllByText(/affected: mangrove run, cypress coil/i)).not.toHaveLength(0);
  });
});
