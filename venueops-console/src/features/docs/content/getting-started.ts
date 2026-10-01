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
      id: 'stack',
      title: 'Six-piece stack',
      body: [
        'Six runtime pieces share one Compose graph. VenueOps API is the system of record. Environmental Monitor, Park Flow Intelligence, and Reliability Intelligence recommend; they never command a ride or open a work order. This console is Control Tower. The Flutter guest app is the sanitized companion.',
        'Reliability Intelligence posts Cypress Coil vibration into the maintenance inbox. The Dashboard attention queue includes open P1 work orders and unpublished flow recommendations, not only weather.',
        'Attraction, incident, maintenance, and flow commands use the same envelope: commandId, type, expectedVersion, optional reason, optional data. Retries reuse commandId. A new intent gets a new identifier. Weather inbox ACKNOWLEDGE, DISMISS, and LINK_INCIDENT stay on their existing shape.',
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
      id: 'operators-and-guests',
      title: 'Operators vs guests',
      body: [
        'Operators decide. Guests see a published projection: attraction status, rounded waits, advisories, and Best Next from published park-flow guidance only.',
        'Guest copy never includes internalDescription, actor names, work-order numbers, asset codes, unpublished recommendations, or weather-inbox events.',
        'Publishing a guest advisory or park-flow message is a supervisor command. Automation does not close Mangrove Run, open a work order, or rewrite Best Next by itself.',
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
        'Use Park Flow for queue forecasts and operator-reviewed guest guidance. Publishing guest guidance is supervisor-only.',
        'Weather recommendations appear on the Attractions overview. Docs (this section) holds procedures and deep links from help controls next to key panels.',
      ],
    },
    {
      id: 'proofs',
      title: 'Repeatable proofs',
      body: [
        'Three HTTP proofs live next to Compose: storm-lifecycle-acceptance.sh (weather → hold → guest advisory → testing), maintenance-lifecycle-acceptance.sh (reliability ingest → inspect → operations testing), and flow-lifecycle-acceptance.sh (mangrove disruption → publish → guest Best Next).',
        'Run them locally after ./scripts/dev-up.sh. GitHub Actions runs the same Compose proofs on a manual workflow and on a nightly schedule. Pull-request CI stays on unit, lint, and config checks so the full stack is not on every PR.',
      ],
    },
  ],
};
