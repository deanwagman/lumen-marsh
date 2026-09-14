# Lumen Marsh Environmental Monitor

A Python service that retrieves public Orlando-area weather data, normalizes observations, evaluates **fictional** operational thresholds, and sends recommendations to VenueOps.

The service provides evidence and recommendations. **It never changes attraction state directly.**

```text
National Weather Service
Simulated lightning sensor
          ↓
Environmental Monitor
          ↓
Weather observations and recommendations
          ↓
VenueOps API
          ↓
Operator approval
          ↓
Incidents and attraction commands
```

## Architecture

- **Domain** models are provider-neutral. They do not import FastAPI, HTTPX, or SQLAlchemy.
- **Application** services depend on a `WeatherProvider` protocol and a `RecommendationSink` protocol.
- **Infrastructure** contains the National Weather Service adapter, a fake provider for tests, optional PostgreSQL persistence, and the VenueOps publisher.
- Internal timestamps are timezone-aware UTC. Park local time is converted only for display.

Initial affected attractions: **Mangrove Run** (`mangrove-run`) and **Cypress Coil** (`cypress-coil`). Stormglass Station remains the fictional monitoring location and is not placed on weather hold by these rules.

## Requirements

- Python 3.13
- [uv](https://docs.astral.sh/uv/)

## Local startup

```bash
cp .env.example .env
# Replace NWS_USER_AGENT with an identifier that includes your contact information.
uv sync
uv run fastapi dev src/environmental_monitor/main.py
```

The development server listens on http://localhost:8000.

Production-style container (listens on **8080** inside the image; map to host 8000):

```bash
docker build -t environmental-monitor:local .
docker run --rm -p 8000:8080 \
  -e NWS_USER_AGENT='LumenMarshEnvironmentalMonitor/0.1 (you@example.com)' \
  -e PARK_LATITUDE=28.474 \
  -e PARK_LONGITUDE=-81.466 \
  -e VENUEOPS_BASE_URL=http://host.docker.internal:8080 \
  environmental-monitor:local
```

The image defaults to `APP_ENV=production` and `SIMULATION_ENABLED=false`. Override only for demo/dev.

```bash
curl http://localhost:8000/health/live
curl http://localhost:8000/health/ready
curl http://localhost:8000/api/v1/weather/status
```

`live` confirms the process is running. `ready` confirms configuration loaded and initialization finished. Readiness does not require the National Weather Service to be available.

## Quality commands

```bash
uv run ruff format --check .
uv run ruff check .
uv run mypy src
uv run pytest
```

## Configuration

All environment access lives in `src/environmental_monitor/settings.py`. The process fails at startup when required settings are absent or invalid. Coordinates, polling intervals, and fictional rule thresholds are configuration, not code constants.

See `.env.example` for the full set. Important values:

| Variable | Purpose |
| --- | --- |
| `PARK_LATITUDE` / `PARK_LONGITUDE` | Stormglass Station / Orlando-area point used for NWS lookups |
| `NWS_USER_AGENT` | Required identification with contact information |
| `SIMULATION_ENABLED` | Development lightning simulator endpoints |
| `VENUEOPS_BASE_URL` | Recommendation delivery target |
| `DATABASE_URL` | Optional PostgreSQL URL. Empty keeps data in memory |
| `OIDC_TOKEN_URL` | Cognito token endpoint used for client-credentials authentication |
| `OIDC_CLIENT_ID` / `OIDC_CLIENT_SECRET` | Weather-service machine client credentials |
| `OIDC_SCOPE` | VenueOps recommendation-write scope requested by the service |
| `OIDC_DISABLED` | Local/test escape hatch only; production rejects this setting |

Production should set `APP_ENV=production` and `SIMULATION_ENABLED=false`. Never commit `.env` or credentials.

## HTTP API

| Method | Path | Notes |
| --- | --- | --- |
| GET | `/health/live` | Process liveness |
| GET | `/health/ready` | Initialization complete |
| GET | `/api/v1/weather/current` | Latest observation and simulated lightning |
| GET | `/api/v1/weather/forecast` | Hourly forecast |
| GET | `/api/v1/weather/alerts` | Active official alerts |
| GET | `/api/v1/weather/recommendations` | Active and cleared recommendations |
| GET | `/api/v1/weather/status` | Poll freshness, station, provider state, metrics |
| POST | `/api/v1/simulation/scenarios/clear` | Development only |
| POST | `/api/v1/simulation/scenarios/storm-approaching` | Development only |
| POST | `/api/v1/simulation/scenarios/hold-conditions` | Development only |
| POST | `/api/v1/simulation/scenarios/clearance-period` | Development only |
| POST | `/api/v1/simulation/lightning` | Controlled simulated strike |

Dataset freshness is explicit: `CURRENT`, `STALE`, `UNAVAILABLE`, or `NOT_YET_LOADED`. Missing measurements stay `null` and are never treated as safe zeros.

## Fictional demonstration rules

These thresholds are labeled in recommendation evidence and are **not** real park operating criteria.

- Official NWS warnings → review outdoor attractions for weather hold
- Gusts above the configured threshold → review Cypress Coil
- Rainfall above the configured threshold → inspect Mangrove Run water-level conditions
- Simulated lightning inside the configured radius → recommend weather hold for Mangrove Run and Cypress Coil
- Simulated area clear for the configured duration → recommend return-to-service review

Repeated evaluations of the same active condition update the existing recommendation. Clearing a condition sets status to `CLEARED`. A later reactivation opens a new recommendation.

## Persistence

When `DATABASE_URL` is set, the service uses its own PostgreSQL database or schema. It never writes VenueOps tables.

```bash
docker compose up -d postgres
export DATABASE_URL=postgresql+asyncpg://environmental:environmental@localhost:5433/environmental_monitor
uv run alembic upgrade head
```

Recommended demo retention: observations 30 days, forecast periods 7 days after expiration, alerts and recommendations indefinitely, poll attempts 14 days.

## VenueOps integration

Recommendations are published to:

```http
POST /api/v1/integrations/weather/recommendations
```

In Cognito OIDC mode, the monitor obtains a cached client-credentials access token and requests only `venueops/weather-recommendations.write`. VenueOps rejects that service identity from operator routes. `OIDC_DISABLED=true` and `VENUEOPS_BEARER_TOKEN` exist only for the isolated local/test workflow.

Delivery uses recommendation ID and version for idempotency, retries temporary failures, and keeps undelivered recommendations until VenueOps recovers. This service does not call attraction-command endpoints or create guest advisories.

VenueOps remains responsible for operator presentation, incidents, attraction commands, guest advisories, and the operational audit trail.

## Demo flow

With `SIMULATION_ENABLED=true`:

```bash
./scripts/demo.sh http://localhost:8000
```

1. Clear conditions
2. Storm approaching (outside the hold radius)
3. Nearby simulated lightning → active weather-hold recommendation
4. Clearance period → return-to-service recommendation
5. Restore clear conditions with a single command

Simulated lightning is always labeled `provider=simulated` and cannot be stored as an NWS observation.

## Docker

```bash
docker compose up --build
```

The image installs from `uv.lock`, runs as a non-root user, exposes port 8080, and uses a health check. Configuration comes from the environment. Do not copy `.env` into the image.

Run exactly one polling instance. Multiple web workers or replicas would duplicate scheduled polling.

Production-style command:

```bash
uv run uvicorn environmental_monitor.main:app --host 0.0.0.0 --port 8080
```

## Limitations

- No direct automatic attraction commands
- Lightning is simulated; there is no commercial lightning-provider integration
- No machine-learning forecasting
- Single park
- Cognito client credentials are used in OIDC mode; there is no mTLS or service mesh
- No Kafka or other brokers
- No multi-instance polling coordination
- No long-term analytics
- No guest-facing forecasts

## References

- [uv project documentation](https://docs.astral.sh/uv/concepts/projects/init/)
- [FastAPI documentation](https://fastapi.tiangolo.com/)
- [National Weather Service API](https://www.weather.gov/documentation/services-web-api)
- [National Weather Service alerts](https://www.weather.gov/documentation/services-web-alerts)
- [HTTPX documentation](https://www.python-httpx.org/)
- [Pydantic Settings documentation](https://www.pydantic.dev/latest/concepts/pydantic-settings/)
