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
        'Open an attraction to reach its command workspace. Weather recommendations for the shift sit on this same page when the session has venueops/weather-recommendations.review.',
      ],
    },
    {
      id: 'commands',
      title: 'Attraction commands',
      body: [
        'Commands offer only transitions valid for the current status and capacity mode. You never pick a destination status directly.',
        'The panel buttons are Start testing, Place weather hold, Clear weather hold, Report technical fault, Complete repair, Complete testing, Approve return to service, Close for day, Reduce capacity, and Restore capacity. Select one, read the description, add a reason when the form marks it required, then Confirm command. Place weather hold, Report technical fault, Close for day, and Reduce capacity require a reason. After success, check the activity timeline for the receipt.',
        'Clear weather hold returns the ride to testing. Complete repair returns a technical delay to testing. Return to service is then Complete testing, then Approve return to service.',
        'Every attraction command sends a client-generated command ID and the expected version. Retries of the same intent reuse the command ID. A new intent gets a new identifier.',
        'The command panel is on screen when the signed-in role is supervisor. Operators can still open the workspace and update wait times while the attraction is operating.',
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
        'Supervisors see the command panel: Start testing, Place weather hold, Clear weather hold, Report technical fault, Complete repair, Complete testing, Approve return to service, Close for day, Reduce capacity, and Restore capacity, limited to the transitions valid right now.',
        'A supervisor who also has venueops/attractions.command sees Start attraction testing or Report technical fault on a work-order handoff when that command is valid for the ride. The dialog submits Start testing or Report fault. Complete testing and Approve return to service stay on this workspace.',
        'Operators keep the workspace and wait-time updates. The API still accepts every attraction command, including the ones this screen reserves for supervisors, from a token with venueops/attractions.command. No attraction command is supervisor-only on the API. Ask a supervisor to use the screen when the panel or handoff button is hidden.',
      ],
      roles: ['operator', 'supervisor'],
    },
  ],
};
