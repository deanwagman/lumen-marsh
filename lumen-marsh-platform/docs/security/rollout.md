# Cognito rollout status

This is the working copy of the 12-phase Cognito plan. Application code already covers Phases 4–11. The remaining work is applying the user pool and proving the local login loop.

| Phase | Status |
|---|---|
| 1 Security model | Done — [security-model.md](../security-model.md), [endpoint-inventory.md](./endpoint-inventory.md) |
| 2 Cognito foundation | Ready to apply in `infrastructure/environments/dev-identity` |
| 3 Local authentication loop | Console on `http://localhost:5173`; after apply, `./scripts/print-cognito-local-env.sh` |
| 4 VenueOps JWT resource server | Done |
| 5 Verified `ActorIdentity` | Done |
| 6 Console OIDC + attraction command | Done; first command still `POST .../attractions/{id}/commands` |
| 7 Remaining control-plane authorization | Done |
| 8 Operator SSE | Done (fetch + Bearer) |
| 9 Environmental Monitor client credentials | Application done; development Cognito client ready to apply |
| 10 Local + offline tests | Done (`LOCAL_JWT` / test JWTs) |
| 11 Hardening | Application controls done; edge WAF remains |
| 12 Hosted acceptance | Blocked by [public-deployment-gate.md](./public-deployment-gate.md) |

## Immediate milestone (Phases 1–6)

The first meaningful checkpoint is:

```text
Guest can read Mangrove Run
Operator signs in
Operator changes its state
Anonymous command is rejected
Activity history records the authenticated operator
```

Keep Flutter, Environmental Monitor, and operator SSE out of that checkpoint.

Native console loop (not Compose on :3001):

1. `./scripts/provision-dev-cognito.sh` (applies `infrastructure/environments/dev-identity` only)
2. `python3 ./scripts/verify-cognito-console-login.py` and sign in as the operator from `.env.cognito.local`

Compose defaults to the offline `LOCAL_JWT` stack. Use `./scripts/dev-up.sh --oidc` for the Cognito-backed full stack; the launcher keeps local JWTs out of OIDC mode.
