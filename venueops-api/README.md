# VenueOps API

VenueOps API is the Java/Spring Boot operational core for **Lumen Marsh**, a fictional eco-futurist themed destination. It models attraction availability, wait times, capacity, operational transitions, and an auditable history of changes.

The Attractions MVP answers one question well:

> What is each attraction's current operational condition, and how did it get there?

Incidents answer a second:

> What park-wide situation is in progress, who is handling it, and what should guests be told?

Weather recommendations answer a third:

> What did Environmental Monitor just advise, and has Control Tower acknowledged, dismissed, or opened an incident?

Maintenance work orders answer a fourth:

> What repair or inspection work is in progress, and is the attraction ready for operational testing?

## Technology

- Java 21
- Spring Boot 4.1
- Gradle
- Spring Web MVC, Validation, Actuator
- Spring Data JPA, PostgreSQL, Flyway
- Testcontainers
- OpenAPI / Swagger UI
- JUnit 5

## Getting started

### Requirements

- Java 21
- Docker (optional; needed for the PostgreSQL profile and Testcontainers tests)

The Gradle wrapper is included.

```bash
./gradlew bootRun
```

Production-style container (Gradle builds in the image; JRE-only runtime):

```bash
docker build -t venueops-api:local .
docker run --rm -p 8080:8080 venueops-api:local
# health: GET /actuator/health
```

The service starts at `http://localhost:8080` with an in-memory repository. Demo attractions are loaded when `venueops.attractions.seed=true` (the default in `application.properties`). Disable seeding with `venueops.attractions.seed=false`.

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/v1/attractions
```

OpenAPI documentation:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

### PostgreSQL

```bash
docker compose up -d
./gradlew bootRun --args='--spring.profiles.active=postgres'
```

### Tests

```bash
./gradlew test
```

The PostgreSQL integration test uses Testcontainers and is skipped automatically when Docker is not available.

## Seeded attractions

| ID | Name | Area | Type | Initial state |
| --- | --- | --- | --- | --- |
| `mangrove-run` | Mangrove Run | Luminous Wetlands | Boat expedition | `OPERATING`, normal capacity, 25-minute wait |
| `stormglass-station` | Stormglass Station | Research Quarter | Indoor dark ride | `CLOSED` |
| `cypress-coil` | Cypress Coil | Cypress Basin | Launch coaster | `CLOSED` |

Seeded Cypress Coil maintenance assets (when `venueops.attractions.seed=true`):

| Asset code | Name | Type |
| --- | --- | --- |
| `CC-ATTRACTION` | Cypress Coil | Attraction |
| `CC-RIDE-SYSTEM` | Cypress Coil Ride System | System |
| `CC-TRAIN-01` | Cypress Coil Train 1 | Vehicle |
| `CC-TRAIN-01-WHEEL-A` | Cypress Coil Train 1 Wheel Assembly A | Component |
| `CC-TRAIN-01-VIB-01` | Cypress Coil Train 1 Vibration Sensor | Sensor |

## Operational states

```mermaid
stateDiagram-v2
  [*] --> CLOSED
  CLOSED --> TESTING: START_TESTING
  TESTING --> RETURNING_TO_SERVICE: COMPLETE_TESTING
  RETURNING_TO_SERVICE --> OPERATING: APPROVE_RETURN_TO_SERVICE
  OPERATING --> WEATHER_HOLD: PLACE_WEATHER_HOLD
  WEATHER_HOLD --> TESTING: CLEAR_WEATHER_HOLD
  OPERATING --> TECHNICAL_DELAY: REPORT_TECHNICAL_FAULT
  TECHNICAL_DELAY --> TESTING: COMPLETE_REPAIR
  TESTING --> CLOSED: CLOSE_FOR_DAY
  RETURNING_TO_SERVICE --> CLOSED: CLOSE_FOR_DAY
  OPERATING --> CLOSED: CLOSE_FOR_DAY
  WEATHER_HOLD --> CLOSED: CLOSE_FOR_DAY
  TECHNICAL_DELAY --> CLOSED: CLOSE_FOR_DAY
```

Capacity is modeled separately from status: `NOT_APPLICABLE`, `NORMAL`, `REDUCED`. Non-operating attractions always use `NOT_APPLICABLE` and a `null` wait time. Returning to operation always starts at `NORMAL` capacity with a zero wait.

Clients submit commands. They cannot select a destination status directly. Anything not in the transition table is rejected.

## HTTP API

### Guest reads

```http
GET /api/v1/attractions
GET /api/v1/attractions/{attractionId}
GET /api/v1/attractions/events
```

The list endpoint returns lightweight operational `AttractionResponse` objects for catalog views. The detail endpoint returns `AttractionDetailResponse`, which composes live operational state with a stable `experience` profile (description, media, duration, intensity, environment, accessibility, height requirement).

Guest responses include a derived `statusMessage` and never include internal reasons. Missing height requirements are represented as JSON `null`, not placeholder copy.

### Live guest stream

```http
GET /api/v1/attractions/events
GET /api/v1/events
Accept: text/event-stream
```

The stream is a live projection of committed changes. Internal activity records remain the source of truth; SSE does not replay missed events. `GET /api/v1/attractions/events` remains for attraction-only clients. `GET /api/v1/events` is the park-wide **guest** stream (attractions and guest advisories). `GET /api/v1/operator/events` is the Control Tower stream and is the only stream that carries weather recommendations.

| SSE event | Stream | Purpose |
| --- | --- | --- |
| `attractions.snapshot` | Guest and operator | Current catalog, sent immediately after connect |
| `attraction.updated` | Guest and operator | One attraction changed after a successful operator command |
| `advisories.snapshot` | Guest park stream | Current published guest advisories |
| `advisory.published` / `advisory.updated` / `advisory.withdrawn` | Guest park stream | Guest-safe advisory changes |
| `weather.recommendations.snapshot` | Operator | Current weather-recommendation inbox |
| `weather.recommendation.updated` | Operator | A recommendation was created, updated, or handled |
| `weather.recommendation.cleared` | Operator | Environmental Monitor sent a cleared source version |
| `maintenance.work-orders.snapshot` | Operator with `venueops/maintenance.read` | Current work-order summaries |
| `maintenance.work-order.updated` | Operator with `venueops/maintenance.read` | One work order changed after a successful maintenance command |
| heartbeat comment | Guest and operator | Keeps proxies from closing an idle connection |

Each `attraction.updated` payload includes the complete operational state so clients can apply it independently:

```json
{
  "eventId": "activity-uuid",
  "eventType": "WAIT_TIME_CHANGED",
  "occurredAt": "2026-08-27T19:20:00Z",
  "attraction": {
    "id": "mangrove-run",
    "status": "OPERATING",
    "capacityMode": "NORMAL",
    "waitMinutes": 35,
    "statusMessage": null,
    "updatedAt": "2026-08-27T19:20:00Z",
    "version": 7
  }
}
```

`eventType` is guest-safe (`STATUS_CHANGED`, `CAPACITY_CHANGED`, or `WAIT_TIME_CHANGED`). Actor names and internal reasons are never included. Attraction `version` values let clients ignore an older snapshot or a duplicate event after reconnect.

Incident payloads include current operational state for cache updates, without internal descriptions, actors, or reasons:

```json
{
  "eventId": "activity-uuid",
  "eventType": "INCIDENT_REPORTED",
  "occurredAt": "2026-09-01T15:30:00Z",
  "incident": {
    "id": "incident-uuid",
    "title": "Lightning activity near western basin",
    "type": "WEATHER",
    "severity": "MAJOR",
    "status": "REPORTED",
    "assignedTo": null,
    "guestAdvisoryPublished": false,
    "guestTitle": null,
    "guestMessage": null,
    "affectedAttractionIds": ["mangrove-run"],
    "updatedAt": "2026-09-01T15:30:00Z",
    "version": 1
  }
}
```

Follow the attraction stream:

```bash
curl -N -H 'Accept: text/event-stream' http://localhost:8080/api/v1/attractions/events
```

Follow the operator console stream (requires an operator bearer token):

```bash
curl -N -H 'Accept: text/event-stream' \
  -H 'Authorization: Bearer local-development-token' \
  http://localhost:8080/api/v1/operator/events
```

In another terminal, publish a wait-time change:

```bash
curl -s -X POST http://localhost:8080/api/v1/operator/attractions/mangrove-run/commands \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer local-development-token' \
  -d '{"type":"UPDATE_WAIT_TIME","waitMinutes":35,"expectedVersion":0}'
```

If a client disconnects, it receives current state on reconnection rather than every event it missed. A durable outbox and replay mechanism can be added later.

### Operator operations

```http
GET  /api/v1/operator/dashboard
GET  /api/v1/operator/attractions/{attractionId}
GET  /api/v1/operator/attractions/{attractionId}/activity
POST /api/v1/operator/attractions/{attractionId}/commands
```

Operator commands require `Authorization: Bearer` (Cognito access token, or `local-development-token` in `LOCAL_JWT` mode). `local-operator-token` is an operator-only LOCAL_JWT token without `venueops/advisories.publish`. Example:

```json
{
  "type": "PLACE_WEATHER_HOLD",
  "reason": "Lightning detected within operating radius",
  "expectedVersion": 0
}
```

`expectedVersion` is the attraction `version` from the last read. A mismatch returns `409` with code `STALE_VERSION`.

### Guest advisories

Contract (allowlist, forbidden fields, SSE event names): [guest-advisory-contract.md](../lumen-marsh-platform/docs/guest-advisory-contract.md). Guests use `GET /api/v1/advisories` — not `/api/v1/guest/advisories`.

```http
GET /api/v1/advisories
```

Returns only unresolved incidents that currently have a published public title and message:

```json
{
  "id": "incident-uuid",
  "severity": "MAJOR",
  "title": "Weather advisory",
  "message": "Some outdoor attractions are temporarily paused.",
  "affectedAttractionIds": ["mangrove-run", "cypress-coil"],
  "updatedAt": "2026-09-01T15:30:00Z"
}
```

Guest responses never include internal descriptions, operator identities, assignment, reasons, or activity history. Resolving an incident or withdrawing its advisory removes it from this list.

### Operator incidents

```http
POST /api/v1/operator/incidents
GET  /api/v1/operator/incidents
GET  /api/v1/operator/incidents/{incidentId}
POST /api/v1/operator/incidents/{incidentId}/commands
GET  /api/v1/operator/incidents/{incidentId}/activity
```

Creation example:

```json
{
  "title": "Lightning activity near western basin",
  "type": "WEATHER",
  "severity": "MAJOR",
  "internalDescription": "Repeated strikes detected within the hold radius.",
  "attractionIds": ["mangrove-run", "cypress-coil"]
}
```

Incident creation is itself an audited event: the aggregate starts at version `1` with `IncidentReported` from version `0` to `1`. Linking an attraction to an incident does **not** change attraction state. Weather holds remain explicit attraction commands.

### Weather recommendation inbox

```http
POST /api/v1/integrations/weather/recommendations
GET  /api/v1/operator/weather/recommendations
GET  /api/v1/operator/weather/recommendations/{recommendationId}
POST /api/v1/operator/weather/recommendations/{recommendationId}/commands
GET  /api/v1/operator/events
```

Environmental Monitor posts recommendations. VenueOps does not call attraction commands from this inbox. Operators still place weather holds explicitly.

Ingest payload (matches the Python mapper):

```json
{
  "recommendationId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "ruleId": "simulated-lightning-hold",
  "status": "ACTIVE",
  "severity": "WARNING",
  "summary": "Place Mangrove Run and Cypress Coil on weather hold",
  "evidence": "Simulated lightning strike 1.2 miles from the western basin.",
  "recommendedAction": "Place Mangrove Run and Cypress Coil on weather hold",
  "affectedAttractionIds": ["mangrove-run", "cypress-coil"],
  "observedAt": "2026-09-01T16:00:00Z",
  "version": 1
}
```

Idempotency uses recommendation ID and source `version`:

| Delivery | Result |
| --- | --- |
| New ID | Create (`201`) |
| Same ID, newer version | Update (`200`) |
| Same ID, same version | Accept as duplicate with no new activity (`200`) |
| Same ID, older version | Reject as stale (`409` `STALE_VERSION`) |

`simulated` is derived from a `simulated-` rule ID or evidence that mentions a simulated source. Operator commands are `ACKNOWLEDGE`, `DISMISS`, and `LINK_INCIDENT`. `LINK_INCIDENT` requires an existing incident ID and `expectedVersion`.

Incident commands:

| Command | Additional data |
| --- | --- |
| `ACKNOWLEDGE` | Optional reason |
| `ASSIGN` | `assignee` |
| `START_MITIGATION` | Optional reason |
| `CHANGE_SEVERITY` | `severity`, required reason |
| `LINK_ATTRACTION` | `attractionId` |
| `UNLINK_ATTRACTION` | `attractionId`, required reason |
| `PUBLISH_GUEST_ADVISORY` | `guestTitle`, `guestMessage` |
| `WITHDRAW_GUEST_ADVISORY` | Required reason |
| `RESOLVE` | Required reason |

Every command also carries `expectedVersion`. Guest copy is never derived from `internalDescription`.

### Error responses

Spring `ProblemDetail` JSON with a stable `code`:

| Situation | HTTP status | Code |
| --- | ---: | --- |
| Unknown attraction | `404` | `ATTRACTION_NOT_FOUND` |
| Unknown incident | `404` | `INCIDENT_NOT_FOUND` |
| Unknown weather recommendation | `404` | `WEATHER_RECOMMENDATION_NOT_FOUND` |
| Unknown maintenance resource | `404` | `MAINTENANCE_NOT_FOUND` |
| Invalid request | `400` | `INVALID_REQUEST` |
| Invalid transition | `409` | `INVALID_TRANSITION` |
| Stale expected version | `409` | `STALE_VERSION` |
| Maintenance prerequisite not met | `422` | `MAINTENANCE_PREREQUISITE` |
| Active P1/P2 work orders on incident resolve | `422` | `ACTIVE_WORK_ORDERS` |
| Unexpected failure | `500` | `INTERNAL_ERROR` |

## Weather-hold demonstration

With the app running (`./gradlew bootRun`):

```bash
# 1. Mangrove Run is operating with a 25-minute wait
curl -s http://localhost:8080/api/v1/attractions/mangrove-run

# 2-5. Place a weather hold: wait becomes null, capacity NOT_APPLICABLE, guest-safe message
curl -s -X POST http://localhost:8080/api/v1/operator/attractions/mangrove-run/commands \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer local-development-token' \
  -d '{"type":"PLACE_WEATHER_HOLD","reason":"Lightning detected within operating radius","expectedVersion":0}'

curl -s http://localhost:8080/api/v1/attractions/mangrove-run

# 6. Direct return to operation is rejected
curl -s -X POST http://localhost:8080/api/v1/operator/attractions/mangrove-run/commands \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer local-development-token' \
  -d '{"type":"APPROVE_RETURN_TO_SERVICE","reason":"Skip testing","expectedVersion":1}'

# 7-10. Clear hold, complete testing, approve return to service
curl -s -X POST http://localhost:8080/api/v1/operator/attractions/mangrove-run/commands \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer local-development-token' \
  -d '{"type":"CLEAR_WEATHER_HOLD","reason":"Storm cell moved out of radius","expectedVersion":1}'

curl -s -X POST http://localhost:8080/api/v1/operator/attractions/mangrove-run/commands \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer local-development-token' \
  -d '{"type":"COMPLETE_TESTING","expectedVersion":2}'

curl -s -X POST http://localhost:8080/api/v1/operator/attractions/mangrove-run/commands \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer local-development-token' \
  -d '{"type":"APPROVE_RETURN_TO_SERVICE","reason":"Return to service approved","expectedVersion":3}'

# 11-12. Operating again; complete ordered history
curl -s http://localhost:8080/api/v1/attractions/mangrove-run
curl -s http://localhost:8080/api/v1/operator/attractions/mangrove-run/activity
```

Stale concurrent command example:

```bash
curl -s -X POST http://localhost:8080/api/v1/operator/attractions/mangrove-run/commands \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer local-development-token' \
  -d '{"type":"PLACE_WEATHER_HOLD","reason":"Lightning nearby","expectedVersion":0}'
```

If the attraction has already moved past version `0`, this returns `409` `STALE_VERSION`.

## Incident lifecycle

```mermaid
stateDiagram-v2
  [*] --> REPORTED: report
  REPORTED --> ACKNOWLEDGED: ACKNOWLEDGE
  ACKNOWLEDGED --> MITIGATING: START_MITIGATION
  MITIGATING --> RESOLVED: RESOLVE
```

Assign, severity changes, attraction links, and guest advisories are allowed on any unresolved incident. Resolved incidents cannot be modified. Duplicate attraction links are rejected. Resolving an incident unpublishes its guest advisory.

## Weather-incident demonstration

With the app running (`./gradlew bootRun`):

```bash
# 1. Report a major weather incident
INCIDENT=$(curl -s -X POST http://localhost:8080/api/v1/operator/incidents \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer local-development-token' \
  -d '{"title":"Lightning activity near western basin","type":"WEATHER","severity":"MAJOR","internalDescription":"Repeated strikes detected within the hold radius."}')
echo "$INCIDENT"
ID=$(python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])' <<<"$INCIDENT")

# 2. Link Mangrove Run and Cypress Coil (does not change attraction status)
curl -s -X POST http://localhost:8080/api/v1/operator/incidents/$ID/commands \
  -H 'Content-Type: application/json' -H 'Authorization: Bearer local-development-token' \
  -d '{"type":"LINK_ATTRACTION","attractionId":"mangrove-run","expectedVersion":1}'
curl -s -X POST http://localhost:8080/api/v1/operator/incidents/$ID/commands \
  -H 'Content-Type: application/json' -H 'Authorization: Bearer local-development-token' \
  -d '{"type":"LINK_ATTRACTION","attractionId":"cypress-coil","expectedVersion":2}'

# 3-6. Acknowledge, assign Control Tower, publish a guest advisory, start mitigation
curl -s -X POST http://localhost:8080/api/v1/operator/incidents/$ID/commands \
  -H 'Content-Type: application/json' -H 'Authorization: Bearer local-development-token' \
  -d '{"type":"ACKNOWLEDGE","expectedVersion":3}'
curl -s -X POST http://localhost:8080/api/v1/operator/incidents/$ID/commands \
  -H 'Content-Type: application/json' -H 'Authorization: Bearer local-development-token' \
  -d '{"type":"ASSIGN","assignee":"Control Tower","expectedVersion":4}'
curl -s -X POST http://localhost:8080/api/v1/operator/incidents/$ID/commands \
  -H 'Content-Type: application/json' -H 'Authorization: Bearer local-development-token' \
  -d '{"type":"PUBLISH_GUEST_ADVISORY","guestTitle":"Weather advisory","guestMessage":"Some outdoor attractions are temporarily paused.","expectedVersion":5}'
curl -s -X POST http://localhost:8080/api/v1/operator/incidents/$ID/commands \
  -H 'Content-Type: application/json' -H 'Authorization: Bearer local-development-token' \
  -d '{"type":"START_MITIGATION","expectedVersion":6}'

curl -s http://localhost:8080/api/v1/advisories

# 7. Explicitly place Mangrove Run on weather hold
curl -s -X POST http://localhost:8080/api/v1/operator/attractions/mangrove-run/commands \
  -H 'Content-Type: application/json' -H 'Authorization: Bearer local-development-token' \
  -d '{"type":"PLACE_WEATHER_HOLD","reason":"Lightning detected within operating radius","expectedVersion":0}'

# Cypress Coil starts CLOSED, so open it before a weather hold
curl -s -X POST http://localhost:8080/api/v1/operator/attractions/cypress-coil/commands \
  -H 'Content-Type: application/json' -H 'Authorization: Bearer local-development-token' \
  -d '{"type":"START_TESTING","expectedVersion":0}'
curl -s -X POST http://localhost:8080/api/v1/operator/attractions/cypress-coil/commands \
  -H 'Content-Type: application/json' -H 'Authorization: Bearer local-development-token' \
  -d '{"type":"COMPLETE_TESTING","expectedVersion":1}'
curl -s -X POST http://localhost:8080/api/v1/operator/attractions/cypress-coil/commands \
  -H 'Content-Type: application/json' -H 'Authorization: Bearer local-development-token' \
  -d '{"type":"APPROVE_RETURN_TO_SERVICE","reason":"Opened to apply weather hold","expectedVersion":2}'
curl -s -X POST http://localhost:8080/api/v1/operator/attractions/cypress-coil/commands \
  -H 'Content-Type: application/json' -H 'Authorization: Bearer local-development-token' \
  -d '{"type":"PLACE_WEATHER_HOLD","reason":"Lightning detected within operating radius","expectedVersion":3}'

# 8. Operator activity for both domains
curl -s http://localhost:8080/api/v1/operator/incidents/$ID/activity
curl -s http://localhost:8080/api/v1/operator/attractions/mangrove-run/activity

# 9-10. Resolve the incident; guest advisory disappears; attractions stay on hold
curl -s -X POST http://localhost:8080/api/v1/operator/incidents/$ID/commands \
  -H 'Content-Type: application/json' -H 'Authorization: Bearer local-development-token' \
  -d '{"type":"RESOLVE","reason":"Storm cell moved out of radius","expectedVersion":7}'
curl -s http://localhost:8080/api/v1/advisories

# 11. Explicitly return attractions to service
curl -s -X POST http://localhost:8080/api/v1/operator/attractions/mangrove-run/commands \
  -H 'Content-Type: application/json' -H 'Authorization: Bearer local-development-token' \
  -d '{"type":"CLEAR_WEATHER_HOLD","reason":"Storm cell moved out of radius","expectedVersion":1}'
curl -s -X POST http://localhost:8080/api/v1/operator/attractions/mangrove-run/commands \
  -H 'Content-Type: application/json' -H 'Authorization: Bearer local-development-token' \
  -d '{"type":"COMPLETE_TESTING","expectedVersion":2}'
curl -s -X POST http://localhost:8080/api/v1/operator/attractions/mangrove-run/commands \
  -H 'Content-Type: application/json' -H 'Authorization: Bearer local-development-token' \
  -d '{"type":"APPROVE_RETURN_TO_SERVICE","reason":"Return to service approved","expectedVersion":3}'
```

## Recommendation-to-operator handoff

With VenueOps running (`./gradlew bootRun`) and Environmental Monitor posting to this API:

```bash
# 1. Environmental Monitor (or a replay) delivers an active lightning recommendation
curl -s -X POST http://localhost:8080/api/v1/integrations/weather/recommendations \
  -H 'Content-Type: application/json' \
  -d '{"recommendationId":"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee","ruleId":"simulated-lightning-hold","status":"ACTIVE","severity":"WARNING","summary":"Place Mangrove Run and Cypress Coil on weather hold","evidence":"Simulated lightning strike 1.2 miles from the western basin; this is a demonstration signal, not an NWS observation.","recommendedAction":"Place Mangrove Run and Cypress Coil on weather hold","affectedAttractionIds":["mangrove-run","cypress-coil"],"observedAt":"2026-09-01T16:00:00Z","version":1}'

# 2. A retried delivery of the same source version is accepted without duplicating activity
curl -s -X POST http://localhost:8080/api/v1/integrations/weather/recommendations \
  -H 'Content-Type: application/json' \
  -d '{"recommendationId":"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee","ruleId":"simulated-lightning-hold","status":"ACTIVE","severity":"WARNING","summary":"Place Mangrove Run and Cypress Coil on weather hold","evidence":"Simulated lightning strike 1.2 miles from the western basin; this is a demonstration signal, not an NWS observation.","recommendedAction":"Place Mangrove Run and Cypress Coil on weather hold","affectedAttractionIds":["mangrove-run","cypress-coil"],"observedAt":"2026-09-01T16:00:00Z","version":1}'

curl -s http://localhost:8080/api/v1/operator/weather/recommendations

# 3. Control Tower reviews a prefilled weather incident, then links it
INCIDENT=$(curl -s -X POST http://localhost:8080/api/v1/operator/incidents \
  -H 'Content-Type: application/json' -H 'Authorization: Bearer local-development-token' \
  -d '{"title":"Place Mangrove Run and Cypress Coil on weather hold","type":"WEATHER","severity":"MAJOR","internalDescription":"Simulated lightning strike 1.2 miles from the western basin.","attractionIds":["mangrove-run","cypress-coil"]}')
ID=$(python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])' <<<"$INCIDENT")

curl -s -X POST http://localhost:8080/api/v1/operator/weather/recommendations/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/commands \
  -H 'Content-Type: application/json' -H 'Authorization: Bearer local-development-token' \
  -d "{\"type\":\"LINK_INCIDENT\",\"incidentId\":\"$ID\",\"expectedVersion\":1}"

# 4. Operator explicitly holds Mangrove Run (Cypress Coil starts CLOSED)
curl -s -X POST http://localhost:8080/api/v1/operator/attractions/mangrove-run/commands \
  -H 'Content-Type: application/json' -H 'Authorization: Bearer local-development-token' \
  -d '{"type":"PLACE_WEATHER_HOLD","reason":"Lightning detected within operating radius","expectedVersion":0}'

# 5. Environmental Monitor later sends the cleared source version
curl -s -X POST http://localhost:8080/api/v1/integrations/weather/recommendations \
  -H 'Content-Type: application/json' \
  -d '{"recommendationId":"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee","ruleId":"simulated-lightning-hold","status":"CLEARED","severity":"INFO","summary":"Lightning hold can be reviewed for clearance","evidence":"Simulated lightning is outside the hold radius.","recommendedAction":"Review weather holds for return-to-service","affectedAttractionIds":["mangrove-run","cypress-coil"],"observedAt":"2026-09-01T16:30:00Z","version":2}'
```

The operator stream emits `weather.recommendation.updated` then `weather.recommendation.cleared`. Guest Flutter streams never receive those events. Linking or clearing a recommendation does not change attraction status.

### Operator maintenance

```http
GET  /api/v1/operator/maintenance/assets
GET  /api/v1/operator/maintenance/assets/{assetId}
GET  /api/v1/operator/maintenance/assets/{assetId}/work-orders
GET  /api/v1/operator/maintenance/work-orders
POST /api/v1/operator/maintenance/work-orders
GET  /api/v1/operator/maintenance/work-orders/{workOrderId}
POST /api/v1/operator/maintenance/work-orders/{workOrderId}/commands
GET  /api/v1/operator/maintenance/work-orders/{workOrderId}/activity
GET  /api/v1/operator/maintenance/recommendations
POST /api/v1/operator/maintenance/recommendations/{recommendationId}/commands
POST /api/v1/integrations/reliability/recommendations
```

Maintenance work orders never reopen an attraction. `COMPLETE` is blocked while the attraction is in `TECHNICAL_DELAY`, `TESTING`, or `RETURNING_TO_SERVICE`. `CLOSED`, `OPERATING`, and `WEATHER_HOLD` are allowed so overnight work can finish on a closed attraction and weather holds stay independent of maintenance completion. Reliability ingest creates a pending recommendation, not a work order. Machine tokens use `Authorization: Bearer local-reliability-token` in `LOCAL_JWT` mode.

Work-order commands: `OPEN`, `ASSIGN`, `START_WORK`, `REQUEST_INSPECTION`, `REJECT_INSPECTION`, `APPROVE_INSPECTION`, `COMPLETE`, `CANCEL`, `REASSIGN`, `SET_ESTIMATED_RESTORE`, `RECORD_CHECKLIST_RESULT`, `LINK_INCIDENT`, `ADD_NOTE`, `ADD_EVIDENCE`. Every command requires `commandId` and `expectedVersion`. Duplicate `commandId` values replay the original result. Reusing a `commandId` against a different work-order URL returns `409` with code `DUPLICATE_COMMAND`.

Recommendation commands: `ACCEPT`, `DISMISS`. Every command requires `commandId` and `expectedVersion`. `ACCEPT` claims the recommendation before creating a work order so concurrent accepts cannot create duplicates.

## Architecture

```text
com.deanwagman.lumenmarsh.venueops
├── attraction/
│   ├── domain/           # plain Java aggregate, state machine, events
│   ├── application/      # service and repository port
│   ├── api/              # HTTP controllers, ProblemDetail mapping, SSE
│   └── infrastructure/   # in-memory adapter, seed data, JPA/Flyway, SSE broadcaster
├── incident/
│   ├── domain/           # incident aggregate, lifecycle, guest-advisory rules
│   ├── application/      # service, repository port, operational updates
│   ├── api/              # operator API, guest advisories, park-wide SSE
│   └── infrastructure/   # in-memory and JPA adapters
├── weather/
    ├── domain/           # recommendation inbox aggregate and idempotent ingest
    ├── application/      # service, repository port, operator SSE updates
    ├── api/              # integration ingest, operator commands, operator SSE
    └── infrastructure/   # in-memory and JPA adapters
└── maintenance/
    ├── domain/           # assets, work orders, checklists, recommendations
    ├── application/      # commands, queries, incident/attraction gates
    ├── api/              # operator maintenance API and reliability ingest
    └── infrastructure/   # in-memory and JPA adapters, Cypress Coil seed
```

The domain packages do not import Spring, JPA, HTTP, or JDBC. Persistence is selected with `venueops.attractions.persistence`:

- `memory` (default): in-memory repository for local demonstration
- `jpa`: PostgreSQL via Flyway migrations and optimistic version checks

Demo attractions are loaded only when `venueops.attractions.seed=true`. The in-memory and JPA repositories both reject stale versions on save.

## Deferred scope

Maps, show schedules, wait prediction, notifications, Kafka/SQS, AI, multiple parks, food ordering, purchasing, and technician workforce identity.
