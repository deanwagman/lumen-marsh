import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { AttractionQueries } from '@/features/attractions/api/AttractionQueries';
import { catalogAttractions, mangroveRun } from '@/test/fixtures';
import { renderApp } from '@/test/renderApp';

describe('live shift overview', () => {
  it('renders operational summary facts from the catalog', async () => {
    renderApp({ initialEntries: ['/attractions'] });

    expect(await screen.findByRole('heading', { name: 'Shift overview' })).toBeInTheDocument();
    expect(await screen.findByRole('link', { name: /mangrove run/i })).toBeInTheDocument();
    expect(screen.getByText('Needs attention')).toBeInTheDocument();
    expect(screen.getByText('Average posted wait')).toBeInTheDocument();
    expect(screen.getAllByText('25 min').length).toBeGreaterThan(0);
    expect(screen.getByText('Live')).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Attention required' })).not.toBeInTheDocument();
  });

  it('keeps closed attractions in the closed filter and writes the view to the URL', async () => {
    const user = userEvent.setup();
    renderApp({ initialEntries: ['/attractions'] });
    await screen.findByRole('link', { name: /mangrove run/i });

    await user.click(screen.getByRole('button', { name: 'Closed' }));

    expect(await screen.findByRole('link', { name: /cypress coil/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /stormglass station/i })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /mangrove run/i })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Closed' })).toHaveAttribute('aria-pressed', 'true');
  });

  it('reads a bookmarked attention filter from search params', async () => {
    renderApp({ initialEntries: ['/attractions?filter=attention'] });

    expect(await screen.findByRole('heading', { name: 'No attractions match this view' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Attention required' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
  });

  it('promotes a live SSE update into the attention queue, table flash, and announcement', async () => {
    const { queryClient } = renderApp({ initialEntries: ['/attractions'] });
    await screen.findByRole('link', { name: /mangrove run/i });

    const updated = catalogAttractions.map((attraction) =>
      attraction.id === 'mangrove-run'
        ? {
            ...mangroveRun,
            status: 'WEATHER_HOLD' as const,
            waitMinutes: null,
            statusMessage: 'Temporarily unavailable due to nearby weather.',
            version: 8,
            updatedAt: '2026-09-01T16:10:00Z',
          }
        : attraction,
    );
    queryClient.setQueryData(AttractionQueries.list(), updated);

    expect(await screen.findByRole('heading', { name: 'Attention required' })).toBeInTheDocument();
    const queue = screen.getByRole('heading', { name: 'Attention required' }).closest('section');
    expect(queue).toBeTruthy();
    expect(within(queue as HTMLElement).getByRole('link', { name: /mangrove run/i })).toHaveAttribute(
      'href',
      '/attractions/mangrove-run',
    );
    expect(screen.getByText('Updated just now')).toBeInTheDocument();
    expect(screen.getByText('Mangrove Run is now Weather Hold')).toBeInTheDocument();
    expect(screen.getByText('Needs attention').nextElementSibling).toHaveTextContent('1');
  });

  it('keeps cached rows visible while the stream is stale', async () => {
    const { queryClient } = renderApp({ initialEntries: ['/attractions'] });
    await screen.findByRole('link', { name: /mangrove run/i });

    queryClient.setQueryData(AttractionQueries.stream(), {
      status: 'stale',
      lastEventAt: Date.parse('2026-09-01T16:00:00Z'),
    });

    expect(await screen.findByRole('alert')).toHaveTextContent(/showing last-known conditions/i);
    expect(screen.getByRole('link', { name: /mangrove run/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Reconnect' })).toBeInTheDocument();
  });
});
