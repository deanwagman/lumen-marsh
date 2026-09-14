# Container inventory

Verified Phase 1 independent builds on 2026-09-02 (Docker Desktop, arm64).

| Image | Size | Exposed port | Runtime user | Health check | Configuration notes |
| --- | --- | --- | --- | --- | --- |
| `venueops-api:local` | 512MB | `8080` | `appuser` (uid 1000) | `GET /actuator/health` → `{"status":"UP"}` | In-memory persistence by default; graceful shutdown enabled. Postgres via `SPRING_PROFILES_ACTIVE=postgres` + JDBC env. |
| `environmental-monitor:local` | 370MB | `8080` | `appuser` (uid 1000) | `GET /health/live`, `GET /health/ready` | Image sets `APP_ENV=production` and `SIMULATION_ENABLED=false`. Requires `NWS_USER_AGENT`, `PARK_LATITUDE`, `PARK_LONGITUDE`. Map host `8000:8080` for the planned public port. |
| `venueops-console:local` | 77.2MB | `8080` | `nginx` (uid 101) | `GET /health` → `ok` | Build-arg `VITE_API_BASE_URL` is compiled into the Vite bundle. SPA fallback via nginx. Map host `3001:8080`. |
| `lumen-marsh-app:local` | 194MB | `8080` | `nginx` (uid 101) | `GET /health` → `ok` | Build pins Flutter **3.47.1** (Dart 3.13.1). Build-arg `VENUEOPS_API_BASE_URL` via `--dart-define`. Map host `3000:8080`. |

## Verification performed

- Each image built with `docker build` from its application directory (no root Compose).
- Each container started alone, answered its health endpoint, and ran as non-root.
- VenueOps accepted `docker stop` (SIGTERM) within the graceful shutdown window.
- Environmental Monitor loaded with simulation disabled by image defaults.
- Frontends returned HTTP 200 for `/` and `/health`.

## Rebuild commands

```bash
docker build -t venueops-api:local ../venueops-api
docker build -t environmental-monitor:local ../environmental-monitor
docker build --build-arg VITE_API_BASE_URL=http://localhost:8080 \
  -t venueops-console:local ../venueops-console
docker build --build-arg VENUEOPS_API_BASE_URL=http://localhost:8080 \
  -t lumen-marsh-app:local ../lumen-marsh-app
```

Tag with version and commit SHA when publishing:

```bash
SHA=$(git -C ../venueops-api rev-parse --short HEAD)
docker tag venueops-api:local venueops-api:0.0.1-$SHA
```

## Next

Phase 2–3: local databases and root Compose. Do not start Compose until these images remain independently healthy.
