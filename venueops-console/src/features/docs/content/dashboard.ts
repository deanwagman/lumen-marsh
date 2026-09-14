import type { DocArticle } from '@/features/docs/domain/types';

export const dashboardArticle: DocArticle = {
  slug: 'dashboard',
  title: 'Operations dashboard',
  summary: 'Park-wide conditions, needs attention, and shift handoff.',
  category: 'overview',
  sections: [
    {
      id: 'landing',
      title: 'Landing page',
      body: [
        'After sign-in, the console opens on the operations dashboard. It is read-only. Commands still happen in Attractions, Incidents, and the weather review inbox.',
        'Use Needs attention first, then open the linked workspace for the item you will work.',
      ],
    },
    {
      id: 'freshness',
      title: 'Live and stale data',
      body: [
        'The header shows park time, snapshot time, live-connection state, and who is signed in.',
        'If live updates are interrupted, the last snapshot stays visible. Do not treat stale or unavailable data as current.',
      ],
    },
    {
      id: 'handoff',
      title: 'Shift handoff',
      body: [
        'Generate shift handoff builds a summary from the current snapshot. Copy or print it for the incoming operator.',
        'The summary is not stored as a record and does not replace incident or attraction history.',
      ],
    },
  ],
};
