# Maintenance lifecycle demo (Cypress Coil)

HTTP proof of the integrated reliability ingest → accept → inspect → operations testing path. Automated script: [`../scripts/maintenance-lifecycle-acceptance.sh`](../scripts/maintenance-lifecycle-acceptance.sh). Weather sibling: [`storm-lifecycle-demo.md`](./storm-lifecycle-demo.md). Park Flow sibling: [`flow-lifecycle-demo.md`](./flow-lifecycle-demo.md).

This is not a second product. VenueOps remains the system of record. Reliability Intelligence can post ingest locally; this script posts the same payload the Java acceptance tests use so the proof does not depend on the simulator loop.

## Surfaces

| Surface | LOCAL_JWT | Cognito OIDC |
| --- | --- | --- |
| Control Tower maintenance | http://localhost:3001/maintenance | http://127.0.0.1:5173/maintenance |
| Guest app | http://localhost:3000 | http://localhost:3000 |
| VenueOps API | http://localhost:8080 | http://localhost:8080 |

Park Flow Intelligence (`:8100`) is part of the same Compose graph and is not used on this path.

## Prerequisites

1. Stack healthy: `./scripts/dev-up.sh` then `./scripts/wait-for-ready.sh` (add `--oidc` for Cognito).
2. LOCAL_JWT tokens are the offline default (`local-development-token`, `local-operator-token`, `local-reliability-token`).
3. OIDC requires exported `SUPERVISOR_TOKEN`, `OPERATOR_LIMITED_TOKEN`, and `RELIABILITY_TOKEN`. Never paste tokens into chat.

```bash
# LOCAL_JWT
./scripts/maintenance-lifecycle-acceptance.sh

# OIDC
SUPERVISOR_TOKEN=... OPERATOR_LIMITED_TOKEN=... RELIABILITY_TOKEN=... \
  ./scripts/maintenance-lifecycle-acceptance.sh
```

## What the script proves

1. Unauthenticated operator maintenance is `401`. Reliability ingest without a machine token is `401`.
2. The reliability token cannot call operator routes (`403`). An operator token cannot ingest (`403`).
3. A unique Cypress Coil vibration observation creates a pending recommendation. A retried `observationId` is accepted as a duplicate and does not open a work order.
4. Operator `ACCEPT` (with `commandId` + `expectedVersion`) creates a work order. The same `commandId` replays. A new `commandId` against the spent recommendation is `409`.
5. `START_WORK` before `OPEN` is `409`. Stale `expectedVersion` is `409 STALE_VERSION`.
6. Open → assign → start work → checklist → request inspection.
7. Operator `APPROVE_INSPECTION` is `403`. Supervisor inspect moves the order to `READY_FOR_TESTING` and does **not** reopen Cypress Coil.
8. Guest REST and SSE never contain `internalDescription`, work-order numbers, asset codes, or the script's internal marker.
9. Operations `START_TESTING`. Supervisor `COMPLETE` while the attraction is testing is `422 MAINTENANCE_PREREQUISITE`.
10. Complete testing, approve return to service, then complete the work order.
11. Work-order activity records the JWT actor, not a forged `X-Actor`.

## Control Tower companion

After the script, or instead of it for a narrated pass:

1. Open **Maintenance**. Accept a pending Cypress Coil vibration recommendation (or use the HTTP ingest).
2. Work the order through inspection. Confirm **Ready for testing** recommends Start testing and does not change attraction status.
3. From the attraction workspace, start testing, complete testing, and approve return to service.
4. Complete the work order. Confirm the guest app still has no work-order numbers, asset codes, or internal notes.

## Pass criteria

- `./scripts/maintenance-lifecycle-acceptance.sh` exits 0 (LOCAL_JWT, or OIDC with exported tokens).
- Guest REST and SSE never carry maintenance internals.
- Audit actors come from the verified token.
- Completing a work order never skips attraction testing.
