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
        'Lumen Marsh Control is the operator workstation for attraction status, incidents, weather recommendations, maintenance, and park flow.',
        'Use it during a shift to keep posted waits accurate, escalate issues, follow weather guidance, work reliability-driven maintenance, and review queue forecasts. Guests never see this console.',
      ],
    },
    {
      id: 'roles',
      title: 'Operator vs supervisor',
      body: [
        'Operators can view attractions, update wait times when an attraction is operating, report and work most incidents, review weather and park-flow items when scoped, and work maintenance except inspection approval.',
        'Supervisors inherit that work, plus Control Tower shows attraction state-change commands, MAJOR/CRITICAL resolve, guest advisory publish/withdraw, maintenance inspection, and guest flow publish. The API still authorizes attraction commands with venueops/attractions.command for any operator token that has that scope — no attraction command is supervisor-only.',
        'If a control is missing or disabled, the UI may be hiding it. The API is the final authority.',
      ],
    },
    {
      id: 'navigation',
      title: 'How to navigate',
      body: [
        'Use Dashboard for park-wide conditions after sign-in.',
        'Use Attractions for the shift overview and per-attraction command workspace.',
        'Use Incidents for the incident center and detail commands.',
        'Use Maintenance for reliability recommendations, work orders, checklists, and inspection handoff to Operations.',
        'Use Park Flow for queue forecasts and operator-reviewed guest guidance.',
        'Weather recommendations appear on the Attractions overview. Docs (this section) holds procedures and deep links from help controls next to key panels.',
      ],
    },
  ],
};
