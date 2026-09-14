import { QueryClient } from '@tanstack/react-query';
import { render, type RenderOptions } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';

import { appRoutes } from '@/app/router';
import { AppProviders } from '@/app/providers';
import { AttractionQueries } from '@/features/attractions/api/AttractionQueries';
import { WeatherRecommendationQueries } from '@/features/weather/api/WeatherRecommendationQueries';
import { localDevelopmentSession, type AuthSession } from '@/auth/session';

export function createTestQueryClient(): QueryClient {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        refetchOnWindowFocus: false,
      },
    },
  });

  queryClient.setQueryData(AttractionQueries.stream(), {
    status: 'live',
    lastEventAt: Date.parse('2026-08-28T16:00:00Z'),
  });
  queryClient.setQueryData(WeatherRecommendationQueries.list(), []);
  queryClient.setQueryDefaults(WeatherRecommendationQueries.list(), { staleTime: Infinity });

  return queryClient;
}

export function renderApp(
  options?: {
    initialEntries?: string[];
    queryClient?: QueryClient;
    session?: AuthSession | null;
  } & Omit<RenderOptions, 'wrapper'>,
) {
  const {
    initialEntries,
    queryClient: providedQueryClient,
    session = localDevelopmentSession,
    ...renderOptions
  } =
    options ?? {};
  const queryClient = providedQueryClient ?? createTestQueryClient();
  const router = createMemoryRouter(appRoutes, {
    initialEntries: initialEntries ?? ['/'],
  });

  return {
    queryClient,
    ...render(
      <AppProviders queryClient={queryClient} initialSession={session}>
        <RouterProvider router={router} />
      </AppProviders>,
      renderOptions,
    ),
  };
}
