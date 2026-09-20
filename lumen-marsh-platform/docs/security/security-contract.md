# Lumen Marsh security contract

**Status:** Implemented for the running stack (Phases 4–11 in application code)  
**Owners:** VenueOps API, VenueOps Console, Environmental Monitor, Park Flow Intelligence, Reliability Intelligence, Platform  
**Related ADR:** [ADR 0001 — Cognito and JWT](./adr/0001-cognito-jwt.md)

This document is the shared security boundary. The Java API and React console implement it independently. The API remains authoritative: hiding a console button is not authorization.

## Goals

- Guests anonymously view attractions, wait times, advisories, media, and guest live updates.
- Operators sign in before using Control Tower.
- Supervisors receive elevated operational permissions.
- Environmental Monitor authenticates as a service, not a human.
- Park Flow Intelligence authenticates as a service, not a human.
- Reliability Intelligence authenticates as a service, not a human.
- VenueOps derives audit identities from verified credentials — never from `X-Actor`.
- REST commands and the operator event stream enforce the same rules.

## Deny by default

Every route is denied unless it is explicitly classified below as **Public**, **Operator**, **Supervisor**, **Integration**, or **Internal**.

Internal routes are unavailable outside trusted local/dev tooling and must be disabled or protected in shared/demo deployments.

## Identities

| Identity | Authentication | Principal purpose |
|---|---|---|
| Guest | None | Public park reads and guest SSE |
| Operator | Cognito user access token (group `operators`) | Day-to-day Control Tower work |
| Supervisor | Cognito user access token (group `supervisors`) | Guest-facing publish and high-impact resolution |
| Weather service | Cognito client-credentials token | Submit weather recommendations only |
| Reliability Intelligence | Cognito client-credentials token | Submit reliability recommendations only. Never creates a work order. |
| Park Flow Intelligence | Cognito client-credentials token | Submit queue observations and forecasts only |
| VenueOps system | Internal process identity | Automated system events (no external token) |

### Cognito groups

- `operators`
- `supervisors` (implies operator capabilities)

A user may belong to both groups. Supervisor membership grants every operator permission plus supervisor-only commands.

### OAuth resource and scopes

Resource server identifier (scope prefix): `venueops`. Cognito access tokens identify the calling app with `client_id`, not `aud=venueops`.

| Scope | Intended holder | Grants |
|---|---|---|
| `venueops/operator.read` | Console (operators, supervisors) | Operator GET endpoints and operator SSE. Maintenance snapshot/update events on that stream additionally require `venueops/maintenance.read`. |
| `venueops/attractions.command` | Console | Attraction command POSTs |
| `venueops/incidents.command` | Console | Incident create/list/get/activity and non-supervisor incident commands |
| `venueops/advisories.publish` | Console (supervisors) | Publish/withdraw guest advisories |
| `venueops/weather-recommendations.review` | Console | Weather inbox reads and ACKNOWLEDGE / DISMISS / LINK_INCIDENT |
| `venueops/maintenance.read` | Console | Maintenance asset and work-order reads |
| `venueops/maintenance.command` | Console | Create, assign, and update maintenance work |
| `venueops/maintenance.inspect` | Console (supervisors) | Approve/reject inspection and complete work orders |
| `venueops/flow.read` | Console | Park flow reads and operator flow SSE events |
| `venueops/flow.command` | Console | Approve or dismiss flow recommendations |
| `venueops/flow.publish` | Console (supervisors) | Publish or withdraw guest flow guidance |
| `venueops/weather-recommendations.write` | Environmental Monitor only | `POST /api/v1/integrations/weather/recommendations` |
| `venueops/reliability.write` | Reliability integration only | `POST /api/v1/integrations/reliability/recommendations` |
| `venueops/flow-ingest.write` | Park Flow Intelligence only | `POST /api/v1/integrations/flow/observations` and `/forecasts` |

Human tokens must never receive `venueops/weather-recommendations.write`, `venueops/reliability.write`, or `venueops/flow-ingest.write`.
The weather-service client must receive only the weather write scope.
The reliability-integration client must receive only `venueops/reliability.write`.
The park-flow-intelligence client must receive only `venueops/flow-ingest.write`.

## Token claims

VenueOps validates access tokens with at least:

| Claim | Requirement |
|---|---|
| `iss` | Exact Cognito user-pool issuer URL |
| `token_use` | Must be `access` |
| `client_id` | Must match an allowed app client (console, Environmental Monitor, Park Flow Intelligence, or reliability ingest) |
| `sub` | Present; durable subject for humans and services |
| `scope` / `scp` | Space-delimited scopes used for authorization |
| `cognito:groups` | Mapped to `ROLE_OPERATOR` / `ROLE_SUPERVISOR` |
| `exp` | Required; reject expired tokens |

Optional display claims (`email`, `preferred_username`, `name`) may populate UI labels and activity **display snapshots** only. They are not durable identity keys.

## Audit identity

### Value object (target model)

```text
ActorIdentity
  subject       // Cognito sub, or fixed system/service id
  displayName   // snapshot for humans; descriptive label for services
  type          // HUMAN | SERVICE | SYSTEM | LEGACY
  issuer        // token iss, or "venueops" for SYSTEM
```

### Recording rules

| Actor type | When | `subject` | `displayName` |
|---|---|---|---|
| `HUMAN` | Operator/supervisor command | Cognito `sub` | Best-effort name/email from claims at event time |
| `SERVICE` | Weather recommendation ingest | Service client subject / client id policy | e.g. `environmental-monitor` |
| `SYSTEM` | Internal automation | `venueops-system` | `VenueOps system` |
| `LEGACY` | Backfilled pre-auth activity | Prior `X-Actor` string or `unknown` | Same as historic actor string |

### UI vs history

- **UI** may show `displayName` (and role badges).
- **Permanent audit key** is `subject` + `actor_type` (+ `issuer` when available).
- Spoofed `X-Actor` headers must be ignored after Phase 4 and must not change recorded identity.

## Endpoint classification

Full inventory: [endpoint-inventory.md](./endpoint-inventory.md).

### Public (anonymous)

- `GET /api/v1/attractions`
- `GET /api/v1/attractions/{id}`
- `GET /api/v1/attractions/events`
- `GET /api/v1/advisories` (guest advisory contract: [guest-advisory-contract.md](../guest-advisory-contract.md); not `/api/v1/guest/advisories`)
- `GET /api/v1/events`
- `GET /api/v1/flow/overview`
- `GET /api/v1/flow/recommendations`
- `GET /api/v1/attractions/{id}/wait-forecast`
- `GET /media/**`
- `GET /actuator/health` (and liveness/readiness equivalents if exposed)

### Operator

Requires authenticated operator or supervisor with matching scopes:

- All `/api/v1/operator/**` reads
- `GET /api/v1/operator/events` (maintenance SSE payloads require `venueops/maintenance.read` in addition to stream access)
- Attraction commands (all current `AttractionCommand` values)
- Incident report + operator incident commands except supervisor-only ones
- Weather recommendation inbox + `ACKNOWLEDGE` / `DISMISS` / `LINK_INCIDENT`
- Maintenance asset and work-order reads
- Maintenance commands except supervisor inspection/completion (`OPEN`, `ASSIGN`, `START_WORK`, `REQUEST_INSPECTION`, `SET_ESTIMATED_RESTORE`, recommendation `ACCEPT` / `DISMISS`)
- Flow reads and `APPROVE` / `DISMISS`

### Supervisor

Requires supervisor group **and** `venueops/advisories.publish` (for advisory commands):

- Incident command `PUBLISH_GUEST_ADVISORY`
- Incident command `WITHDRAW_GUEST_ADVISORY`
- Incident command `RESOLVE` when severity is `MAJOR` or `CRITICAL`
- Maintenance `APPROVE_INSPECTION`, `REJECT_INSPECTION`, and `COMPLETE`
- Maintenance `CANCEL` for P1/P2 work orders
- Flow `PUBLISH` / `WITHDRAW` (requires `venueops/flow.publish`)
- Future emergency / override commands (none yet)

`RESOLVE` for `MINOR` / `MODERATE` remains an operator capability.

### Integration

Requires scope `venueops/weather-recommendations.write`:

- `POST /api/v1/integrations/weather/recommendations`

Requires scope `venueops/reliability.write`:

- `POST /api/v1/integrations/reliability/recommendations`

Requires scope `venueops/flow-ingest.write`:

- `POST /api/v1/integrations/flow/observations`
- `POST /api/v1/integrations/flow/forecasts`

### Denied / not public

- `GET /api/hello` — legacy probe; deny in secured deployments
- SpringDoc / Swagger UI (`/swagger-ui/**`, `/v3/api-docs/**`) — local/dev only; deny or protect in demo/prod
- All other Actuator endpoints — deny

## Role × permission matrix

| Capability | Guest | Operator | Supervisor | Weather service | Reliability ingest | Park Flow Intelligence |
|---|---|---|---|---|---|---|
| Guest attraction/advisory/media/SSE/flow reads | ✓ | ✓ | ✓ | ✓ (unnecessary) | ✓ (unnecessary) | ✓ (unnecessary) |
| Operator reads + operator SSE | | ✓ | ✓ | | | |
| Attraction commands | | ✓ | ✓ | | | |
| Report / manage incidents (non-supervisor cmds) | | ✓ | ✓ | | | |
| Publish / withdraw guest advisory | | | ✓ | | | |
| Resolve MAJOR/CRITICAL incident | | | ✓ | | | |
| Resolve MINOR/MODERATE incident | | ✓ | ✓ | | | |
| Review weather recommendations | | ✓ | ✓ | | | |
| Write weather recommendations | | | | ✓ | | |
| Read maintenance data | | ✓ | ✓ | | | |
| Create / assign / update maintenance work | | ✓ | ✓ | | | |
| Approve inspection or complete work orders | | | ✓ | | | |
| Write reliability recommendations | | | | | ✓ | |
| Read park flow | | ✓ | ✓ | | | |
| Approve / dismiss flow recommendations | | ✓ | ✓ | | | |
| Publish / withdraw guest flow guidance | | | ✓ | | | |
| Write flow observations and forecasts | | | | | | ✓ |

Park Flow Intelligence is a Compose producer and a machine client. It may call only the flow ingest routes and cannot use operator endpoints. Reliability Intelligence is the same kind of Compose producer for reliability ingest; it never creates a work order.

## Client architecture

### `venueops-console` (public Cognito app client)

- No client secret
- Authorization Code + PKCE
- Scopes: operator read/command scopes (supervisors receive advisory publish via group + scope assignment)
- Tokens attached as `Authorization: Bearer` on REST and operator SSE (fetch-based SSE; never query-string tokens)

### `environmental-monitor` (confidential Cognito app client)

- Client secret via local env / Secrets Manager / SSM — never Git or ordinary Compose files
- Client Credentials
- Scope: `venueops/weather-recommendations.write` only

### `reliability-integration` (confidential Cognito app client)

- Client secret via local env / Secrets Manager / SSM — never Git or ordinary Compose files
- Client Credentials
- Scope: `venueops/reliability.write` only
- Reliability Intelligence is the Compose producer. Scripts and tests may also POST ingest with `local-reliability-token` in `LOCAL_JWT` mode.

### `park-flow-intelligence` (confidential Cognito app client)

- Client secret via local env / Secrets Manager / SSM — never Git or ordinary Compose files
- Client Credentials
- Scope: `venueops/flow-ingest.write` only

## Error contract

| Condition | HTTP | Notes |
|---|---|---|
| Missing / invalid / expired token on protected route | `401` | Problem Details JSON; no token contents |
| Valid token, insufficient role/scope | `403` | Problem Details JSON |
| Public route | `200`/`…` | No auth required |

## Explicit non-goals

- Guest Cognito login
- Fine-grained per-attraction ACLs
- Mutual TLS between services
- AWS-hosted Reliability Intelligence (local Compose only for this milestone)

## Acceptance anchors

These scenarios must remain true after Phases 1–9:

1. Guest reads attractions and guest SSE without a token.
2. Guest cannot call attraction or incident commands (`401`).
3. Operator can change attraction state and review recommendations.
4. Operator cannot publish a guest advisory (`403`).
5. Supervisor can publish advisories and resolve major incidents.
6. Weather service can POST recommendations and cannot call `/api/v1/operator/**` (`403`).
7. Reliability ingest can POST recommendations and cannot call `/api/v1/operator/**` (`403`).
8. Forged `X-Actor` does not change audit identity.
9. Operator SSE requires the same session as REST; expired tokens stop reconnect and request sign-in.
10. Park Flow Intelligence can POST observations/forecasts and cannot call `/api/v1/operator/**` (`403`).
11. Operator cannot approve maintenance inspection (`403`); supervisor can.
