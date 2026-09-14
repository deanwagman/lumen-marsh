import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { localDevelopmentSession } from '@/auth/session';
import { lightningIncident } from '@/test/fixtures';
import { renderApp } from '@/test/renderApp';
import { incidentStore } from '@/test/server';

describe('IncidentsPage', () => {
  it('shows Incidents in the console rail', async () => {
    renderApp({ initialEntries: ['/incidents'] });
    const nav = await screen.findByRole('navigation', { name: 'Console' });
    expect(within(nav).getByRole('link', { name: 'Incidents' })).toHaveAttribute(
      'aria-current',
      'page',
    );
  });

  it('shows an empty incident center', async () => {
    renderApp({ initialEntries: ['/incidents'] });
    expect(await screen.findByRole('heading', { name: 'Incident Center' })).toBeInTheDocument();
    expect(await screen.findByRole('heading', { name: 'No incidents reported' })).toBeInTheDocument();
    expect(
      screen.getByText(
        /Operational incidents will appear here when they are reported by Control Tower./i,
      ),
    ).toBeInTheDocument();
  });

  it('displays incident rows and the open-incident count', async () => {
    incidentStore.push({ ...lightningIncident });
    renderApp({ initialEntries: ['/incidents'] });

    expect(
      await screen.findByRole('link', {
        name: /Lightning activity near western basin/i,
      }),
    ).toHaveAttribute('href', '/incidents/inc-lightning-1');
    expect(screen.getByText('Guest advisory not published')).toBeInTheDocument();

    const nav = screen.getByRole('navigation', { name: 'Console' });
    expect(within(nav).getByLabelText('1 open incidents')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Incident Center' })).toBeInTheDocument();
  });

  it('stores filters in the URL and applies them', async () => {
    incidentStore.push(
      { ...lightningIncident },
      {
        ...lightningIncident,
        id: 'inc-resolved',
        title: 'Resolved technical fault',
        type: 'TECHNICAL',
        status: 'RESOLVED',
        severity: 'MINOR',
      },
    );
    const user = userEvent.setup();
    renderApp({ initialEntries: ['/incidents'] });

    expect(await screen.findByRole('link', { name: /Lightning activity/i })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /Resolved technical fault/i })).not.toBeInTheDocument();

    await user.selectOptions(screen.getByLabelText('Status'), 'RESOLVED');
    expect(screen.getByLabelText('Status')).toHaveValue('RESOLVED');
    expect(await screen.findByRole('link', { name: /Resolved technical fault/i })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /Lightning activity/i })).not.toBeInTheDocument();

    await user.selectOptions(screen.getByLabelText('Type'), 'WEATHER');
    expect(screen.getByLabelText('Type')).toHaveValue('WEATHER');
    expect(
      await screen.findByRole('heading', { name: 'No incidents match this view' }),
    ).toBeInTheDocument();
  });

  it('preserves filters across refresh-like navigation', async () => {
    incidentStore.push({ ...lightningIncident });
    renderApp({ initialEntries: ['/incidents?status=open&severity=MAJOR&type=WEATHER'] });

    expect(await screen.findByRole('link', { name: /Lightning activity/i })).toBeInTheDocument();
    expect(screen.getByLabelText('Status')).toHaveValue('open');
    expect(screen.getByLabelText('Severity')).toHaveValue('MAJOR');
    expect(screen.getByLabelText('Type')).toHaveValue('WEATHER');
  });

  it('displays API error and retry', async () => {
    const user = userEvent.setup();
    const { server } = await import('@/test/server');
    const { http, HttpResponse } = await import('msw');
    let attempts = 0;

    server.use(
      http.get('/api/v1/operator/incidents', () => {
        attempts += 1;
        if (attempts === 1) {
          return HttpResponse.json({ message: 'boom' }, { status: 500 });
        }
        return HttpResponse.json([]);
      }),
    );

    renderApp({ initialEntries: ['/incidents'] });
    expect(await screen.findByRole('heading', { name: 'Unable to load incidents' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Retry' }));
    expect(await screen.findByRole('heading', { name: 'No incidents reported' })).toBeInTheDocument();
  });

  it('opens a reported incident and acknowledges it', async () => {
    incidentStore.push({ ...lightningIncident });
    const user = userEvent.setup();
    renderApp({ initialEntries: ['/incidents'] });

    await user.click(await screen.findByRole('link', { name: /Lightning activity/ }));
    expect(await screen.findByRole('heading', { name: lightningIncident.title })).toBeInTheDocument();
    expect(screen.getByText('Incident reported')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Acknowledge' }));
    await user.click(screen.getByRole('button', { name: 'Confirm command' }));
    expect(await screen.findAllByText('Acknowledged')).not.toHaveLength(0);
  });

  it('hides report when incident command scope is missing', async () => {
    renderApp({
      initialEntries: ['/incidents'],
      session: {
        ...localDevelopmentSession,
        role: 'operator',
        scopes: ['venueops/operator.read'],
      },
    });
    expect(await screen.findByRole('heading', { name: 'Incident Center' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Report incident' })).not.toBeInTheDocument();
  });
});
