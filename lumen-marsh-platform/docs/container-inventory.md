# Container inventory

Compose (`compose.yaml`) runs six application images, two PostgreSQL instances, and one one-shot Environmental Monitor migrate job. Independent image sizes below for the original four apps were recorded on 2026-09-02 (Docker Desktop, arm64). Park Flow Intelligence and Reliability Intelligence were added later; sizes are omitted rather than guessed.

| Image | Size | Host port | Container port | Runtime user | Health check | Configuration notes |
| --- | --- | --- | --- | --- | --- | --- |
| `venueops-api:local` | 512MB | `8080` | `8080` | `appuser` (uid 1000) | `GET /actuator/health` → `{"status":"UP"}` | Compose sets `SPRING_PROFILES_ACTIVE=postgres`. Native `bootRun` defaults to in-memory persistence. |
| `environmental-monitor:local` | 370MB | `8000` | `8080` | `appuser` (uid 1000) | `GET /health/live`, `GET /health/ready` | Image sets `APP_ENV=production` and `SIMULATION_ENABLED=false`. `compose.override.yaml` turns simulation on for local demos. |
| `park-flow-intelligence:local` | — | `8100` | `8080` | `appuser` (uid 1000) | `GET /health/live`, `GET /health/ready` | Python 3.13 slim. Stateless. `compose.override.yaml` enables simulation and `local-flow-token`. |
| `reliability-intelligence:local` | — | `8200` | `8080` | `appuser` (uid 1000) | `GET /health/live`, `GET /health/ready` | Python 3.13 slim. Posts Cypress Coil vibration ingest. Never creates a work order. |
| `venueops-console:local` | 77.2MB | `3001` | `8080` | `nginx` (uid 101) | `GET /health` → `ok` | Build-arg `VITE_API_BASE_URL` is compiled into the Vite bundle. SPA fallback via nginx. |
| `lumen-marsh-app:local` | 194MB | `3000` | `8080` | `nginx` (uid 101) | `GET /health` → `ok` | Build pins Flutter **3.47.1** (Dart 3.13.1). Build-arg `VENUEOPS_API_BASE_URL` via `--dart-define`. |

Compose also starts `venueops-db` and `environmental-db` on private data networks (not published in `compose.yaml`) and `environmental-migrate` (Alembic, `restart: "no"`).

## Verification performed

- Each application image builds from its sibling directory (see `compose.yaml` `build.context`).
- Containers run as non-root.
- VenueOps accepted `docker stop` (SIGTERM) within the graceful shutdown window.
- Environmental Monitor, Park Flow Intelligence, and Reliability Intelligence load with simulation disabled by image / `compose.yaml` defaults.
- Frontends returned HTTP 200 for `/` and `/health`.

## Rebuild commands

```bash
docker build -t venueops-api:local ../venueops-api
docker build -t environmental-monitor:local ../environmental-monitor
docker build -t park-flow-intelligence:local ../park-flow-intelligence
docker build -t reliability-intelligence:local ../reliability-intelligence
docker build --build-arg VITE_API_BASE_URL=http://localhost:8080 \
  -t venueops-console:local ../venueops-console
docker build --build-arg VENUEOPS_API_BASE_URL=http://localhost:8080 \
  -t lumen-marsh-app:local ../lumen-marsh-app
```

Prefer `./scripts/dev-up.sh`, which builds and starts the full Compose graph.

Tag with version and commit SHA when publishing:

```bash
SHA=$(git -C ../venueops-api rev-parse --short HEAD)
docker tag venueops-api:local venueops-api:0.0.1-$SHA
```
