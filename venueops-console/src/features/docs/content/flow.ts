import type { DocArticle } from '@/features/docs/domain/types';

export const parkFlowArticle: DocArticle = {
  slug: 'park-flow',
  title: 'Park Flow',
  summary: 'Review queue forecasts and publish guest guidance without letting automation change attraction state.',
  category: 'flow',
  sections: [
    {
      id: 'human-in-the-loop',
      title: 'Human in the loop',
      body: [
        'Park Flow Intelligence may recommend congestion, demand-shift, or guest-redirection actions. It cannot change attraction status, capacity, posted waits, or guest guidance.',
        'Flow recommendations do not change attraction state or capacity automatically.',
      ],
    },
    {
      id: 'forecasts',
      title: 'Forecast presentation',
      body: [
        'Each attraction card shows posted wait, calculated wait, and 15-, 30-, and 60-minute predictions. The compact chart has a text equivalent for screen readers.',
        'Freshness is FRESH up to two minutes, DELAYED up to five minutes, and STALE after that. Stale telemetry is a warning, not a reason to automate a ride change.',
      ],
    },
    {
      id: 'recommendations',
      title: 'Recommendation review',
      body: [
        'Operators with flow.command can approve or dismiss pending recommendations. The dialog submits Approve or Dismiss. Dismissal and withdrawal require a reason.',
        'If another operator changes the record, the dialog stays open with the form values and submission is blocked until you close it and review the current state.',
      ],
    },
    {
      id: 'publication',
      title: 'Guest publication',
      body: [
        'Publishing or withdrawing guest guidance requires ROLE_SUPERVISOR and venueops/flow.publish. The dialog submits Publish or Withdraw.',
        'Guests only see published guidance. Operator identities, incidents, work orders, and unpublished recommendations stay inside Control Tower.',
        'Best Next in the guest app must follow that published copy. If the message says an attraction is unavailable, it is not ranked as a good next choice even when posted wait still looks operating. Recommended destinations can still appear when they are operating.',
      ],
      roles: ['supervisor'],
    },
    {
      id: 'demo-story',
      title: 'Mangrove disruption scenario',
      body: [
        'Start the stack, then POST /simulation/scenarios/mangrove-disruption on Park Flow Intelligence. Simulated boarding at Mangrove Run drops to zero and a share of expected arrivals moves to Cypress Coil and Stormglass Station.',
        'Predicted waits rise before posted waits change. An operator reviews the recommendation; a supervisor publishes guest guidance. The Flutter app shows that guidance and does not rank Mangrove Run as a good next choice while the published copy says it is unavailable. Guests never see incidents, work orders, or operator identities.',
        'Park Flow Intelligence never changes attraction status or capacity. Repeat the HTTP story with ./scripts/flow-lifecycle-acceptance.sh after the stack is up (or POST /simulation/scenarios/mangrove-disruption on :8100), and reset with ./scripts/reset-demo.sh --confirm.',
      ],
    },
    {
      id: 'retention',
      title: 'Data retention',
      body: [
        'Local demo data lives in Compose volumes until you run reset-demo. Observation, forecast, and recommendation rows are demonstration artifacts marked simulated. Production retention for queue observations is 24 hours; this milestone does not ship an AWS purge job.',
      ],
    },
    {
      id: 'scopes',
      title: 'Required scopes',
      body: [
        'flow.read opens the workspace. flow.command reviews recommendations. flow.publish is supervisor-only for guest guidance.',
      ],
    },
  ],
};
