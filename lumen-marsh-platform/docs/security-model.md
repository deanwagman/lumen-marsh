# VenueOps security contract

**Status:** Access rules defined from controller inspection. Cognito is not required to accept this document.
**Source:** every `@RestController` in `venueops-api` plus static `/media/**` and Actuator.
**First checkpoint:** guest read of Mangrove Run; operator sign-in; operator attraction command; anonymous command rejected; activity history records the authenticated operator.

Flutter, Environmental Monitor, and the operator event stream stay out of scope until that checkpoint works.

## Policy

| Area | Access |
|---|---|
| Guest attraction reads | Public |
| Published advisory reads | Public |
| Guest SSE | Public |
| Attraction commands | Operator |
| Incident commands | Operator |
| Recommendation review | Operator |
| Advisory publishing | Supervisor |
| Weather recommendation ingestion | Weather service |
| Reliability recommendation ingestion | Reliability service |
| Media and health | Public |
| Everything else | Denied |

Operator **queries** (dashboard reads and activity history) are Operator. They are required so an operator can see the attraction they are about to command and later confirm the audit record. They are not anonymous.

Supervisors inherit every operator permission.

## Identities

| Identity | How it authenticates | Role | Durable audit `subject` |
|---|---|---|---|
| Guest | None | — | None (no audit write) |
| Operator | Human access token, group `operators` | `OPERATOR` | JWT `sub` |
| Supervisor | Human access token, group `supervisors` | `SUPERVISOR` | JWT `sub` |
| Reliability service | Client-credentials token | `RELIABILITY_SERVICE` | Token `sub` / client id |
| VenueOps system | Internal process only | `SYSTEM` | `venueops-system` |

Client-supplied `X-Actor` is not identity. Protected commands must ignore or reject it.

### Scopes

| Scope | Who receives it | Used for |
|---|---|---|
| `venueops/operator.read` | Operators, supervisors | Operator GET routes (and operator SSE later) |
| `venueops/attractions.command` | Operators, supervisors | Attraction command POST |
| `venueops/incidents.command` | Operators, supervisors | Incident report and non-publish incident commands |
| `venueops/advisories.publish` | Supervisors | Publish / withdraw guest advisory |
| `venueops/weather-recommendations.review` | Operators, supervisors | Weather inbox reads and review commands |
| `venueops/maintenance.read` | Operators, supervisors | Maintenance asset and work-order reads |
| `venueops/maintenance.command` | Operators, supervisors | Create, assign, and update work orders |
| `venueops/maintenance.inspect` | Supervisors | Inspection approval and work-order completion |
| `venueops/weather-recommendations.write` | Weather service only | Recommendation ingest |
| `venueops/reliability.write` | Reliability service only | Reliability recommendation ingest |

### Claims VenueOps will require on access tokens

`iss` (exact issuer), `token_use` (`access`), `client_id` (an allowed console or monitor app client), `sub`, `scope`/`scp`, `cognito:groups`, `exp`. Signature must verify. Access tokens are not required to carry `aud`; Cognito identifies the caller with `client_id`. Display names (`name`, `email`, `preferred_username`) are presentation snapshots only.

## Route inventory

Kind: **Query** does not write activity. **Command** writes activity (or ingest). **Stream** is SSE.

Audit: **None** means the route does not record an actor. History routes **return** previously recorded identities; they do not create them.

| Method | Path | Controller | Kind | Access | Role / scope | Audit identity |
|---|---|---|---|---|---|---|
| GET | `/api/v1/attractions` | `AttractionController` | Query | Public | — | None |
| GET | `/api/v1/attractions/{attractionId}` | `AttractionController` | Query | Public | — | None |
| GET | `/api/v1/attractions/events` | `AttractionEventController` | Stream | Public | — | None (payload must not include actors) |
| GET | `/api/v1/events` | `ParkEventController` | Stream | Public | — | None (payload must not include actors) |
| GET | `/api/v1/advisories` | `GuestAdvisoryController` | Query | Public | — | None (no operator fields). Contract: [guest-advisory-contract.md](./guest-advisory-contract.md) |
| GET | `/media/**` | Static resources | Query | Public | — | None |
| GET | `/actuator/health` | Actuator | Query | Public | — | None |
| GET | `/api/v1/operator/dashboard` | `OperatorDashboardController` | Query | Protected | `OPERATOR` + `venueops/operator.read` | None |
| GET | `/api/v1/operator/attractions/{attractionId}/activity` | `OperatorAttractionController` | Query | Protected | `OPERATOR` + `venueops/operator.read` | Returns stored actors |
| POST | `/api/v1/operator/attractions/{attractionId}/commands` | `OperatorAttractionController` | Command | Protected | `OPERATOR` + `venueops/attractions.command` | **HUMAN** from JWT (`sub`, display name, issuer) |
| GET | `/api/v1/operator/incidents` | `OperatorIncidentController` | Query | Protected | `OPERATOR` + `venueops/operator.read` | None |
| GET | `/api/v1/operator/incidents/{incidentId}` | `OperatorIncidentController` | Query | Protected | `OPERATOR` + `venueops/operator.read` | None |
| GET | `/api/v1/operator/incidents/{incidentId}/activity` | `OperatorIncidentController` | Query | Protected | `OPERATOR` + `venueops/operator.read` | Returns stored actors |
| POST | `/api/v1/operator/incidents` | `OperatorIncidentController` | Command | Protected | `OPERATOR` + `venueops/incidents.command` | **HUMAN** from JWT |
| POST | `/api/v1/operator/incidents/{incidentId}/commands` | `OperatorIncidentController` | Command | Protected | See incident commands | **HUMAN** from JWT |
| GET | `/api/v1/operator/weather/recommendations` | `OperatorWeatherRecommendationController` | Query | Protected | `OPERATOR` + `venueops/weather-recommendations.review` | None |
| GET | `/api/v1/operator/weather/recommendations/{recommendationId}` | `OperatorWeatherRecommendationController` | Query | Protected | `OPERATOR` + `venueops/weather-recommendations.review` | None |
| POST | `/api/v1/operator/weather/recommendations/{recommendationId}/commands` | `OperatorWeatherRecommendationController` | Command | Protected | `OPERATOR` + `venueops/weather-recommendations.review` | **HUMAN** from JWT |
| GET | `/api/v1/operator/events` | `OperatorEventController` | Stream | Protected | `OPERATOR` + `venueops/operator.read` | None — **deferred** until the attraction checkpoint. `maintenance.work-orders.snapshot` and `maintenance.work-order.updated` additionally require `venueops/maintenance.read` on the same token. |
| GET | `/api/v1/operator/maintenance/assets` | `MaintenanceAssetController` | Query | Protected | `OPERATOR` + `venueops/maintenance.read` | None |
| GET | `/api/v1/operator/maintenance/assets/{assetId}` | `MaintenanceAssetController` | Query | Protected | `OPERATOR` + `venueops/maintenance.read` | None |
| GET | `/api/v1/operator/maintenance/assets/{assetId}/work-orders` | `MaintenanceAssetController` | Query | Protected | `OPERATOR` + `venueops/maintenance.read` | None |
| GET | `/api/v1/operator/maintenance/work-orders` | `MaintenanceWorkOrderController` | Query | Protected | `OPERATOR` + `venueops/maintenance.read` | None |
| GET | `/api/v1/operator/maintenance/work-orders/{workOrderId}` | `MaintenanceWorkOrderController` | Query | Protected | `OPERATOR` + `venueops/maintenance.read` | None |
| GET | `/api/v1/operator/maintenance/work-orders/{workOrderId}/activity` | `MaintenanceWorkOrderController` | Query | Protected | `OPERATOR` + `venueops/maintenance.read` | Returns stored actors |
| POST | `/api/v1/operator/maintenance/work-orders` | `MaintenanceWorkOrderController` | Command | Protected | `OPERATOR` + `venueops/maintenance.command` | **HUMAN** from JWT |
| POST | `/api/v1/operator/maintenance/work-orders/{workOrderId}/commands` | `MaintenanceWorkOrderController` | Command | Protected | See maintenance commands | **HUMAN** from JWT |
| GET | `/api/v1/operator/maintenance/recommendations` | `MaintenanceRecommendationController` | Query | Protected | `OPERATOR` + `venueops/maintenance.read` | None |
| GET | `/api/v1/operator/maintenance/recommendations/{recommendationId}` | `MaintenanceRecommendationController` | Query | Protected | `OPERATOR` + `venueops/maintenance.read` | None |
| POST | `/api/v1/operator/maintenance/recommendations/{recommendationId}/commands` | `MaintenanceRecommendationController` | Command | Protected | `OPERATOR` + `venueops/maintenance.command` | **HUMAN** from JWT |
| POST | `/api/v1/integrations/weather/recommendations` | `WeatherRecommendationIntegrationController` | Command | Protected | `WEATHER_SERVICE` + `venueops/weather-recommendations.write` | **SERVICE** from JWT — **deferred** until the attraction checkpoint |
| POST | `/api/v1/integrations/reliability/recommendations` | `ReliabilityRecommendationIntegrationController` | Command | Protected | `RELIABILITY_SERVICE` + `venueops/reliability.write` | **SERVICE** from JWT |
| GET | `/api/hello` | `HelloController` | Query | Denied | — | None |
| GET | `/swagger-ui/**`, `/v3/api-docs/**` | SpringDoc | Query | Denied outside local/dev | — | None |
| GET | Other Actuator endpoints | Actuator | Query | Denied | — | None |
| * | Any other path | — | — | Denied | — | None |

There is no guest POST for attractions, incidents, or weather. An anonymous attraction command is `401`.

## Attraction commands

All via `POST /api/v1/operator/attractions/{attractionId}/commands`. Access: Operator. Audit: HUMAN.

`START_TESTING`, `COMPLETE_TESTING`, `APPROVE_RETURN_TO_SERVICE`, `PLACE_WEATHER_HOLD`, `CLEAR_WEATHER_HOLD`, `REPORT_TECHNICAL_FAULT`, `COMPLETE_REPAIR`, `CLOSE_FOR_DAY`, `REDUCE_CAPACITY`, `RESTORE_CAPACITY`, `UPDATE_WAIT_TIME`.

No attraction command is supervisor-only. The first protected command for the checkpoint may be any of these (for example `PLACE_WEATHER_HOLD` or `UPDATE_WAIT_TIME` on Mangrove Run).

## Incident commands

All via `POST /api/v1/operator/incidents/{incidentId}/commands`.

| Command | Access | Role / scope | Audit |
|---|---|---|---|
| `ACKNOWLEDGE` | Operator | `venueops/incidents.command` | HUMAN |
| `ASSIGN` | Operator | `venueops/incidents.command` | HUMAN |
| `START_MITIGATION` | Operator | `venueops/incidents.command` | HUMAN |
| `CHANGE_SEVERITY` | Operator | `venueops/incidents.command` | HUMAN |
| `LINK_ATTRACTION` | Operator | `venueops/incidents.command` | HUMAN |
| `UNLINK_ATTRACTION` | Operator | `venueops/incidents.command` | HUMAN |
| `PUBLISH_GUEST_ADVISORY` | Supervisor | `SUPERVISOR` + `venueops/advisories.publish` | HUMAN |
| `WITHDRAW_GUEST_ADVISORY` | Supervisor | `SUPERVISOR` + `venueops/advisories.publish` | HUMAN |
| `RESOLVE` (MINOR / MODERATE) | Operator | `venueops/incidents.command` | HUMAN |
| `RESOLVE` (MAJOR / CRITICAL) | Supervisor | `SUPERVISOR` + `venueops/incidents.command` | HUMAN |

`POST /api/v1/operator/incidents` (report) is an Operator command and records HUMAN.

## Weather recommendation commands

All via `POST /api/v1/operator/weather/recommendations/{id}/commands`. Access: Operator. Audit: HUMAN.

`ACKNOWLEDGE`, `DISMISS`, `LINK_INCIDENT`.

Ingest (`POST /api/v1/integrations/weather/recommendations`) is Weather service, audit SERVICE. Out of scope for the first checkpoint.

## Maintenance commands

Work-order lifecycle via `POST /api/v1/operator/maintenance/work-orders/{id}/commands`. Audit: HUMAN from JWT. Client-supplied actor fields are ignored.

| Command | Access | Role / scope |
|---|---|---|
| `OPEN`, `ASSIGN`, `START_WORK`, `REQUEST_INSPECTION`, `REASSIGN`, `SET_ESTIMATED_RESTORE`, `RECORD_CHECKLIST_RESULT`, `LINK_INCIDENT`, `ADD_NOTE`, `ADD_EVIDENCE` | Operator | `venueops/maintenance.command` |
| `CANCEL` (P3/P4) | Operator | `venueops/maintenance.command` |
| `CANCEL` (P1/P2) | Supervisor | `SUPERVISOR` + `venueops/maintenance.command` |
| `APPROVE_INSPECTION`, `REJECT_INSPECTION`, `COMPLETE` | Supervisor | `SUPERVISOR` + `venueops/maintenance.inspect` |

Ingest (`POST /api/v1/integrations/reliability/recommendations`) is Reliability service, audit SERVICE. A recommendation never becomes a work order until an operator accepts it. `ACCEPT` and `DISMISS` require `commandId` and `expectedVersion`. Work orders never reopen attractions.

## Checkpoint (do this next)

```text
Guest can read Mangrove Run
Operator signs in
Operator changes its state
Anonymous command is rejected
Activity history records the authenticated operator
```

Implementation order after this contract:

1. Provision the development Cognito user pool.
2. Create one operator and one supervisor test account.
3. Validate Cognito JWTs in VenueOps.
4. Protect `POST /api/v1/operator/attractions/{id}/commands`.
5. Record JWT identity on that command; ignore `X-Actor`.
6. Add Cognito login to the console.
7. Prove the checkpoint locally.
8. Expand the same rules to the remaining protected routes above.

Until step 7 passes: do not change Flutter, Environmental Monitor, or operator SSE as part of this work.
