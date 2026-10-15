# Cutaway

Local booth for Lumen Marsh. The guest room and Control Tower are open at the same time, and the producer cues sit outside both rooms. Cutaway is not a park domain, not a seventh application, and not a public host.

Weather walkthrough: [`storm-lifecycle-demo.md`](./storm-lifecycle-demo.md). Cypress Coil: [`maintenance-lifecycle-demo.md`](./maintenance-lifecycle-demo.md). Park Flow: [`flow-lifecycle-demo.md`](./flow-lifecycle-demo.md).

## What it is

One static page in the local Compose env (`compose.override.yaml` only). `compose.yaml` does not start it.

- Left: guest app, **Today** (Best Next lives there)
- Right: Control Tower, the existing local supervisor session
- Rail, outside both frames: four producer triggers
- Strip under the frames: the last VenueOps recommendation, and whether the guest projection changed

The rail fires producers only. Place hold, accept, dismiss, and publish stay inside Control Tower. The strip reads VenueOps, not the producer logs: pending weather, pending reliability, and unpublished flow on the operator side; guest advisory and Best Next on the guest side.

## How to open it

1. From `lumen-marsh-platform`, `cp .env.example .env` and set `NWS_USER_AGENT`.
2. `./scripts/dev-up.sh` (LOCAL_JWT). `./scripts/dev-up.sh --oidc` does not start Cutaway.
3. Open <http://localhost:3090>.

The console iframe is the local supervisor session (`local-development-token`). There is no Cognito step on this page. If you open `http://127.0.0.1:3090` instead, the local console image must have been built with that origin in `CONSOLE_FRAME_ANCESTORS` (the override default includes both).

Control Tower’s production nginx sends `X-Frame-Options: DENY` and `frame-ancestors 'none'`. The local image allows only the Cutaway origin. `docker compose -f compose.yaml` does not pass that exception.

| Surface | URL |
| --- | --- |
| Cutaway | http://localhost:3090 |
| Guest Today | http://localhost:3000/today |
| Control Tower dashboard | http://localhost:3001/dashboard |
| VenueOps API | http://localhost:8080 |
| Environmental Monitor | http://localhost:8000 |
| Park Flow Intelligence | http://localhost:8100 |
| Reliability Intelligence | http://localhost:8200 |

## What each rail button does

Simulation has to be on. `compose.override.yaml` already enables it for local demos. Each button `POST`s an empty JSON body, the same call the lifecycle scripts use, and shows **Fired** or **Failed**.

| Rail button | Call | What should appear before anyone clicks in the tower |
| --- | --- | --- |
| Lightning hold | `POST http://localhost:8000/api/v1/simulation/scenarios/hold-conditions` | Pending weather on the strip and in Control Tower. Guest advisory stays quiet. |
| Clear the storm | `POST http://localhost:8000/api/v1/simulation/scenarios/clearance-period` | The monitor moves to the clearance scenario. Recovery commands still happen in Control Tower. |
| Cypress Coil vibration | `POST http://localhost:8200/simulation/scenarios/cypress-coil-vibration` | Pending reliability on the strip and in Control Tower. The guest app stays quiet. |
| Mangrove disruption | `POST http://localhost:8100/simulation/scenarios/mangrove-disruption` | Unpublished flow on the strip and in Control Tower. Best Next does not treat Mangrove as down. |

## What you still click

Cutaway does not hold a ride, accept a recommendation, dismiss one, or publish guest copy.

- **Lightning.** In Control Tower, review the weather recommendation, open the incident, and place the hold. **Publish guest advisory** is the click that lets Today show it. Until that publish, the strip’s guest advisory stays **Quiet** and the projection stays **Unchanged**. Full path: [`storm-lifecycle-demo.md`](./storm-lifecycle-demo.md).
- **Vibration.** Accept the reliability recommendation and work the order through inspection and testing. Reliability Intelligence never creates the work order. Guests still must not see work-order numbers, asset codes, or internal notes. Full path: [`maintenance-lifecycle-demo.md`](./maintenance-lifecycle-demo.md).
- **Mangrove.** Approve if you want, then **Publish** as a supervisor. Best Next stays empty until that publish. Full path: [`flow-lifecycle-demo.md`](./flow-lifecycle-demo.md).
- **Clear the storm.** Clearance is a producer scenario. Return to service is still an operator command in the tower.

## What the guest must not see

Until a supervisor publishes:

- No park advisory on Today after lightning. Pending weather is an operator card.
- No reliability internals after vibration. The guest projection stays unchanged.
- Best Next does not rank Mangrove Run down, and does not show the unpublished guest message, after mangrove disruption.

The strip is that split. Operator rows come from `GET /api/v1/operator/dashboard`. Guest rows come from `GET /api/v1/advisories` and `GET /api/v1/flow/recommendations`. A pending card with a quiet guest row means the publish has not happened.

## Spent mangrove

Mangrove disruption reuses one recommendation id. After that id is dismissed, published, or otherwise no longer a new pending review, another click will not open a new unpublished card.

The strip then shows a reset note. It is not a button. From `lumen-marsh-platform`:

```bash
./scripts/reset-demo.sh --confirm
./scripts/dev-up.sh
```

## Pass

On a stack started with `./scripts/dev-up.sh`:

- Cutaway shows Today and Control Tower together. The console frame is not blank.
- Lightning shows a pending weather recommendation in Control Tower without a reload. Today gains an advisory only after someone publishes one in the tower.
- Vibration does the same for the reliability row.
- Mangrove disruption does the same for unpublished flow. Best Next does not treat Mangrove as down until that guidance is published.
- A spent mangrove id shows the reset note and does not reset the databases by itself.
