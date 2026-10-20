import type { DocArticle } from '@/features/docs/domain/types';

export const operatingHistoryArticle: DocArticle = {
  slug: 'operating-history',
  title: 'Operating history',
  summary:
    'Interpret synthetic availability, downtime, stable citations, and deterministic answers.',
  category: 'history',
  sections: [
    {
      id: 'scope',
      title: 'Scope and isolation',
      body: [
        'The persistent label is Synthetic October story. This is the synthetic October story, separate from the live board.',
        'History uses dataset-scoped replay records. The synthetic historical dataset label means the records are demonstration fixtures, not observed production events.',
        'Changing a dataset, date range, attraction, or question never changes the live attraction workspace, live activity, or guest experience.',
        'Hosted Start over clears live demonstration activity and returns attractions to the quiet opening. It keeps historical datasets, calendars, plans, snapshots, activity, and processed commands.',
        'A full database volume reset removes that history. The next startup reloads the bundled fixture when its checksum is missing, without changing the live attraction workspace.',
      ],
    },
    {
      id: 'availability-definitions',
      title: 'Availability definitions',
      body: [
        'Scheduled minutes come from each attraction’s expected-service interval in America/New_York. The API constructs all time boundaries; Control Tower does not infer them from the browser timezone.',
        'Operating and reduced-capacity segments count as uptime. Reduced capacity is also reported separately as degraded time.',
        'A reduced-capacity hour stays in uptime and is also reported as degraded time, so a slow hour does not become downtime.',
        'Weather hold, technical delay, testing or return to service, and commanded closure inside expected service count as downtime. An attraction that never opens receives Closed · did not open for its entire expected interval.',
        'A planned closure has no availability denominator and is excluded from rankings. Uptime rankings and percentages are rendered exactly as returned by the server.',
      ],
    },
    {
      id: 'charts-and-tables',
      title: 'Charts and table alternatives',
      body: [
        'The ranked comparison orders lowest uptime first. Select a comparison bar or use the attraction and park-date controls to inspect one day.',
        'Timeline color is never the only cue: symbols, pattern fills, labels, and the segment table identify every state. The tables contain the exact values represented by each visual.',
      ],
    },
    {
      id: 'citations',
      title: 'Stable citations',
      body: [
        'Every timeline segment has a stable segment identifier. Choosing a citation opens that park date and highlights the segment.',
        'A citation link selects its dataset, attraction, and park date, then scrolls to the matching segment row. Evidence rows cite the same segment when they explain that part of the day.',
        'Segment rows include exact start and end instants rendered in the server-provided park timezone, duration, classification, reason, and citation identifier. Use that identifier when reviewing or sharing a claim.',
      ],
    },
    {
      id: 'questions',
      title: 'Deterministic questions',
      body: [
        'Ask the curated history submits the selected dataset and inclusive date range with the question. The current provider reads only the availability summary; it cannot query raw SQL or invent a new calculation.',
        'The availability summary answers three questions without taking a turn: which attraction had the worst uptime, what caused it, and which day. Claims cite the segment and, where relevant, the evidence row.',
        'The same three wordings can be submitted to the curated history. The provider reads only this availability projection. No external model, vendor, or secret is used.',
      ],
    },
  ],
};
