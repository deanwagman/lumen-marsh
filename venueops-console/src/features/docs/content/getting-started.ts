import type { DocArticle } from '@/features/docs/domain/types';

export const gettingStartedArticle: DocArticle = {
  slug: 'getting-started',
  title: 'Getting started',
  summary: 'What this console is for, who can do what, and how to move around.',
  category: 'overview',
  sections: [
    {
      id: 'what-this-is',
      title: 'What this console is',
      body: [
        'Lumen Marsh Control is the operator workstation for attraction status, incidents, and weather recommendations.',
        'Use it during a shift to keep posted waits accurate, escalate issues, and follow weather guidance. Guests never see this console.',
      ],
    },
    {
      id: 'roles',
      title: 'Operator vs supervisor',
      body: [
        'Operators can view attractions, update wait times when an attraction is operating, report and work most incidents, and review weather items when scoped.',
        'Supervisors can do everything an operator can, plus run attraction state-change commands, resolve MAJOR and CRITICAL incidents, and publish or withdraw guest advisories.',
        'If a control is missing or disabled, your role or scopes may not allow it. The API is the final authority.',
      ],
    },
    {
      id: 'navigation',
      title: 'How to navigate',
      body: [
        'Use Dashboard for park-wide conditions after sign-in.',
        'Use Attractions for the shift overview and per-attraction command workspace.',
        'Use Incidents for the incident center and detail commands.',
        'Weather recommendations appear on the Attractions overview. Docs (this section) holds procedures and deep links from help controls next to key panels.',
      ],
    },
  ],
};
