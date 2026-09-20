# Storm lifecycle demo (Control Tower + Flutter)

UI-driven walkthrough of the integrated weather hold → advisory → recovery path. Automated HTTP proof: [`../scripts/storm-lifecycle-acceptance.sh`](../scripts/storm-lifecycle-acceptance.sh). Guest contract: [`guest-advisory-contract.md`](./guest-advisory-contract.md). Cypress Coil maintenance uses the same stack and leak/authz style: [`maintenance-lifecycle-demo.md`](./maintenance-lifecycle-demo.md).

This is the portfolio demo. Do not start the [AWS public deployment gate](./security/public-deployment-gate.md) until this pass is reliable.

## Surfaces

| Surface | LOCAL_JWT | Cognito OIDC |
| --- | --- | --- |
| Control Tower | http://localhost:3001 | http://127.0.0.1:5173 |
| Control Tower dashboard | http://localhost:3001/dashboard | http://127.0.0.1:5173/dashboard |
| Guest app | http://localhost:3000 | http://localhost:3000 |
| Environmental Monitor | http://localhost:8000 | http://localhost:8000 |
| VenueOps API | http://localhost:8080 | http://localhost:8080 |

## Prerequisites

1. Clean seed: `./scripts/reset-demo.sh --confirm` (or `--confirm --oidc`).
2. Stack healthy: `./scripts/wait-for-ready.sh` (add `--oidc` for Cognito).
3. Control Tower signed in:
   - After authentication, the landing page is **Dashboard** (`/dashboard`).
   - OIDC: a **supervisor** (`./scripts/create-dev-supervisor.sh`) for publish/resolve of MAJOR incidents. An **operator** (`./scripts/create-dev-operator.sh`) is enough for acknowledge / assign / hold, but **Publish guest advisory** stays hidden.
   - LOCAL_JWT: the console uses `local-development-token` (supervisor-equivalent).
4. Guest app open on **Today**.
5. Optional HTTP companion (does not replace the UI path):

```bash
# LOCAL_JWT
./scripts/storm-lifecycle-acceptance.sh

# OIDC — export tokens from the signed-in browser session. Never paste tokens into chat.
SUPERVISOR_TOKEN=... OPERATOR_LIMITED_TOKEN=... ./scripts/storm-lifecycle-acceptance.sh
```

## Screenshot slots

Capture these frames (or a single screen recording) into `docs/demo/screenshots/`:

| File | Moment |
| --- | --- |
| `00-console-login.png` | Control Tower sign-in wall (OIDC) |
| `00-cognito-hosted-ui.png` | Cognito hosted UI |
| `01-weather-inbox.png` | Weather recommendation after simulated hold |
| `02-incident-workspace.png` | Incident acknowledged, assigned, mitigating |
| `03-weather-hold.png` | Mangrove Run / Cypress Coil on weather hold |
| `04-publish-advisory.png` | Supervisor publish dialog |
| `05-guest-today.png` | Flutter **Park advisories** banner |
| `06-guest-detail.png` | Advisory detail — title/message only |
| `07-clearance.png` | Recommendation status **CLEARED** |
| `08-restored.png` | Attractions operating, incident resolved |
| `09-guest-cleared.png` | Flutter Today with advisory gone |
| `10-audit.png` | Incident activity: authenticated actor, increasing versions |

Frames `00-*`, `05`, and `06` are checked in under `docs/demo/screenshots/`. Capture `01`–`04` and `07`–`10` after a supervisor sign-in on a clean seed.

## UI checklist (about 12 minutes)

Work left-to-right: Monitor simulation → Control Tower → Flutter.

The dashboard is the operator landing page. Confirm metrics and Needs attention as you progress; commands still happen in Attractions and Incidents.

### 1. Environmental Monitor generates hold conditions

- [ ] `POST http://localhost:8000/api/v1/simulation/scenarios/hold-conditions` (empty JSON body), **or** run the monitor demo helper if you prefer the HTTP-only path.
- [ ] Control Tower **Dashboard** pending-weather and Needs attention update without a manual refresh (second console tab optional).
- [ ] Control Tower **Attractions** shows **Weather recommendations** with an ACTIVE hold card (lightning / outdoor attractions).

### 2. Operator reviews the weather recommendation

- [ ] Open the recommendation. Evidence and recommended action are visible.
- [ ] Affected attractions include Mangrove Run and Cypress Coil (or the seeded outdoor set).

### 3. Operator creates, acknowledges, assigns, and mitigates an incident

- [ ] **Create incident** → **Submit incident**. Internal description stays on the operator incident only.
- [ ] Open the incident. **Acknowledge** → **Confirm command**.
- [ ] **Assign** to `Control Tower` → **Confirm command**.
- [ ] **Start mitigation** → **Confirm command**.
- [ ] Status is **Mitigating**. Attraction operational state is still unchanged.

### 4. Operator places affected attractions on weather hold

- [ ] Open Mangrove Run. **Place weather hold** → reason `Lightning detected within operating radius` → **Confirm command**.
- [ ] Repeat for Cypress Coil.
- [ ] Shift overview / attraction rows show weather hold. Flutter catalog status updates live.

### 5. Supervisor publishes a guest advisory

- [ ] Sign in as supervisor if the current session is operator-only (**Publish guest advisory** is absent for operators).
- [ ] On the incident: **Guest title** `Outdoor weather pause`, **Guest message** `Some outdoor attractions are temporarily paused.`
- [ ] **Publish guest advisory**. Confirm the composer shows the published copy, not the internal description.

### 6. Flutter receives the advisory and attraction changes live

- [ ] Today shows a **Park advisories** banner without refreshing the browser if the park SSE is connected.
- [ ] Open the banner → detail shows title, message, severity, affected attractions.
- [ ] **View all advisories** lists the same item.
- [ ] Confirm the guest UI never shows internal description, assignee, actor, or activity.

### 7. Monitor generates clearance conditions

- [ ] `POST http://localhost:8000/api/v1/simulation/scenarios/clearance-period`
- [ ] Control Tower recommendation moves to **CLEARED**.

### 8. Operator restores attractions and resolves the incident

- [ ] Per held attraction: **Clear weather hold** → **Complete testing** → **Approve return to service**.
- [ ] Supervisor **Resolve** with reason `Storm cell moved out of radius` → **Confirm command**.
- [ ] Incident is read-only. Guest advisory composer is no longer publishing.
- [ ] Dashboard open-incident and weather-hold counts return to the recovered state.
- [ ] Optional: **Generate shift handoff**, copy or print, then close. The summary is not stored.

### 9. Flutter removes the advisory automatically

- [ ] Today banner / inbox clear (a brief “cleared” state is acceptable).
- [ ] Stale `/advisories/:id` shows **Advisory cleared**.
- [ ] `GET /api/v1/advisories` is empty for this title.

### 10. Audit histories show authenticated actors and monotonic versions

- [ ] Incident **activity**: actor is the signed-in operator/supervisor display name (never a forged `X-Actor` header).
- [ ] Versions increase by 1 on each command (1, 2, 3, …).
- [ ] Attraction activity for the hold/clear cycle matches the same actor rule.

## Failure checks (do not skip)

| Check | How | Expected |
| --- | --- | --- |
| Disconnected SSE | Stop the API or toggle offline, then restore | Control Tower reconnects; Flutter/console recover from a fresh snapshot, not replayed history |
| Stale version | Confirm a command twice, or change the attraction in another tab first | `409` / “state changed”; retry with the current version succeeds |
| Unauthorized publish | Operator session on **Publish guest advisory**, or HTTP with `OPERATOR_LIMITED_TOKEN` / `local-operator-token` | Button hidden or `403` |
| Unauthenticated command | Sign out, or omit `Authorization` | Login wall / `401` |
| Service restart | `docker compose -p lumen-marsh restart venueops-api` | Health returns; incident + advisory + attraction state persist |
| Guest leak | `GET /api/v1/advisories` and guest SSE while published | No `internalDescription`, `assignedTo`, `actor`, `activity`, `activities`, `reason` |

## Seed / reset

```bash
./scripts/reset-demo.sh --confirm           # LOCAL_JWT clean seed
./scripts/reset-demo.sh --confirm --oidc    # Cognito overlay + new volumes
```

Reset deletes only `lumen-marsh_venueops-data` and `lumen-marsh_environmental-data`, then runs `dev-up`. Attractions must seed (`GET /api/v1/attractions` non-empty) before the next take.

## Pass criteria

- All ten UI steps complete on a clean seed without database edits.
- `./scripts/storm-lifecycle-acceptance.sh` exits 0 on the same stack (LOCAL_JWT, or OIDC with exported tokens).
- Guest REST and SSE never carry internal incident data.
- Audit actors come from the verified token, and versions are monotonic.
- Screenshots or a short walkthrough video exist for the ten slots above.

When that is true, the next milestone is [docs/security/public-deployment-gate.md](./security/public-deployment-gate.md).
