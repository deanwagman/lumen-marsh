# Lumen Marsh Incident Center

Status: Implemented  
Owner: VenueOps Console  
Supporting systems: VenueOps API, Environmental Monitor, Flutter guest app  
Shipped with: weather recommendation → incident → attraction hold → guest advisory ([storm-lifecycle-demo.md](./storm-lifecycle-demo.md))

## 1. Purpose

The Incident Center gives Control Tower operators one place to receive, coordinate, communicate, and resolve operational incidents. Its first complete vertical slice begins with a weather recommendation, creates a weather incident, coordinates attraction response, publishes a guest advisory, and records the full operator history.

The system supports human decisions. Weather recommendations never issue attraction commands automatically, and linking an incident to an attraction never changes that attraction's state.

## 2. Goals

- Let operators find active incidents and understand their current state quickly.
- Provide only the commands valid for the current incident state and authenticated capability set.
- Preserve the existing VenueOps API state machine and optimistic concurrency contract.
- Make weather-created incidents navigable from the recommendation inbox.
- Publish and withdraw guest advisories through an explicit supervisor action.
- Update every open operator console through the existing server-sent event stream.
- Show published advisories in the Flutter guest app without exposing internal incident details.
- Maintain an immutable, readable activity timeline for operational review.

## 3. Non-goals

- Automatically placing attractions on weather hold.
- Automatically publishing guest communications.
- Replacing emergency services, life-safety systems, or maintenance work-order systems.
- File uploads, chat, paging, staff scheduling, or push notifications in this milestone.
- Editing historical activity or changing the original reporter identity.
- Production-grade dispatch or escalation policy management.

## 4. Users and authorization

### Operator

An authenticated member of the Cognito `operators` group.

- Read incidents with `venueops/operator.read`.
- Report incidents and perform standard incident commands with `venueops/incidents.command`.
- Acknowledge and manage weather recommendations with `venueops/weather-recommendations.review`.
- Cannot publish guest advisories.
- Cannot resolve `MAJOR` or `CRITICAL` incidents.

### Supervisor

An authenticated member of the Cognito `supervisors` group. Supervisor membership also grants the API's operator role.

- Receives all operator capabilities when the corresponding scopes are present.
- Publishes or withdraws guest advisories with `venueops/advisories.publish`.
- Resolves `MAJOR` and `CRITICAL` incidents with `venueops/incidents.command`.

### UI authorization rule

The UI must derive action visibility from both OAuth scopes and role requirements. It must not use role alone as a proxy for capabilities.

| Capability | Required scope | Additional role |
| --- | --- | --- |
| View incident list/detail/activity | `venueops/operator.read` | Operator or supervisor |
| Report incident | `venueops/incidents.command` | Operator or supervisor |
| Acknowledge, assign, start mitigation, change severity, link/unlink attraction | `venueops/incidents.command` | Operator or supervisor |
| Resolve `MINOR` or `MODERATE` | `venueops/incidents.command` | Operator or supervisor |
| Resolve `MAJOR` or `CRITICAL` | `venueops/incidents.command` | Supervisor |
| Publish/withdraw guest advisory | `venueops/advisories.publish` | Supervisor |

Unavailable high-impact actions should be hidden when the user lacks the required capability. When an action is unavailable because of incident state, the workspace should explain the required next state rather than presenting a disabled wall of buttons.

## 5. Incident lifecycle

```text
REPORTED --ACKNOWLEDGE--> ACKNOWLEDGED --START_MITIGATION--> MITIGATING --RESOLVE--> RESOLVED
```

The state machine is owned by VenueOps API. The console presents only valid transitions:

| Current state | Primary transition | Result |
| --- | --- | --- |
| `REPORTED` | Acknowledge | `ACKNOWLEDGED` |
| `ACKNOWLEDGED` | Start mitigation | `MITIGATING` |
| `MITIGATING` | Resolve with reason | `RESOLVED` |
| `RESOLVED` | None | Terminal, read-only |

The following commands do not change lifecycle status but are allowed while the incident remains open, subject to API validation:

- Assign
- Change severity
- Link attraction
- Unlink attraction
- Publish guest advisory
- Withdraw guest advisory

Every successful command increments `version`. Every command submits `expectedVersion`; stale writes return a version-conflict response and must never be silently retried.

## 6. Information architecture

### Navigation

Add **Incidents** below **Attractions** in the console rail.

- Route: `/incidents`
- Detail route: `/incidents/:incidentId`
- Show a compact badge containing the count of non-resolved incidents.
- A weather recommendation's linked incident identifier becomes a link to its detail route.

### Incident list

The list prioritizes current operational work.

Header summary:

- Open incidents
- Critical or major incidents
- Unassigned incidents
- Published guest advisories
- Last synchronized time

Default ordering:

1. Open before resolved.
2. Severity: critical, major, moderate, minor.
3. Most recently updated first.

Filters:

- Status: open, reported, acknowledged, mitigating, resolved
- Severity
- Type
- Assignment: all, assigned, unassigned
- Attraction

Each row shows:

- Title and incident type
- Severity and lifecycle status
- Assignee or **Unassigned**
- Linked attraction names
- Guest-advisory state
- Updated time
- Version

Selecting a row opens the incident detail workspace. Filters should be represented in URL query parameters so refresh and browser navigation preserve the view.

### Incident detail

The workspace uses the established attraction-detail layout:

- Back link to Incidents
- Incident title
- Severity and status badges
- Type, assignee, created time, updated time, and version
- Internal description
- Linked weather recommendation, when discoverable from the loaded recommendation inbox
- Linked attractions as routes to `/attractions/:id`
- Guest advisory preview and publication status
- Contextual command panel
- Immutable activity timeline

## 7. Commands and interaction design

### Report incident

Provide a **Report incident** action on the list page for users with incident-command scope.

Required fields:

- Title
- Type: weather, technical, medical, operations, guest experience
- Severity: minor, moderate, major, critical

Optional fields:

- Internal description
- Linked attractions

After creation, navigate to the new incident detail and announce the successful report through the existing command receipt pattern.

### Acknowledge

- Available only in `REPORTED`.
- Optional reason.
- On success, status becomes `ACKNOWLEDGED`.

### Assign

- Available on any open incident.
- Requires a non-empty assignee.
- MVP uses a free-text team or operator label; directory-backed assignment is deferred.

### Start mitigation

- Available only in `ACKNOWLEDGED`.
- Optional reason.
- On success, status becomes `MITIGATING`.

### Change severity

- Available on any open incident.
- Requires a different severity and a reason.
- Present old and new severity in the confirmation summary.

### Link or unlink attractions

- Link is available for attractions not already associated with the incident.
- Unlink requires a reason.
- Clearly state that linking does not change attraction operational state.
- Provide a separate **Open attraction workspace** link for any required hold, delay, or recovery command.

### Publish guest advisory

- Supervisor-only and requires advisory-publish scope.
- Available on open incidents.
- Requires a guest-facing title and message.
- Show a preview that contains no internal description, evidence, operator identity, or incident activity.
- Require explicit confirmation before publishing.
- On success, show **Published** and the exact guest-visible copy.

### Withdraw guest advisory

- Supervisor-only and requires advisory-publish scope.
- Available only while an advisory is published.
- Requires a reason and explicit confirmation.

### Resolve

- Available only in `MITIGATING`.
- Requires a reason.
- Major and critical incidents require supervisor role.
- Confirmation summarizes unresolved operational context, including linked attractions and any published advisory.
- Resolving also removes the incident's advisory from the active guest feed, per the existing domain invariant.

### Failure behavior

- `401`: expire the local session and route to the session-expired experience.
- `403`: show access denied without discarding loaded incident data.
- `404`: show incident not found with a route back to the list.
- Stale version: retain entered form values, show current and submitted versions, and offer **Reload incident**.
- Invalid transition: refresh detail and explain which state changed.
- Network/server failure: retain entered values and offer retry.
- Disable duplicate submission while a command is pending.

## 8. Live update contract

### Existing API usage

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/operator/incidents` | List incidents |
| `POST` | `/api/v1/operator/incidents` | Report incident |
| `GET` | `/api/v1/operator/incidents/{id}` | Incident detail |
| `GET` | `/api/v1/operator/incidents/{id}/activity` | Immutable activity |
| `POST` | `/api/v1/operator/incidents/{id}/commands` | Execute lifecycle and supporting commands |

The existing request and response payloads remain authoritative. TypeScript schemas must reject malformed API responses at the client boundary.

### Required operator SSE extension

Extend `GET /api/v1/operator/events` with:

- `incidents.snapshot` immediately after connection
- `incident.reported`
- `incident.updated`
- `incident.resolved`

Each incremental event carries the existing `IncidentOperationalUpdate`, including event ID, event type, occurrence time, and the full current incident snapshot. Incident events are operator-only and must never be added to the public guest stream.

The console event handler should upsert the incident in the list cache, update the matching detail cache, invalidate that incident's activity query, and announce material status changes accessibly. Reconnection receives a fresh snapshot; replay is not required for this milestone.

### Guest advisory propagation

The existing public guest-advisory API and SSE stream remain the only incident-derived data available to the Flutter app. A guest receives only the published title, message, affected attraction identifiers, and public timestamps/status already present in the guest contract.

## 9. Client architecture

Create a vertical feature module:

```text
src/features/incidents/
  api/
    IncidentCommands.ts
    IncidentQueries.ts
    incidentSchema.ts
  components/
    IncidentFilters.tsx
    IncidentList.tsx
    IncidentCommandPanel.tsx
    GuestAdvisoryComposer.tsx
    IncidentActivityTimeline.tsx
  domain/
    incident.ts
    commands.ts
    formatters.ts
    presentation.ts
  hooks/
    useIncidents.ts
    useIncidentDetail.ts
    useIncidentActivity.ts
    useIncidentCommand.ts
    useIncidentParams.ts
  pages/
    IncidentsPage.tsx
    IncidentDetailPage.tsx
```

Use TanStack Query keys rooted at `['incidents']`. Command success updates detail and list caches immediately; SSE remains authoritative for other consoles. Domain helpers own sorting, filtering, valid transitions, action authorization, and presentation labels so components do not duplicate business rules.

## 10. Visual and motion behavior

- Reuse the existing Lumen Marsh tokens, buttons, badges, panels, and command receipt.
- Severity is visually distinct from lifecycle status; never communicate either with color alone.
- Use spring motion only for contextual transitions: a newly reported row entering, a detail command panel expanding, or an updated row briefly emphasizing.
- Respect `prefers-reduced-motion` and remove nonessential movement when enabled.
- Avoid moving list rows while keyboard focus is inside a row; defer reordering until focus leaves or provide a stable update affordance.

## 11. Accessibility requirements

- All commands and forms are keyboard operable with visible focus.
- Status changes use a polite live region; command failures use an assertive alert.
- Confirmation dialogs trap focus, have descriptive headings, and return focus to the invoking control.
- Every input has a programmatic label and associated validation text.
- Badge text states the full severity/status independently of color.
- Activity is a semantic ordered list with actor, action, time, reason, and resulting version.
- Target WCAG 2.2 AA contrast and interaction behavior.

Delivery phases 1–5 below shipped with the incident center, operator SSE, and storm lifecycle. Keep this section as the original slice definition.

## 12. Delivery phases

### Phase 1 — Read-only Incident Center

- Add incident domain types and Zod schemas.
- Add list, detail, and activity queries.
- Add Incidents navigation, list page, filters, and detail page.
- Link weather recommendations to incident detail.
- Cover loading, empty, error, not-found, and resolved states.

Exit criterion: an operator can navigate from a linked weather recommendation to a complete, read-only incident record and activity history.

### Phase 2 — Operator lifecycle commands

- Add report, acknowledge, assign, start-mitigation, severity, attraction-link, attraction-unlink, and permitted resolve flows.
- Drive visibility from scopes, role, severity, and lifecycle state.
- Implement optimistic-version conflict recovery and command receipts.

Exit criterion: an operator can manage a minor/moderate incident from report through resolution without direct API calls.

### Phase 3 — Supervisor guest communications

- Add advisory composer, preview, publish, and withdraw flows.
- Add supervisor-only major/critical resolution.
- Add or verify a local Cognito supervisor test account.
- Verify the Flutter app adds and removes the advisory through public SSE.

Exit criterion: a supervisor can publish guest-safe incident messaging and observe it appear in the Flutter app without refresh.

### Phase 4 — Multi-console live updates

- Publish incident snapshot and incremental events from VenueOps API.
- Extend the console SSE client and caches.
- Add connection, reconnection, stale-state, and concurrent-update tests.

Exit criterion: commands issued in one console update a second open console, including list, detail, count badge, and activity.

### Phase 5 — Integrated weather scenario

- Exercise clear → hold conditions → recommendation → incident → attraction hold → advisory → clearance → recovery → resolution.
- Confirm weather evidence remains labeled simulated.
- Confirm no service identity can call operator command endpoints.
- Capture the workflow for portfolio documentation.

Exit criterion: the scenario completes using UI actions after the simulation trigger, with correct audit actors, versions, SSE updates, and guest-visible messaging.

## 13. Test strategy

### Domain and authorization

- Sorting and filtering priorities.
- Valid state transitions per lifecycle state.
- Capability checks for operator and supervisor sessions.
- Major/critical resolution restrictions.
- Guest advisory publish restrictions.

### API client

- Zod parsing for incident and activity responses.
- Request bodies for every command.
- Problem-detail mapping for stale version and invalid transition.

### Components and routes

- List/loading/empty/error/filter states.
- Detail/not-found/resolved states.
- Form validation and retained input after errors.
- Weather recommendation to incident navigation.
- Keyboard navigation, focus restoration, and live announcements.

### Live updates

- Snapshot replacement.
- Report/update/resolve cache upserts.
- Out-of-order event protection using version.
- Activity invalidation.
- Reconnection snapshot convergence.

### Integrated acceptance

Use separate operator and supervisor Cognito sessions:

1. Trigger fictional `hold-conditions` in Environmental Monitor.
2. Operator acknowledges the pending recommendation.
3. Operator creates and links a major weather incident.
4. Operator assigns it and starts mitigation.
5. Operator places affected attractions on weather hold through their workspaces.
6. Supervisor publishes a guest advisory.
7. Flutter receives the advisory and attraction changes live.
8. Trigger fictional `clearance-period`.
9. Operator performs attraction recovery.
10. Supervisor resolves the major incident with a reason.
11. Flutter removes the resolved incident's advisory.
12. Activity histories show immutable Cognito subjects and monotonic versions.

## 14. Definition of done

- All five phases' exit criteria pass locally in Cognito OIDC mode.
- Console type-check, lint, unit, component, and accessibility tests pass.
- VenueOps API incident and SSE tests pass.
- Flutter guest advisory tests pass.
- No command is made automatically from a weather recommendation or incident link.
- Every mutation is authorized, audited, versioned, and protected against duplicate submission.
- Operator-only incident data never appears in public endpoints or guest SSE.
- The documented integrated scenario can be demonstrated from a clean local stack.

