import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { WeatherRecommendationQueries } from '@/features/weather/api/WeatherRecommendationQueries';
import {
  lightningHoldRecommendation,
  lightningIncident,
  lightningIncidentActivity,
} from '@/test/fixtures';
import { createTestQueryClient, renderApp } from '@/test/renderApp';
import { incidentStore } from '@/test/server';

describe('IncidentDetailPage', () => {
  it('displays incident facts, description, attractions, and unpublished advisory', async () => {
    incidentStore.push({
      ...lightningIncident,
      attractionIds: ['mangrove-run', 'unknown-attraction'],
    });
    renderApp({ initialEntries: [`/incidents/${lightningIncident.id}`] });

    expect(
      await screen.findByRole('heading', { name: lightningIncident.title }),
    ).toBeInTheDocument();
    expect(screen.getByText('Internal description')).toBeInTheDocument();
    expect(screen.getByText(lightningIncident.internalDescription!)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Mangrove Run' })).toHaveAttribute(
      'href',
      '/attractions/mangrove-run',
    );
    expect(screen.getByRole('link', { name: 'unknown-attraction' })).toBeInTheDocument();
    expect(
      screen.getByText('Incident links do not change attraction operating state.'),
    ).toBeInTheDocument();
    expect(screen.getByText('Not published')).toBeInTheDocument();
    expect(
      screen.getByText('No guest-facing message is active for this incident.'),
    ).toBeInTheDocument();
    expect(screen.getByText('Incident reported')).toBeInTheDocument();
  });

  it('displays published guest advisory copy', async () => {
    incidentStore.push({
      ...lightningIncident,
      guestAdvisoryPublished: true,
      guestTitle: 'Weather advisory',
      guestMessage: 'Some outdoor attractions are temporarily paused.',
    });
    renderApp({ initialEntries: [`/incidents/${lightningIncident.id}`] });

    expect(await screen.findByText('Published')).toBeInTheDocument();
    expect(screen.getAllByText('Weather advisory').length).toBeGreaterThan(0);
    expect(
      screen.getAllByText('Some outdoor attractions are temporarily paused.').length,
    ).toBeGreaterThan(0);
  });

  it('keeps detail visible when activity loading fails', async () => {
    incidentStore.push({ ...lightningIncident });
    const { server } = await import('@/test/server');
    const { http, HttpResponse } = await import('msw');
    server.use(
      http.get('/api/v1/operator/incidents/:id/activity', () =>
        HttpResponse.json({ message: 'activity down' }, { status: 500 }),
      ),
    );

    renderApp({ initialEntries: [`/incidents/${lightningIncident.id}`] });

    expect(
      await screen.findByRole('heading', { name: lightningIncident.title }),
    ).toBeInTheDocument();
    expect(
      await screen.findByText('Unable to load incident activity'),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument();
  });

  it('handles incident not found', async () => {
    renderApp({ initialEntries: ['/incidents/missing-incident'] });

    expect(await screen.findByRole('heading', { name: 'Incident not found' })).toBeInTheDocument();
    expect(
      screen.getByText(/The incident may have been removed or the address may be incorrect./i),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Return to incidents' })).toHaveAttribute(
      'href',
      '/incidents',
    );
  });

  it('displays resolved incidents as read-only in the command panel', async () => {
    incidentStore.push({
      ...lightningIncident,
      status: 'RESOLVED',
    });
    renderApp({ initialEntries: [`/incidents/${lightningIncident.id}`] });

    expect(
      await screen.findByRole('heading', { name: lightningIncident.title }),
    ).toBeInTheDocument();
    expect(screen.getByText('Resolved incidents are read-only.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Acknowledge' })).not.toBeInTheDocument();
  });

  it('finds and displays the linked weather recommendation', async () => {
    incidentStore.push({ ...lightningIncident });
    const queryClient = createTestQueryClient();
    queryClient.setQueryData(WeatherRecommendationQueries.list(), [
      {
        ...lightningHoldRecommendation,
        linkedIncidentId: lightningIncident.id,
        operatorStatus: 'LINKED',
      },
    ]);

    renderApp({
      initialEntries: [`/incidents/${lightningIncident.id}`],
      queryClient,
    });

    expect(await screen.findByText('Source recommendation')).toBeInTheDocument();
    expect(screen.getByText(lightningHoldRecommendation.summary)).toBeInTheDocument();
    expect(screen.getByText(/Warning · Active · Linked/i)).toBeInTheDocument();
  });

  it('returns to the previous filtered list when navigating back', async () => {
    incidentStore.push({ ...lightningIncident });
    const user = userEvent.setup();
    renderApp({
      initialEntries: [`/incidents?type=WEATHER`, `/incidents/${lightningIncident.id}`],
    });

    expect(
      await screen.findByRole('heading', { name: lightningIncident.title }),
    ).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '← Incidents' }));

    expect(await screen.findByRole('heading', { name: 'Incident Center' })).toBeInTheDocument();
    expect(screen.getByLabelText('Type')).toHaveValue('WEATHER');
    expect(await screen.findByRole('link', { name: /Lightning activity/i })).toBeInTheDocument();
  });

  it('preserves filtered list entry points from the URL', async () => {
    incidentStore.push({ ...lightningIncident });
    renderApp({ initialEntries: ['/incidents?type=WEATHER'] });

    expect(await screen.findByRole('heading', { name: 'Incident Center' })).toBeInTheDocument();
    expect(screen.getByLabelText('Type')).toHaveValue('WEATHER');
    expect(lightningIncidentActivity.type).toBe('INCIDENT_REPORTED');
  });
});
