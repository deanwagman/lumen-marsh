# Lumen Marsh Park Flow Intelligence

A local Python service that generates **simulated** attraction queue observations, deterministic wait forecasts, and operator-reviewed flow recommendations.

It submits evidence to VenueOps. **It never changes attraction state, capacity, posted waits, or guest guidance.**

```text
Simulator
      ↓
Park Flow Intelligence
          ↓
Observations, forecasts, recommendation proposals
          ↓
VenueOps API (authoritative)
          ↓
Operator review / supervisor publication
          ↓
Guest Best Next Experience
```

Lumen Marsh is a fictional demonstration environment. Simulated data is marked `simulated: true` in every payload.

## Requirements

- Python 3.13
- [uv](https://docs.astral.sh/uv/)

## Local startup

```bash
cp .env.example .env
uv sync
uv run fastapi dev src/park_flow_intelligence/main.py --port 8100
```

The development server listens on http://localhost:8100.

Production-style container (listens on **8080** inside the image; map to host 8100):

```bash
docker build -t park-flow-intelligence:local .
docker run --rm -p 8100:8080 \
  -e APP_ENV=development \
  -e SIMULATION_ENABLED=true \
  -e OIDC_DISABLED=true \
  -e VENUEOPS_BEARER_TOKEN=local-flow-token \
  -e VENUEOPS_BASE_URL=http://host.docker.internal:8080 \
  park-flow-intelligence:local
```

```bash
curl http://localhost:8100/health/live
curl http://localhost:8100/health/ready
```

Simulation controls are local-development only (`SIMULATION_ENABLED=true`):

```bash
curl -X POST http://localhost:8100/simulation/start
curl -X POST http://localhost:8100/simulation/scenarios/normal
curl -X POST http://localhost:8100/simulation/scenarios/mangrove-disruption
curl -X POST http://localhost:8100/simulation/scenarios/clear
curl -X POST http://localhost:8100/simulation/stop
```

## Quality commands

```bash
uv run ruff format --check .
uv run ruff check .
uv run mypy src
uv run pytest
```

## Forecasting

Forecasts are deterministic. For horizon `h`:

```text
predictedQueue = max(0, round(currentQueue + arrivalRate * h - throughputRate * h + disruptionTransfer))
predictedWait  = ceil(predictedQueue / max(throughputRate, 0.5))
```

Arrival and throughput rates use an exponentially weighted moving average of the three newest samples (`0.50 / 0.30 / 0.20`).

When Mangrove Run is unavailable, a configurable share of expected arrivals is redistributed to Cypress Coil (50%) and Stormglass Station (30%). Remaining demand is treated as other experiences and is not applied to those attractions.

## Authorization

VenueOps ingest requires `venueops/flow-ingest.write`. Local Compose uses `local-flow-token`. Cognito client-credentials use the `park-flow-intelligence` machine client.
