import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { renderApp } from '@/test/renderApp';

describe('application bootstrap', () => {
  it('redirects unauthenticated operators to sign in', async () => {
    renderApp({ initialEntries: ['/attractions'], session: null });

    expect(await screen.findByRole('heading', { name: 'VenueOps sign in' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Shift overview' })).not.toBeInTheDocument();
  });

  it('renders the Lumen Marsh Control shell', async () => {
    renderApp({ initialEntries: ['/attractions'] });

    expect(screen.getByRole('link', { name: /lumen marsh/i })).toBeInTheDocument();
    expect(screen.getByText('Local Operator')).toBeInTheDocument();
    expect(await screen.findByRole('heading', { name: 'Shift overview' })).toBeInTheDocument();
  });

  it('redirects the root route to the operations dashboard', async () => {
    renderApp({ initialEntries: ['/'] });

    expect(await screen.findByRole('heading', { name: 'Operations dashboard' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Dashboard' })).toHaveAttribute('aria-current', 'page');
  });

  it('makes console navigation keyboard accessible', async () => {
    const user = userEvent.setup();
    renderApp({ initialEntries: ['/dashboard'] });

    await screen.findByRole('heading', { name: 'Operations dashboard' });

    const skipLink = screen.getByRole('link', { name: /skip to workspace/i });
    const nav = screen.getByRole('navigation', { name: 'Console' });
    const dashboardLink = screen.getByRole('link', { name: 'Dashboard' });

    expect(nav).toBeInTheDocument();
    expect(dashboardLink).not.toHaveAttribute('tabIndex', '-1');

    await user.tab();
    expect(skipLink).toHaveFocus();

    await user.tab();
    expect(screen.getByRole('link', { name: /lumen marsh/i })).toHaveFocus();

    await user.tab();
    expect(dashboardLink).toHaveFocus();

    await user.keyboard('{Enter}');
    expect(await screen.findByRole('heading', { name: 'Operations dashboard' })).toBeInTheDocument();
  });

  it('renders the fallback page for an unknown route', async () => {
    renderApp({ initialEntries: ['/definitely-missing'] });

    expect(await screen.findByRole('heading', { name: 'Page not found' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /go to attractions/i })).toBeInTheDocument();
  });
});

describe('attraction catalog', () => {
  it('renders a loading state while the catalog request is in flight', async () => {
    const { server } = await import('@/test/server');
    const { delay, http, HttpResponse } = await import('msw');

    server.use(
      http.get('/api/v1/attractions', async () => {
        await delay('infinite');
        return HttpResponse.json([]);
      }),
    );

    renderApp({ initialEntries: ['/attractions'] });

    expect(await screen.findByRole('status')).toHaveTextContent(/loading attractions/i);
  });

  it('renders attraction rows from a successful response', async () => {
    renderApp({ initialEntries: ['/attractions'] });

    expect(await screen.findByRole('link', { name: /mangrove run/i })).toBeInTheDocument();
    expect(screen.getByText('Luminous Wetlands')).toBeInTheDocument();
    expect(screen.getAllByText('Operating').length).toBeGreaterThan(0);
    expect(screen.getByText('Normal')).toBeInTheDocument();
    expect(screen.getAllByText('25 min').length).toBeGreaterThan(0);
    expect(screen.getAllByText('v0').length).toBeGreaterThan(0);
    expect(screen.getByRole('link', { name: /stormglass station/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /cypress coil/i })).toBeInTheDocument();
  });

  it('renders retry behavior after a network failure', async () => {
    const user = userEvent.setup();
    const { server } = await import('@/test/server');
    const { http, HttpResponse } = await import('msw');
    const { catalogAttractions } = await import('@/test/fixtures');

    let attempts = 0;
    server.use(
      http.get('/api/v1/attractions', () => {
        attempts += 1;
        if (attempts === 1) {
          return HttpResponse.error();
        }
        return HttpResponse.json(catalogAttractions);
      }),
    );

    renderApp({ initialEntries: ['/attractions'] });

    expect(await screen.findByRole('alert')).toHaveTextContent(/unable to load attractions/i);
    await user.click(screen.getByRole('button', { name: 'Retry' }));

    await waitFor(() => {
      expect(screen.getByRole('link', { name: /mangrove run/i })).toBeInTheDocument();
    });
  });
});
