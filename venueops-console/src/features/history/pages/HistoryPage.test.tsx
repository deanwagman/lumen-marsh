import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { renderApp } from '@/test/renderApp';

const historyUrl =
  '/history?datasetId=lumen-marsh-availability-demo-v1&from=2026-10-13&to=2026-10-15&date=2026-10-15&attractionId=cypress-coil';

describe('operating history', () => {
  it('renders the dedicated synthetic history route and accessible chart alternatives', async () => {
    renderApp({ initialEntries: [historyUrl] });

    expect(
      await screen.findByRole('heading', { name: 'Operating history' }),
    ).toBeInTheDocument();
    expect(
      screen.getByText('Synthetic historical dataset'),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'History' })).toHaveAttribute(
      'aria-current',
      'page',
    );

    expect(
      await screen.findByRole('button', {
        name: /rank 1, cypress coil, 77\.08 percent uptime/i,
      }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('table', {
        name: 'Accessible ranked uptime comparison',
      }),
    ).toBeInTheDocument();

    const timelineTable = await screen.findByRole('table', {
      name: 'Timeline segments and stable citations',
    });
    expect(
      within(timelineTable).getByText('Technical delay'),
    ).toBeInTheDocument();
    expect(
      within(timelineTable).getByText('segment-cypress-technical'),
    ).toHaveAttribute('href', '#segment-cypress-technical');
    expect(
      screen.getByText('Total downtime').nextElementSibling,
    ).toHaveTextContent('3 hr 10 min');
    expect(
      screen.getByText('Longest outage', { selector: 'dt' }).nextElementSibling,
    ).toHaveTextContent('3 hr 10 min');
  });

  it('submits a deterministic question and links its supporting citation', async () => {
    const user = userEvent.setup();
    renderApp({ initialEntries: [historyUrl] });

    await screen.findByRole('heading', { name: 'Ask the curated history' });
    await user.click(screen.getByRole('button', { name: 'Ask' }));

    expect(
      await screen.findByRole('status', { name: 'Historical answer' }),
    ).toHaveTextContent(/answer: cypress coil had the worst uptime/i);
    expect(
      screen.getByRole('link', {
        name: /2026-10-15 · segment-cypress-technical/i,
      }),
    ).toHaveAttribute(
      'href',
      '/history?datasetId=lumen-marsh-availability-demo-v1&date=2026-10-15&attractionId=cypress-coil&citation=segment-cypress-technical',
    );
    expect(
      screen.getByText(/provider: deterministic-curated-availability/i),
    ).toBeInTheDocument();
  });

  it('provides non-color timeline labels and symbols', async () => {
    renderApp({ initialEntries: [historyUrl] });

    const key = await screen.findByRole('list', { name: 'Timeline key' });
    expect(within(key).getByText('Operating')).toBeInTheDocument();
    expect(within(key).getByText('Technical delay')).toBeInTheDocument();
    expect(
      within(key).getByText('Testing / return to service'),
    ).toBeInTheDocument();
    expect(within(key).getByText('●')).toBeInTheDocument();
    expect(within(key).getByText('×')).toBeInTheDocument();
    expect(within(key).getByText('◆')).toBeInTheDocument();
  });
});
