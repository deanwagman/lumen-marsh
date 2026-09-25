# Security documentation

Identity and access control for the Lumen Marsh control plane.

| Document | Purpose |
|---|---|
| [../security-model.md](../security-model.md) | Phase 1 access model and endpoint permission matrix |
| [../guest-advisory-contract.md](../guest-advisory-contract.md) | Guest advisory REST/SSE allowlist (not `/api/v1/guest/**`) |
| [rollout.md](./rollout.md) | 12-phase Cognito rollout status |
| [security-contract.md](./security-contract.md) | Roles, scopes, claims, audit identity, deny-by-default policy |
| [endpoint-inventory.md](./endpoint-inventory.md) | Every route and command classified |
| [adr/0001-cognito-jwt.md](./adr/0001-cognito-jwt.md) | Decision to use Cognito + JWT resource server |
| [local-development.md](./local-development.md) | Cognito vs LOCAL_JWT Compose bridge |
| [hardening.md](./hardening.md) | Phase 9 hardening checklist |
| [secret-rotation.md](./secret-rotation.md) | Credential rotation |
| [acceptance-matrix.md](./acceptance-matrix.md) | Acceptance scenarios |
| [../storm-lifecycle-demo.md](../storm-lifecycle-demo.md) | Integrated weather UI demo + failure checks |
| [../maintenance-lifecycle-demo.md](../maintenance-lifecycle-demo.md) | Reliability ingest → inspect → testing HTTP proof |
| [public-deployment-gate.md](./public-deployment-gate.md) | Phase 10 public demo gate |

See [rollout.md](./rollout.md) for the 12-phase Cognito plan. Snapshot:

| Phase | Status |
|---|---|
| 1 Security model | Done |
| 2 Cognito OpenTofu module | Ready to apply (`infrastructure/environments/dev-identity`) |
| 3 Local login on :5173 | Ready after apply + `create-dev-operator.sh` |
| 4–9 API, console, monitor authz | Done |
| 10 Local/test modes | Done |
| 11 Hardening | Application controls done |
| 12 Public deploy gate | **Blocked until signed off** |
