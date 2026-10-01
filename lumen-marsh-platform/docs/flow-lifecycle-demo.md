# Flow lifecycle demo (Park Flow → Best Next)

UI-driven walkthrough of the mangrove disruption → recommendation → supervisor publish → guest Best Next path. Automated HTTP proof: [`../scripts/flow-lifecycle-acceptance.sh`](../scripts/flow-lifecycle-acceptance.sh). Weather sibling: [`storm-lifecycle-demo.md`](./storm-lifecycle-demo.md). Maintenance sibling: [`maintenance-lifecycle-demo.md`](./maintenance-lifecycle-demo.md).

Park Flow Intelligence submits evidence. VenueOps remains the system of record. Automation never changes attraction status or capacity.

## Surfaces

| Surface | LOCAL_JWT | Cognito OIDC |
| --- | --- | --- |
| Control Tower dashboard | http://localhost:3001/dashboard | http://127.0.0.1:5173/dashboard |
| Control Tower Park Flow | http://localhost:3001/park-flow | http://127.0.0.1:5173/park-flow |
| Guest app | http://localhost:3000 | http://localhost:3000 |
| Park Flow Intelligence | http://localhost:8100 | http://localhost:8100 |
| VenueOps API | http://localhost:8080 | http://localhost:8080 |

## Prerequisites

1. Clean seed: `./scripts/reset-demo.sh --confirm` (or `--confirm --oidc`). Mangrove disruption reuses a stable recommendation id, so a spent database will not produce a new pending review.
2. Stack healthy: `./scripts/wait-for-ready.sh` (add `--oidc` for Cognito).
3. Control Tower signed in:
   - After authentication, the landing page is **Dashboard** (`/dashboard`).
   - OIDC: a **supervisor** for publish. An **operator** can approve or dismiss, but **Publish** stays hidden without `venueops/flow.publish`.
   - LOCAL_JWT: the console uses `local-development-token` (supervisor-equivalent).
4. Guest app open on **Today** (Best Next).
5. Optional HTTP companion (does not replace the UI path):

```bash
# LOCAL_JWT
./scripts/flow-lifecycle-acceptance.sh

# OIDC — export tokens from the signed-in browser session. Never paste tokens into chat.
SUPERVISOR_TOKEN=... OPERATOR_LIMITED_TOKEN=... FLOW_TOKEN=... \
  ./scripts/flow-lifecycle-acceptance.sh
```

## UI checklist

Work left-to-right: Park Flow simulation → Control Tower → Flutter.

### 1. Simulate mangrove disruption

- [ ] `POST http://localhost:8100/simulation/scenarios/mangrove-disruption` (empty JSON body).
- [ ] Control Tower **Dashboard** unpublished-flow metric and Needs attention update without a manual refresh.
- [ ] Control Tower **Park Flow** shows a pending `GUEST_REDIRECTION` recommendation. Mangrove Run boarding is simulated down; Cypress Coil and Stormglass Station are the suggested destinations.

### 2. Operator reviews the recommendation

- [ ] Open the recommendation. Explanation, confidence, and related operations stay on the operator card.
- [ ] Attraction status and capacity are unchanged.
- [ ] **Approve** → **Confirm command**. Status is **Approved**. Guest Best Next is still empty.

### 3. Operator cannot publish

- [ ] Operator session: Publish is hidden, or HTTP with `OPERATOR_LIMITED_TOKEN` / `local-operator-token` returns `403`.

### 4. Supervisor publishes guest guidance

- [ ] Supervisor **Publish** with a guest message (default copy is fine).
- [ ] Status is **Published**. Dashboard unpublished-flow attention for this item is gone.

### 5. Guest Best Next

- [ ] Flutter **Today** shows **Best Next Experiences** with Cypress Coil / Stormglass Station and the published guest message.
- [ ] Guest REST `GET /api/v1/flow/overview` and `GET /api/v1/flow/recommendations` contain only allowlisted fields.
- [ ] Guest wait-forecast has no `queueLength`, `confidence`, `explanation`, `actor`, or `relatedIncidentId`.

## Failure checks (do not skip)

| Check | How | Expected |
| --- | --- | --- |
| Stale version | Confirm dismiss/approve with `expectedVersion` 99 | `409` / `STALE_VERSION` |
| Unauthorized publish | Operator session or `OPERATOR_LIMITED_TOKEN` | Button hidden or `403` |
| Unauthenticated command | Sign out, or omit `Authorization` | Login wall / `401` |
| commandId replay | Repeat approve with the same `commandId` | Same `APPROVED` snapshot; no second activity |
| Service restart | `docker compose -p lumen-marsh restart venueops-api` | Health returns; published guidance persists |
| Guest leak | `GET /api/v1/flow/overview`, `/flow/recommendations`, wait-forecast, guest SSE | No `explanation`, `confidence`, `actor`, `queueLength`, `relatedIncidentId`, `INTERNAL` |

## Seed / reset

```bash
./scripts/reset-demo.sh --confirm           # LOCAL_JWT clean seed
./scripts/reset-demo.sh --confirm --oidc    # Cognito overlay + new volumes
```

## Pass criteria

- The UI steps complete on a clean seed without database edits.
- `./scripts/flow-lifecycle-acceptance.sh` exits 0 on the same stack (LOCAL_JWT, or OIDC with exported tokens).
- Guest REST and SSE never carry operator flow internals.
- Audit actors come from the verified token, not a forged `X-Actor`.
