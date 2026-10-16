import type { DocArticle } from '@/features/docs/domain/types';

export const shiftGuideArticle: DocArticle = {
  slug: 'shift',
  title: 'Shift guide',
  summary:
    'What to do when Needs attention shows pending weather, pending reliability, or unpublished flow.',
  category: 'shift',
  sections: [
    {
      id: 'pending-weather',
      title: 'Pending weather',
      body: [
        'A warning-level weather recommendation is waiting. Read the recommended action on the dashboard, then make the hold from the attraction workspace.',
        'Next click in Control Tower: open Attractions, then open the affected attraction. On the command panel, choose Place weather hold, enter the reason, and Confirm command. Place weather hold stays on that panel.',
        'Guests must not see the weather recommendation, its evidence, the inbox, or the operator who reviewed it until a supervisor publishes a guest advisory. Until that publish, the guest companion keeps the last published advisory.',
      ],
    },
    {
      id: 'pending-reliability',
      title: 'Pending reliability',
      body: [
        'A reliability recommendation is waiting for review. The signal and the recommended action stay in Control Tower.',
        'Next click in Control Tower: open Maintenance, then Accept recommendation on that card, and confirm with Create work order. Accept recommendation stays an operator action for a session with maintenance command scope. Acceptance creates a corrective work order. The attraction status stays as it is.',
        'Guests must not see the asset code, the vibration evidence, the recommended action, or the work-order number. Accepting the recommendation leaves guests on the current published projection until a supervisor publishes an advisory or park-flow message.',
      ],
    },
    {
      id: 'unpublished-flow',
      title: 'Unpublished flow',
      body: [
        'Park Flow has guidance guests have not been given. A recommendation can be pending or approved and still be unpublished.',
        'Next click in Control Tower: open Park Flow. While the recommendation is pending, an operator with flow command chooses Approve or Dismiss. When it is approved, a supervisor with flow publish chooses Publish, reviews the guest message, and confirms Publish. Publish stays that supervisor action.',
        'Guests must not see the operator explanation, the recommendation summary, or a Best Next change until a supervisor publishes. Best Next follows only the published guest message. When that message says an attraction is unavailable, Best Next does not rank it as a good next choice.',
      ],
    },
  ],
};
