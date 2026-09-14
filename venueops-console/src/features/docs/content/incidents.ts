import type { DocArticle } from '@/features/docs/domain/types';

export const incidentsArticle: DocArticle = {
  slug: 'incidents',
  title: 'Incidents',
  summary: 'Lifecycle, severity rules, linking attractions, and guest advisories.',
  category: 'incidents',
  sections: [
    {
      id: 'lifecycle',
      title: 'Incident lifecycle',
      body: [
        'Typical flow: report → acknowledge → start mitigation → resolve. Available commands depend on status and your role.',
        'Acknowledge before mitigation can start. Resolve closes the incident and withdraws any published guest advisory.',
        'Assign, change severity, and link or unlink attractions as needed while the incident is open. Linking does not change attraction operational state by itself.',
      ],
    },
    {
      id: 'severity',
      title: 'Severity and resolve rules',
      body: [
        'Operators may resolve MINOR and MODERATE incidents when they have incident command scope.',
        'MAJOR and CRITICAL resolve actions require a supervisor. If resolve is blocked, the panel explains why.',
        'Always provide a reason when resolving so the activity trail stays clear for the next shift.',
      ],
    },
    {
      id: 'advisories',
      title: 'Guest advisories',
      body: [
        'Guest advisories publish a title and message to the guest feed. Internal notes stay off that feed.',
        'Only supervisors with advisory publish scope can publish or withdraw advisories.',
        'Resolving an incident removes a published advisory. Withdraw manually when messaging should stop before resolve.',
        'Published advisories appear in the Flutter guest app (Today banner, advisories inbox, and detail). Guests never see operators, activity history, or internal descriptions.',
      ],
      roles: ['supervisor'],
    },
    {
      id: 'advisories-operators',
      title: 'Guest advisories (operators)',
      body: [
        'Operators can see whether an advisory is published on the incident detail.',
        'Publishing and withdrawing guest advisories requires a supervisor. Escalate when guests need an update.',
        'Once published, guests see the advisory on the park companion app until it is withdrawn or the incident is resolved.',
      ],
      roles: ['operator'],
    },
  ],
};
