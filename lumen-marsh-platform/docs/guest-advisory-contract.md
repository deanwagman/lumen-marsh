# Guest advisory contract

Source of truth for the **anonymous guest** advisory surface. Operator publish/withdraw lives on the Control Tower incident API; this document covers only what guests may see.

## Paths

```http
GET /api/v1/advisories
GET /api/v1/events
```

- `GET /api/v1/advisories` — list currently published guest advisories (public, no auth).
- `GET /api/v1/events` — park-wide SSE for guests (attractions + advisories).

**Not used:** `/api/v1/guest/advisories` (and any `/api/v1/guest/**` advisory alias). Do not introduce that path without updating this contract first.

There is **no** `GET /api/v1/advisories/{id}` in the current contract. Guest clients resolve detail views from the list + live catalog (REST load and SSE updates).

## List response allowlist

Each advisory object may include only:

| Field | Type | Notes |
| --- | --- | --- |
| `id` | string | Incident id used as advisory id |
| `severity` | enum | Guest-safe severity projection |
| `title` | string | Published guest title |
| `message` | string | Published guest message |
| `affectedAttractionIds` | string[] | Linked attraction ids |
| `updatedAt` | string (ISO-8601) | Last advisory-relevant update |
| `version` | number | Optimistic-concurrency / merge version |

## Forbidden fields

Guest JSON (REST or SSE advisory payloads) must **never** include:

- `internalDescription`
- `assignedTo` / operator identity / Cognito subject
- `actor`
- Activity history / activity arrays
- Incident command metadata, reasons used only for operator audit
- Any other operator-only incident fields

## Park SSE advisory events

| Event | Meaning |
| --- | --- |
| `advisories.snapshot` | Current published advisories on connect |
| `advisory.published` | Advisory became visible to guests |
| `advisory.updated` | Published advisory content or linkage changed |
| `advisory.withdrawn` | Advisory removed (explicit withdraw or resolve) |

## Lifecycle

1. Supervisor publishes via `PUBLISH_GUEST_ADVISORY` on an open incident (`venueops/advisories.publish`).
2. Advisory appears on `GET /api/v1/advisories` and on the park stream.
3. Explicit `WITHDRAW_GUEST_ADVISORY` or incident `RESOLVE` removes it from the guest list and emits `advisory.withdrawn`.

## Sanitize assertion keys

Automated checks (API tests and `scripts/advisory-demo.sh`) should reject guest payloads that contain any of:

```text
internalDescription
assignedTo
actor
activity
activities
reason
```

(Extend this list here first when new forbidden keys are identified.)

## Related docs

- Operator incident center: [incident-center-spec.md](./incident-center-spec.md)
- Security model / endpoint matrix: [security-model.md](./security-model.md), [security/security-contract.md](./security/security-contract.md)
