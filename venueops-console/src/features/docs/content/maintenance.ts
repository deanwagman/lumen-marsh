import type { DocArticle } from '@/features/docs/domain/types';

export const maintenanceArticle: DocArticle = {
  slug: 'maintenance',
  title: 'Maintenance',
  summary:
    'Review reliability recommendations, work a corrective order, and hand the attraction back to Operations for testing.',
  category: 'maintenance',
  sections: [
    {
      id: 'lifecycle',
      title: 'Work-order lifecycle',
      body: [
        'Work orders move Draft → Open → Assigned → In progress → Awaiting inspection → Ready for testing → Completed. Cancel is available on non-terminal states. A note can still be added after completion or cancellation.',
        'The console only offers commands that are valid for the current status, role, and scopes. The API remains the authority if two operators act at once.',
      ],
    },
    {
      id: 'priority',
      title: 'Priority meanings',
      body: [
        'P1 is an immediate safety or ride-stopping fault. P2 is urgent degradation. P3 and P4 are scheduled or routine work.',
        'P1 and P2 cancellation is supervisor-controlled. Inspection approval, rejection, and completion also require a supervisor with maintenance inspect scope.',
      ],
    },
    {
      id: 'reliability',
      title: 'Reliability recommendation review',
      body: [
        'The reliability inbox lists pending telemetry recommendations such as an abnormal Cypress Coil vibration signal.',
        'Accepting a pending recommendation creates a corrective work order. Dismissing it requires a short reason so the trail explains why no work was opened.',
      ],
    },
    {
      id: 'assignment',
      title: 'Assignment',
      body: [
        'Assign a team and optional technician subject after the work order is open. Reassign when ownership changes. Estimated restoration time is an operations planning field, not a guest-facing promise.',
      ],
    },
    {
      id: 'checklists',
      title: 'Checklists',
      body: [
        'Required checklist items must be passed or marked not applicable before inspection can be requested or approved. Failed required items also block approval.',
        'Checklist updates use the current work-order version from the server. Do not record results against a stale copy.',
      ],
    },
    {
      id: 'inspection',
      title: 'Inspection approval',
      body: [
        'Request inspection after required checklist items are resolved. Supervisors with maintenance inspect scope approve or reject.',
        'Approval moves the work order to Ready for testing. It does not start attraction testing and never reopens the attraction.',
      ],
    },
    {
      id: 'handoff',
      title: 'Attraction testing handoff',
      body: [
        'A work order can recommend an attraction action without performing it. Ready for testing recommends Start testing. Active P1 corrective work can recommend Report technical fault.',
        'Start testing from the handoff card or the attraction workspace. Complete testing and approve return to service remain Operations and supervisor attraction commands.',
        'Maintenance cannot reopen an attraction. Return-to-service approval belongs to the attraction workflow so Control Tower does not skip testing.',
      ],
    },
    {
      id: 'concurrency',
      title: 'Stale-version behavior',
      body: [
        'Every command sends a client-generated command ID and the expected version. Retries of the same intent reuse the command ID. A new intent gets a new identifier.',
        'If another operator changed the record, the console refreshes it and does not automatically resubmit. Review the latest state, then decide whether to send the command again.',
      ],
    },
    {
      id: 'scopes',
      title: 'Required scopes and supervisor actions',
      body: [
        'maintenance.read opens the workspace. maintenance.command issues most work-order and recommendation commands. Inspection approval, rejection, and completion require maintenance.inspect and ROLE_SUPERVISOR.',
        'Read-only operators can inspect maintenance information without seeing enabled command controls they are not allowed to use.',
      ],
    },
  ],
};
