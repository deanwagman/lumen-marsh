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
        '/maintenance lists reliability recommendations and work orders. Commands, checklists, inspection, and the attraction handoff run on /maintenance/work-orders/:workOrderId.',
        'The asset code on the work order opens /maintenance/assets/:assetId. That page shows the asset code, service status, and that asset’s work orders. It does not issue commands.',
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
        'The reliability inbox lists pending telemetry recommendations such as an abnormal Cypress Coil vibration signal. The same active Cypress Coil vibration stays one inbox row: additional samples coalesce until an operator accepts or dismisses. A later reactivation can open a new recommendation.',
        'Reliability Intelligence posts at most one sample per vibration episode. Re-applying cypress-coil-vibration while that episode is active does not stack another warning. Clear, then apply the scenario again, to start a new episode.',
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
        'A work order can recommend an attraction action without performing it. Ready for testing recommends Start testing. Active P1 corrective work can recommend Report technical fault. The recommendation does not change attraction status.',
        'On the work order, Start attraction testing is shown to any session with venueops/attractions.command when the ride can start testing. The dialog submits Start testing. Report technical fault is shown to that same session when that is the recommendation and the ride can take it. The dialog submits Report fault. Those checks do not require a supervisor role.',
        'The card still names the recommended command and links to the attraction workspace. The attraction workspace command panel is shown when the signed-in role is supervisor. Complete testing and Approve return to service stay on that panel, so a handoff start still finishes there.',
        'The API still accepts those attraction commands from any token with venueops/attractions.command. No attraction command is supervisor-only. Return-to-service approval stays on the attraction workflow so testing is not skipped.',
        'Scripted HTTP proof: from lumen-marsh-platform, ./scripts/maintenance-lifecycle-acceptance.sh after the stack is up.',
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
