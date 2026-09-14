# Guest Advisory Delivery — UI checklist

This is the short guest-advisory UI checklist. The full storm path is [`storm-lifecycle-demo.md`](./storm-lifecycle-demo.md). Automated HTTP proof: [`../scripts/storm-lifecycle-acceptance.sh`](../scripts/storm-lifecycle-acceptance.sh). Contract: [`guest-advisory-contract.md`](./guest-advisory-contract.md).

Weather-focused long path: [`storm-lifecycle-demo.md`](./storm-lifecycle-demo.md) and [`../scripts/storm-lifecycle-acceptance.sh`](../scripts/storm-lifecycle-acceptance.sh).

## Prerequisites

1. Stack up in OIDC / local JWT mode (`./scripts/dev-up.sh` or equivalent).
2. Control Tower signed in as a **supervisor** with `venueops/advisories.publish`.
3. Flutter guest app open on **Today**.

## Steps

1. In Control Tower, open **Incidents** (or report a new weather incident).
2. Open an unresolved incident workspace.
3. Publish a guest advisory (title + message). Confirm guests see only that copy — not the internal description.
4. In the Flutter guest app, confirm a **Park advisories** banner appears on Today.
5. Tap the banner → `/advisories/:id` detail shows title, message, severity, affected attractions.
6. From Today, open **View all advisories** → inbox lists the same advisory; tap again to confirm detail.
7. Optional HTTP check: `./scripts/advisory-demo.sh` (or inspect `GET /api/v1/advisories` — no `internalDescription` / operator fields).
8. In Control Tower, **Withdraw** the advisory (or **Resolve** the incident).
9. Confirm Flutter Today / inbox clear the advisory (cleared banner is acceptable).
10. Confirm detail for that id shows **Advisory cleared** if reopened from a stale link.
11. Confirm `GET /api/v1/advisories` no longer lists the advisory.

## Pass criteria

- Publish → guest banner/detail → withdraw/resolve → gone.
- Guest JSON never exposes internal description, operators, or activity.
