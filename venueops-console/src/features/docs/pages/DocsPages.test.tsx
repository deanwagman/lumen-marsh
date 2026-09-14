import { render, screen } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';

import { AppProviders } from '@/app/providers';
import { HelpLink } from '@/shared/ui/HelpLink';
import { createTestQueryClient, renderApp } from '@/test/renderApp';
import {
  localDevelopmentSession,
  venueOpsScopes,
  type AuthSession,
} from '@/auth/session';

const operatorSession: AuthSession = {
  accessToken: 'operator-token',
  operatorId: 'operator-1',
  displayName: 'Development Operator',
  role: 'operator',
  scopes: [venueOpsScopes.operatorRead, venueOpsScopes.incidentsCommand],
};

describe('docs pages', () => {
  it('lists operator guides on the docs index', async () => {
    renderApp({ initialEntries: ['/docs'] });

    expect(
      await screen.findByRole('heading', { name: 'Operator guides' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /getting started/i })).toHaveAttribute(
      'href',
      '/docs/getting-started',
    );
    expect(
      screen.getByRole('link', {
        name: /operations dashboard.*park-wide conditions/i,
      }),
    ).toHaveAttribute('href', '/docs/dashboard');
    expect(
      screen.getByRole('link', {
        name: /attractions.*shift overview, state commands/i,
      }),
    ).toHaveAttribute('href', '/docs/attractions');
    expect(screen.getByRole('navigation', { name: 'Console' })).toContainElement(
      screen.getByRole('link', { name: 'Docs' }),
    );
  });

  it('renders article sections with stable anchors', async () => {
    renderApp({ initialEntries: ['/docs/attractions'] });

    expect(
      await screen.findByRole('heading', { name: 'Attractions', level: 1 }),
    ).toBeInTheDocument();
    expect(document.getElementById('commands')).toBeTruthy();
    expect(document.getElementById('wait-times')).toBeTruthy();
    expect(
      screen.getByRole('link', { name: 'Attraction commands' }),
    ).toHaveAttribute('href', '/docs/attractions#commands');
  });

  it('filters role-specific sections on the article page', async () => {
    renderApp({ initialEntries: ['/docs/incidents'], session: operatorSession });

    expect(
      await screen.findByRole('heading', { name: 'Incidents', level: 1 }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Guest advisories (operators)' }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole('heading', { name: 'Guest advisories' }),
    ).not.toBeInTheDocument();
  });

  it('redirects unknown slugs to not found', async () => {
    renderApp({ initialEntries: ['/docs/does-not-exist'] });

    expect(
      await screen.findByRole('heading', { name: 'Page not found' }),
    ).toBeInTheDocument();
  });

  it('scrolls to the hashed section when present', async () => {
    const scrollIntoView = vi.fn();
    HTMLElement.prototype.scrollIntoView = scrollIntoView;

    renderApp({ initialEntries: ['/docs/weather#review'] });

    expect(
      await screen.findByRole('heading', { name: 'Weather recommendations', level: 1 }),
    ).toBeInTheDocument();
    expect(scrollIntoView).toHaveBeenCalled();
  });
});

describe('HelpLink', () => {
  it('renders an accessible deep link to a docs section', () => {
    const queryClient = createTestQueryClient();
    const router = createMemoryRouter(
      [
        {
          path: '/',
          element: (
            <HelpLink
              article="attractions"
              section="commands"
              label="Help: attraction commands"
            />
          ),
        },
      ],
      { initialEntries: ['/'] },
    );

    render(
      <AppProviders queryClient={queryClient} initialSession={localDevelopmentSession}>
        <RouterProvider router={router} />
      </AppProviders>,
    );

    expect(
      screen.getByRole('link', { name: 'Help: attraction commands' }),
    ).toHaveAttribute('href', '/docs/attractions#commands');
  });
});
