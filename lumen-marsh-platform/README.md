# Lumen Marsh Platform

Integration, orchestration, and deployment home for the Lumen Marsh portfolio system.

Application source lives in sibling directories in the Lumen Marsh monorepo. This directory owns Compose orchestration, demonstration scripts, infrastructure as code, and cross-service validation. It does not duplicate application business logic.

## Architecture

Seven pieces, one Compose graph, two databases:

```text
Guest browser                 Control Tower browser
   │                                    │
   ▼                                    ▼
lumen-marsh-app :3000          venueops-console :3001
   │                                    │
   └──────────────┬─────────────────────┘
                  ▼
           venueops-api :8080
          ▲               ▲               ▲
 weather  │               │  observations │  vibration
          │               │  / forecasts  │
environmental-monitor   park-flow-intelligence   reliability-intelligence
         :8000                 :8100                    :8200
          │
venueops-db / environmental-db (private)
```

Reliability Intelligence posts Cypress Coil vibration to VenueOps (`POST /api/v1/integrations/reliability/recommendations`) with `local-reliability-token` in `LOCAL_JWT` mode. It never creates a work order.

## Repository map

```text
lumen-marsh/
├── lumen-marsh-app/          Flutter guest companion
├── venueops-console/         React operator console (Control Tower)
├── venueops-api/             Spring Boot operational API
├── environmental-monitor/    Python weather monitor and recommender
├── park-flow-intelligence/   Python queue simulator and flow recommender
├── reliability-intelligence/ Python vibration simulator and reliability recommender
└── lumen-marsh-platform/     This repository
```

| Application | Responsibility |
| --- | --- |
| **lumen-marsh-app** | Guest catalog, advisories, Best Next Experience, live park updates |
| **venueops-console** | Control Tower: attractions, incidents, weather inbox, maintenance, park flow |
| **venueops-api** | Attractions, incidents, advisories, weather inbox, maintenance, flow, SSE |
| **environmental-monitor** | Weather → recommendations to VenueOps. Never commands a ride. |
| **park-flow-intelligence** | Simulated queues and forecasts → VenueOps. Never changes attraction state. |
| **reliability-intelligence** | Simulated Cypress Coil vibration → VenueOps. Never creates a work order. |
| **lumen-marsh-platform** | Local integration, demos, OpenTofu, platform CI |

## Prerequisites

- Docker Desktop (or compatible engine + Compose v2)
- A complete Lumen Marsh monorepo checkout
- `curl` and `python3`

## Quick start (integrated stack)

```bash
cp .env.example .env
# Edit NWS_USER_AGENT to include your contact information

./scripts/dev-up.sh
./scripts/smoke-test.sh
./scripts/storm-lifecycle-acceptance.sh
./scripts/maintenance-lifecycle-acceptance.sh
./scripts/flow-lifecycle-acceptance.sh
```

To run the browser console against the applied development Cognito user pool:

```bash
./scripts/dev-up.sh --oidc
```

OIDC mode starts the full stack. The console is available at `http://127.0.0.1:5173`; human users authenticate with Authorization Code + PKCE. Environmental Monitor, Park Flow Intelligence, and Reliability Intelligence use separate client-credentials grants. The launcher reads machine secrets from AWS Secrets Manager with `AWS_PROFILE` (default: `lumen-marsh`) and never writes them to `.env`. The default command remains the complete offline `LOCAL_JWT` stack.

Service URLs:

| Surface | URL |
| --- | --- |
| Guest app | http://localhost:3000 |
| Operator console | http://localhost:3001 |
| VenueOps API | http://localhost:8080 |
| Environmental Monitor | http://localhost:8000 |
| Park Flow Intelligence | http://localhost:8100 |
| Reliability Intelligence | http://localhost:8200 |

Stop without deleting data:

```bash
./scripts/dev-down.sh
```

Reset demo databases (destructive):

```bash
./scripts/reset-demo.sh --confirm
./scripts/reset-demo.sh --confirm --oidc
```

Debug overlays (DB host ports + verbose logs):

```bash
./scripts/dev-up.sh --debug
```

Production-like Compose (no local override, simulation off):

```bash
docker compose -f compose.yaml --env-file .env up --build
```

## Native development

Compose does not replace fast local loops:

```bash
cd ../venueops-api && ./gradlew bootRun
cd ../environmental-monitor && uv run fastapi dev src/environmental_monitor/main.py
cd ../park-flow-intelligence && uv run fastapi dev src/park_flow_intelligence/main.py --port 8100
cd ../reliability-intelligence && uv run fastapi dev src/reliability_intelligence/main.py --port 8200
cd ../venueops-console && npm run dev
cd ../lumen-marsh-app && flutter run -d chrome \
  --dart-define=VENUEOPS_API_BASE_URL=http://localhost:8080
```

## Scripts

| Script | Purpose |
| --- | --- |
| `scripts/dev-up.sh [--oidc]` | Validate `.env`, start the offline stack or Cognito-backed full stack, wait for readiness, print URLs |
| `scripts/dev-down.sh` | Compose down (keeps volumes) |
| `scripts/wait-for-ready.sh` | Poll health endpoints |
| `scripts/reset-demo.sh --confirm [--oidc]` | Delete demo volumes and recreate |
| `scripts/storm-lifecycle-acceptance.sh` | Repeatable storm lifecycle + leak/failure checks |
| `scripts/maintenance-lifecycle-acceptance.sh` | Repeatable reliability ingest → accept → inspect → testing + leak/failure checks |
| `scripts/flow-lifecycle-acceptance.sh` | Repeatable mangrove disruption → publish → guest Best Next + leak/failure checks |
| `scripts/storm-demo.sh` | Shorter HTTP storm → incident → hold → clear → recover |
| `scripts/smoke-test.sh` | Health, ingest idempotency, incident, advisory, SSE |
| `scripts/security-scan.sh` | Trivy filesystem + local image scan (requires `trivy`) |
| `scripts/provision-dev-cognito.sh` | Apply `dev-identity` Cognito, then create the development operator |
| `scripts/create-dev-operator.sh` | Create the operator (password in gitignored `.env.cognito.local`, not OpenTofu state) |
| `scripts/create-dev-supervisor.sh` | Create the supervisor (password merged into gitignored `.env.cognito.local`) |
| `scripts/print-cognito-local-env.sh` | Print native console + API env from the applied `dev-identity` stack |
| `scripts/bootstrap-cognito-dev-users.sh` | Create one operator and one supervisor (passwords from env) |
| `scripts/logs.sh [service]` | Tail Compose logs |

## Health endpoints

- VenueOps: `GET /actuator/health`
- Environmental Monitor: `GET /health/live`, `GET /health/ready`
- Park Flow Intelligence: `GET /health/live`, `GET /health/ready`
- Reliability Intelligence: `GET /health/live`, `GET /health/ready`
- Console / guest: `GET /health`

## Container inventory

See [docs/container-inventory.md](docs/container-inventory.md).

## Observability

See [docs/observability.md](docs/observability.md). Follow `recommendation_id` across Monitor → VenueOps → incident → attraction command → SSE, and `observation_id` across reliability ingest or park-flow → VenueOps.

## Security (identity milestone)

Identity and access control docs:

- [docs/security-model.md](docs/security-model.md)
- [docs/security/rollout.md](docs/security/rollout.md)
- [docs/security/hardening.md](docs/security/hardening.md)
- [docs/security/secret-rotation.md](docs/security/secret-rotation.md)
- [docs/security/security-contract.md](docs/security/security-contract.md)
- [docs/security/local-development.md](docs/security/local-development.md)
- [docs/storm-lifecycle-demo.md](docs/storm-lifecycle-demo.md)
- [docs/maintenance-lifecycle-demo.md](docs/maintenance-lifecycle-demo.md)
- [docs/flow-lifecycle-demo.md](docs/flow-lifecycle-demo.md)
- [docs/security/public-deployment-gate.md](docs/security/public-deployment-gate.md)

Public AWS demo remains blocked until the [public deployment gate](docs/security/public-deployment-gate.md) is signed off.

## Version manifest

[versions.yaml](versions.yaml) records which application image tags/digests the platform expects. Shared environments should pin digests, not `latest`.

## OpenTofu (Phase 9)

Bootstrap validates with **no AWS resources** created:

```bash
cd infrastructure/environments/demo
tofu init -backend=false
tofu fmt -check -recursive ../..
tofu validate
```

Armed apply (Phase 10+) requires an explicit reviewed plan:

```bash
tofu plan -var='enable_demo_resources=true'
```

Design notes: [docs/aws-demo.md](docs/aws-demo.md).

## CI

The root `.github/workflows/ci.yml` validates every application plus Compose config, OpenTofu formatting/validation, shell syntax, and the repository security scan.

## Portfolio demo (reviewer path)

Local:

1. `cp .env.example .env` and set `NWS_USER_AGENT`
2. `./scripts/dev-up.sh`
3. Open Control Tower and guest app side by side
4. `./scripts/storm-lifecycle-acceptance.sh` or follow [docs/storm-lifecycle-demo.md](docs/storm-lifecycle-demo.md)
5. Watch recommendation → incident → hold → advisory → clearance → recovery
6. `./scripts/maintenance-lifecycle-acceptance.sh` or follow [docs/maintenance-lifecycle-demo.md](docs/maintenance-lifecycle-demo.md)
7. Watch reliability ingest → accept → inspect → operations testing
8. `./scripts/flow-lifecycle-acceptance.sh` or follow [docs/flow-lifecycle-demo.md](docs/flow-lifecycle-demo.md) for disruption → publish → Best Next

Hosted AWS demo is Phase 10–12 and is not provisioned by default.

## Known limitations

- VenueOps SSE is in-memory → one API instance only
- Environmental Monitor polling is not multi-replica safe
- Park Flow Intelligence is stateless; VenueOps stores observations, projections, forecasts, and recommendations
- Reliability Intelligence is a local simulator; it never creates a work order
- Frontend API origins are build-time (`VITE_*` / `--dart-define`) until runtime config lands
- Self-managed Postgres on the future EC2 demo lacks managed HA/PITR
- Phase 13 managed AWS (Fargate/ALB/RDS) is optional and cost-sensitive
- Stop native `bootRun` / `fastapi` / `npm run dev` before Compose if ports 8080/8000/8100/8200/3000/3001 are already taken
- Public AWS demo stays blocked until [docs/security/public-deployment-gate.md](docs/security/public-deployment-gate.md) is signed off

## Delivery status

| Milestone | Phases | Status |
| --- | --- | --- |
| Local integrated environment | 0–6 | Done — `./scripts/dev-up.sh` |
| Observable / testable platform | 7–8 | Done — observability contract + CI/validation |
| Infrastructure foundation | 9 | Done — OpenTofu validates; no AWS resources |
| Public demo deployment | 10–12 | Designed only (`docs/aws-demo.md`); not applied |
| Managed-cloud evolution | 13 | Documented only |
