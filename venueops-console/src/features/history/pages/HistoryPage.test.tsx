import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import styles from './HistoryPage.module.css';
import historyCss from './HistoryPage.module.css?raw';
import { historyDataset } from '@/features/history/test/fixtures';
import { renderApp } from '@/test/renderApp';
import { server } from '@/test/server';

const historyUrl =
  '/history?datasetId=lumen-marsh-availability-demo-v1&from=2026-10-13&to=2026-10-15&date=2026-10-15&attractionId=cypress-coil';

describe('operating history', () => {
  it('renders the dedicated synthetic history route and accessible chart alternatives', async () => {
    renderApp({ initialEntries: [historyUrl] });

    expect(
      await screen.findByRole('heading', { name: 'Operating history' }),
    ).toBeInTheDocument();
    expect(screen.getByText('Synthetic October story')).toBeInTheDocument();
    expect(
      screen.getByText(
        'This is the synthetic October story, separate from the live board.',
      ),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'History' })).toHaveAttribute(
      'aria-current',
      'page',
    );

    expect(
      await screen.findByRole('button', {
        name: /rank 1, cypress coil, 86\.81 percent uptime/i,
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

    const answer = await screen.findByRole('status', { name: 'Historical answer' });
    expect(answer).toHaveTextContent(/answer: cypress coil had the worst uptime/i);
    expect(
      within(answer).getByRole('link', {
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
    expect(
      screen.getByRole('link', {
        name: /technical delay, 2 hr 30 min, citation segment-cypress-technical/i,
      }),
    ).toHaveAttribute('href', '#segment-cypress-technical');
    expect(
      screen.queryByRole('img', { name: /status timeline/i }),
    ).not.toBeInTheDocument();
  });

  it('renders the cause and answers from the summary and opens a cited segment', async () => {
    const user = userEvent.setup();
    renderApp({ initialEntries: [historyUrl] });

    const answers = await screen.findByRole('region', {
      name: 'Operating story answers',
    });
    expect(
      within(answers).getByRole('heading', {
        name: 'Which attraction had the worst uptime?',
      }),
    ).toBeInTheDocument();
    expect(
      within(answers).getByText(/maintenance episode on 2026-10-15/i),
    ).toBeInTheDocument();
    expect(
      within(answers).getByText(/worst uptime was on 2026-10-15/i),
    ).toBeInTheDocument();

    const cause = await screen.findByRole('region', {
      name: 'Cause for this day',
    });
    expect(
      within(cause).getByText(
        'A vibration recommendation was recorded for Cypress Coil.',
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/reduced-capacity hour stays in uptime/i),
    ).toBeInTheDocument();

    await user.click(
      within(answers).getAllByRole('link', {
        name: /2026-10-15 · segment-cypress-technical/i,
      })[0],
    );

    expect(await screen.findByLabelText('Park date')).toHaveValue('2026-10-15');
    expect(document.getElementById('segment-cypress-technical')).toHaveAttribute(
      'data-highlighted',
      'true',
    );
  });

  it('says the October story is not ready instead of showing an empty ranking', async () => {
    server.use(
      http.get('/api/v1/operator/analytics/datasets', () =>
        HttpResponse.json([{ ...historyDataset, state: 'LOADING' }]),
      ),
    );
    renderApp({ initialEntries: ['/history'] });

    expect(
      await screen.findByText('The October operating story is not ready yet.'),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole('heading', { name: 'Ranked uptime' }),
    ).not.toBeInTheDocument();
  });

  it('keeps the cause and answers readable at phone width', async () => {
    renderApp({ initialEntries: [historyUrl] });
    expect(
      await screen.findByRole('region', { name: 'Operating story answers' }),
    ).toHaveClass(styles.storyAnswers);
    expect(
      screen
        .getByRole('region', { name: 'Cause for this day' })
        .querySelector('ol'),
    ).toHaveClass(styles.episodeCauses);
    const css = historyCss;
    expect(css).toMatch(/@media \(max-width: 30rem\)/);
    expect(css).toContain('.storyAnswers');
    expect(css).toContain('.episodeCauses');
    expect(css).toContain('overflow-wrap: anywhere');
    expect(css).toContain('flex-direction: column');
  });
});
