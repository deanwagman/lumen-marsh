import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { AttractionQueries } from '@/features/attractions/api/AttractionQueries';
import { handleAttractionStreamEvent } from '@/features/attractions/api/AttractionEventSource';
import { WeatherRecommendationQueries } from '@/features/weather/api/WeatherRecommendationQueries';
import type { WeatherRecommendation } from '@/features/weather/domain/recommendation';
import {
  localDevelopmentSession,
  venueOpsScopes,
  type AuthSession,
} from '@/auth/session';
import {
  catalogAttractions,
  lightningHoldRecommendation,
} from '@/test/fixtures';
import { createTestQueryClient, renderApp } from '@/test/renderApp';
import { weatherInbox } from '@/test/server';

function renderOverview(
  recommendations: WeatherRecommendation[] = [],
  session: AuthSession = localDevelopmentSession,
) {
  weatherInbox.splice(
    0,
    weatherInbox.length,
    ...recommendations.map((item) => ({ ...item })),
  );
  const queryClient = createTestQueryClient();
  queryClient.setQueryData(AttractionQueries.list(), catalogAttractions);
  queryClient.setQueryDefaults(AttractionQueries.list(), {
    staleTime: Infinity,
  });
  queryClient.setQueryData(
    WeatherRecommendationQueries.list(),
    recommendations,
  );
  return renderApp({ initialEntries: ['/attractions'], queryClient, session });
}

const operatorSession: AuthSession = {
  accessToken: 'operator-token',
  operatorId: 'operator-1',
  displayName: 'Development Operator',
  role: 'operator',
  scopes: [
    venueOpsScopes.operatorRead,
    venueOpsScopes.weatherRecommendationsReview,
    venueOpsScopes.incidentsCommand,
  ],
};

describe('weather recommendations on shift overview', () => {
  it('renders the inbox section when the catalog loads', () => {
    renderOverview();

    expect(
      screen.getByRole('heading', { name: 'Weather recommendations' }),
    ).toBeInTheDocument();
    expect(
      screen.getByText('No weather recommendations in the inbox.'),
    ).toBeInTheDocument();
  });

  it('shows a live lightning recommendation with operator actions', () => {
    renderOverview([lightningHoldRecommendation], operatorSession);

    expect(
      screen.getByText('Place Mangrove Run and Cypress Coil on weather hold'),
    ).toBeInTheDocument();
    expect(screen.getByText(/simulated lightning strike/i)).toBeInTheDocument();
    expect(screen.getByText('Simulated')).toBeInTheDocument();
    expect(screen.getByText(/mangrove run, cypress coil/i)).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Acknowledge' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Dismiss' })).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Create incident' }),
    ).toBeInTheDocument();
  });

  it('uses scopes rather than role to authorize recommendation actions', () => {
    renderOverview([lightningHoldRecommendation], {
      ...operatorSession,
      role: 'supervisor',
      scopes: [venueOpsScopes.operatorRead],
    });

    expect(
      screen.queryByRole('button', { name: 'Acknowledge' }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: 'Dismiss' }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: 'Create incident' }),
    ).not.toBeInTheDocument();
  });

  it('requires incident-command scope before offering incident creation', () => {
    renderOverview([lightningHoldRecommendation], {
      ...operatorSession,
      scopes: [
        venueOpsScopes.operatorRead,
        venueOpsScopes.weatherRecommendationsReview,
      ],
    });

    expect(
      screen.getByRole('button', { name: 'Acknowledge' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Dismiss' })).toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: 'Create incident' }),
    ).not.toBeInTheDocument();
  });

  it('acknowledges a recommendation from Control Tower', async () => {
    const user = userEvent.setup();
    renderOverview([{ ...lightningHoldRecommendation }]);

    await user.click(screen.getByRole('button', { name: 'Acknowledge' }));

    expect(await screen.findByText('Acknowledged')).toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: 'Acknowledge' }),
    ).not.toBeInTheDocument();
  });

  it('dismisses a recommendation from Control Tower', async () => {
    const user = userEvent.setup();
    renderOverview([{ ...lightningHoldRecommendation }]);

    await user.click(screen.getByRole('button', { name: 'Dismiss' }));

    expect(await screen.findByText('Dismissed')).toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: 'Acknowledge' }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: 'Create incident' }),
    ).not.toBeInTheDocument();
  });

  it('prefills a weather incident for operator review', async () => {
    const user = userEvent.setup();
    renderOverview([{ ...lightningHoldRecommendation }]);

    await user.click(screen.getByRole('button', { name: 'Create incident' }));

    expect(
      screen.getByRole('heading', { name: 'Review weather incident' }),
    ).toBeInTheDocument();
    expect(
      screen.getByDisplayValue(lightningHoldRecommendation.summary),
    ).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: 'Severity' })).toHaveValue(
      'MAJOR',
    );
    expect(
      screen.getByRole('textbox', { name: 'Internal description' }),
    ).toHaveValue(
      `${lightningHoldRecommendation.evidence}\n\nRecommended action: ${lightningHoldRecommendation.recommendedAction}`,
    );
    expect(
      screen.getByRole('checkbox', { name: 'Mangrove Run' }),
    ).toBeChecked();
    expect(
      screen.getByRole('checkbox', { name: 'Cypress Coil' }),
    ).toBeChecked();

    await user.click(screen.getByRole('button', { name: 'Submit incident' }));

    expect(
      await screen.findByRole('link', { name: 'View linked incident' }),
    ).toHaveAttribute('href', '/incidents/inc-weather-1');
    expect(screen.getByText('Linked')).toBeInTheDocument();
  });

  it('applies a live weather SSE update into the inbox', async () => {
    const { queryClient } = renderOverview();

    handleAttractionStreamEvent(
      queryClient,
      'weather.recommendation.updated',
      JSON.stringify({
        eventId: 'weather-evt-1',
        eventType: 'UPDATED',
        occurredAt: '2026-09-01T16:00:00Z',
        recommendation: lightningHoldRecommendation,
      }),
    );

    expect(
      await screen.findByText(
        'Place Mangrove Run and Cypress Coil on weather hold',
      ),
    ).toBeInTheDocument();
    expect(
      queryClient.getQueryData(WeatherRecommendationQueries.list()),
    ).toEqual([lightningHoldRecommendation]);

    const section = screen
      .getByRole('heading', { name: 'Weather recommendations' })
      .closest('section');
    expect(section).toBeTruthy();
    expect(
      within(section as HTMLElement).getByText('Simulated'),
    ).toBeInTheDocument();
  });

  it('marks a recommendation cleared from a live operator event', async () => {
    const { queryClient } = renderOverview([lightningHoldRecommendation]);

    handleAttractionStreamEvent(
      queryClient,
      'weather.recommendation.cleared',
      JSON.stringify({
        eventId: 'weather-evt-2',
        eventType: 'CLEARED',
        occurredAt: '2026-09-01T16:30:00Z',
        recommendation: {
          ...lightningHoldRecommendation,
          status: 'CLEARED',
          severity: 'INFO',
          sourceVersion: 2,
          version: 2,
          evidence: 'Simulated lightning is outside the hold radius.',
          recommendedAction: 'Review weather holds for return-to-service',
          updatedAt: '2026-09-01T16:30:00Z',
        },
      }),
    );

    expect(await screen.findByText('Cleared')).toBeInTheDocument();
    expect(screen.getByText(/outside the hold radius/i)).toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: 'Acknowledge' }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: 'Create incident' }),
    ).not.toBeInTheDocument();
  });
});
