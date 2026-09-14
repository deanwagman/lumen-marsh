import type { DocArticle } from '@/features/docs/domain/types';

export const attractionsArticle: DocArticle = {
  slug: 'attractions',
  title: 'Attractions',
  summary: 'Shift overview, state commands, wait times, and when a supervisor is required.',
  category: 'attractions',
  sections: [
    {
      id: 'overview',
      title: 'Shift overview',
      body: [
        'The Attractions page lists every attraction with status, capacity mode, wait time, and version.',
        'Open an attraction to reach its command workspace. Weather recommendations for the shift sit on this same page.',
      ],
    },
    {
      id: 'commands',
      title: 'Attraction commands',
      body: [
        'Commands offer only transitions valid for the current status and capacity mode. You never pick a destination status directly.',
        'Select a command, read the description, add a reason when required, then confirm. After success, check the activity timeline for the receipt.',
        'Attraction state-change commands are supervisor-only in the UI. Operators can still open the workspace and update wait times when allowed.',
      ],
    },
    {
      id: 'wait-times',
      title: 'Wait times',
      body: [
        'Posted wait time can be updated while the attraction is in an operating state.',
        'Enter minutes (0–maximum allowed), submit, and confirm the posted value matches what guests should see.',
        'If wait time controls are hidden, the attraction is not in a state that accepts wait updates.',
      ],
    },
    {
      id: 'supervisor',
      title: 'When a supervisor is required',
      body: [
        'State transitions such as testing, hold, reopen, and close require a supervisor.',
        'If you are an operator and a needed transition is unavailable, escalate to a supervisor rather than forcing a workaround.',
      ],
      roles: ['operator', 'supervisor'],
    },
  ],
};
