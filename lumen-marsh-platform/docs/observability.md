# Correlation and observability contract (Phase 7)
#
# Platform scripts and Compose do not invent business workflows. Applications
# own domain identifiers. This document standardizes the fields that should
# appear in structured logs so a recommendation can be followed across services.

## Correlation fields

| Field | Owner | Notes |
| --- | --- | --- |
| `correlation_id` | originating request / demo script | Optional until HTTP propagation is wired |
| `recommendation_id` | Environmental Monitor or reliability ingest → VenueOps | Weather inbox or maintenance recommendation join key |
| `observation_id` | Reliability ingest or Park Flow Intelligence → VenueOps | Idempotency key for vibration samples and queue observations |
| `incident_id` | VenueOps | Created by Control Tower |
| `work_order_id` | VenueOps | Created when an operator accepts a reliability recommendation |
| `attraction_id` | VenueOps | Explicit hold/recovery/testing commands |
| `event_id` | VenueOps SSE | Guest and operator streams |
| `actor_id` | VenueOps (JWT `sub` via `ActorIdentity`) | Human subject, machine client subject, or `venueops-system`. Client `X-Actor` is ignored. |
| `source_service` | each backend | `environmental-monitor`, `park-flow-intelligence`, or `venueops-api` |

## Expected flows

```text
weather observation
  → recommendation (recommendation_id, source version)
  → VenueOps ingestion (same recommendation_id)
  → incident (incident_id) + LINK_INCIDENT
  → attraction command (attraction_id)
  → SSE event (event_id)

reliability ingest (observation_id)
  → pending maintenance recommendation (recommendation_id)
  → operator ACCEPT → work order (work_order_id)
  → inspect → attraction START_TESTING
  → operator SSE (maintenance.work-order.updated); guests never receive these events

queue observation / forecast (observation_id)
  → VenueOps projection + optional flow recommendation
  → supervisor PUBLISH
  → guest.flow.updated / guest.flow.recommendation.published
```

## Logging rules

Emit structured logs with timestamp, severity, service name, environment,
operation, duration, success/failure, and the domain IDs above.

Never log database passwords, auth tokens, full environment dumps, personal
contact details beyond the configured NWS user-agent identity, or internal
exception bodies in guest responses.

## Local log access

```bash
./scripts/logs.sh
./scripts/logs.sh venueops-api
./scripts/logs.sh environmental-monitor
./scripts/logs.sh park-flow-intelligence
```

## Initial metrics (application-owned)

VenueOps: command counts/failures, stale-version conflicts, active SSE
subscribers, SSE send failures, active incidents, pending weather
recommendations, open work orders, unpublished flow recommendations.

Environmental Monitor: provider latency/failures, observation age, poll
success/failure, active recommendations, VenueOps delivery failures.

Park Flow Intelligence: simulation tick age, VenueOps observation/forecast
delivery failures. The service is stateless; durable counts live in VenueOps.
