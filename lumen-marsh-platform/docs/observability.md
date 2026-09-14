# Correlation and observability contract (Phase 7)
#
# Platform scripts and Compose do not invent business workflows. Applications
# own domain identifiers. This document standardizes the fields that should
# appear in structured logs so a recommendation can be followed across services.

## Correlation fields

| Field | Owner | Notes |
| --- | --- | --- |
| `correlation_id` | originating request / demo script | Optional until HTTP propagation is wired |
| `recommendation_id` | Environmental Monitor → VenueOps | Primary cross-service join key |
| `incident_id` | VenueOps | Created by Control Tower |
| `attraction_id` | VenueOps | Explicit hold/recovery commands |
| `event_id` | VenueOps SSE | Guest and operator streams |
| `actor_id` | VenueOps (`X-Actor`) | Example: `control-tower` |
| `source_service` | each backend | `environmental-monitor` or `venueops-api` |

## Expected flow

```text
weather observation
  → recommendation (recommendation_id, source version)
  → VenueOps ingestion (same recommendation_id)
  → incident (incident_id) + LINK_INCIDENT
  → attraction command (attraction_id)
  → SSE event (event_id)
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
```

## Initial metrics (application-owned)

VenueOps: command counts/failures, stale-version conflicts, active SSE
subscribers, SSE send failures, active incidents, pending weather recommendations.

Environmental Monitor: provider latency/failures, observation age, poll
success/failure, active recommendations, VenueOps delivery failures.
