# Lumen Marsh Reliability Intelligence

A local Python service that generates **simulated** Cypress Coil vibration telemetry and posts reliability recommendations to VenueOps.

It submits evidence. **It never creates a work order, inspects an asset, or changes attraction state.** Operators accept recommendations in Control Tower.

```text
Simulator
      ↓
Reliability Intelligence
          ↓
Pending reliability recommendation
          ↓
VenueOps API (authoritative)
          ↓
Operator accept → work order
```

Lumen Marsh is a fictional demonstration environment. Simulated data is marked in evidence copy. VenueOps ingest is idempotent on `observationId`.

## Requirements

- Python 3.13
- [uv](https://docs.astral.sh/uv/)

## Local startup

```bash
cp .env.example .env
uv sync
uv run fastapi dev src/reliability_intelligence/main.py --port 8200
```

The development server listens on http://localhost:8200.

Production-style container (listens on **8080** inside the image; map to host 8200):

```bash
docker build -t reliability-intelligence:local .
docker run --rm -p 8200:8080 \
  -e APP_ENV=development \
  -e SIMULATION_ENABLED=true \
  -e OIDC_DISABLED=true \
  -e VENUEOPS_BEARER_TOKEN=local-reliability-token \
  -e VENUEOPS_BASE_URL=http://host.docker.internal:8080 \
  reliability-intelligence:local
```

```bash
curl http://localhost:8200/health/live
curl http://localhost:8200/health/ready
```

Simulation controls are local-development only (`SIMULATION_ENABLED=true`):

```bash
curl -X POST http://localhost:8200/simulation/start
curl -X POST http://localhost:8200/simulation/scenarios/normal
curl -X POST http://localhost:8200/simulation/scenarios/cypress-coil-vibration
curl -X POST http://localhost:8200/simulation/scenarios/clear
curl -X POST http://localhost:8200/simulation/stop
```

`cypress-coil-vibration` posts a WARNING-level recommendation for `CC-TRAIN-01-WHEEL-A`. Duplicate observation IDs are accepted by VenueOps without creating a second recommendation.

## Quality commands

```bash
uv run ruff format --check .
uv run ruff check .
uv run mypy src
uv run pytest
```

## Authorization

VenueOps ingest requires `venueops/reliability.write`. Local Compose uses `local-reliability-token`. Cognito client-credentials use the existing `reliability-integration` machine client.
