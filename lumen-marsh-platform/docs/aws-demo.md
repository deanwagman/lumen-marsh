# AWS demo environment (Phases 10–12) — design only until enable_demo_resources=true

## Target architecture (one visit host)

One HTTPS origin on one EC2 instance. Caddy is the only public door. OpenTofu creates the identity, network, image repositories, instance, and settings. The instance runs the demo compose stack with OIDC.

```text
Public HTTPS → Caddy on one EC2 instance
├── Cutaway
├── Guest app
├── Control Tower
├── VenueOps API
├── Environmental Monitor
├── Park Flow Intelligence
└── Reliability Intelligence

PostgreSQL stays in memory on that instance
├── venueops
└── environmental_monitor
```

Environmental Monitor, Park Flow Intelligence, and Reliability Intelligence use Cognito client credentials. The console callback is `https://<host>/auth/callback` and logout is the origin. A reboot clears the live park; startup seeding restores the opening and the October story. Cognito users survive that reboot. The first supervisor is created after apply with the secure admin script. Static site and observability stay empty. This visit has no CloudFront, load balancer, Fargate, or RDS.

## Security posture

- Do not expose PostgreSQL publicly
- Prefer AWS Systems Manager over password SSH
- Accept public traffic only on HTTP/HTTPS; redirect HTTP → HTTPS
- Run containers as non-root
- Store secrets outside images (SSM / Secrets Manager references)
- Never commit AWS credentials or secret values in OpenTofu config
- Treat state as sensitive once a remote backend is enabled

## Database compromise

PostgreSQL stays in memory on the instance. A reboot clears the park, and the
startup seed restores the opening and the October story. There is no RDS and
there are no disk backups.

## Apply workflow (when ready)

Apply the state bootstrap first. The account, region, DNS name, and ACME contact are apply inputs.

```bash
cd infrastructure/bootstrap
tofu init
tofu apply

cd ../environments/demo
tofu init
tofu plan \
  -var='enable_demo_resources=true' \
  -var='demo_host=visit.example.com' \
  -var='demo_acme_email=ops@example.com'
```

`enable_demo_resources` stays false for validation. CI runs `tofu init -backend=false` and `tofu validate`, and does not apply.

## Phase 13 (optional)

Managed evolution (ECS Fargate, ALB, RDS) is deferred until the inexpensive demo
is stable and the cost is justified. Before scaling VenueOps beyond one instance,
replace the in-memory SSE broadcaster and coordinate Environmental Monitor polling.
