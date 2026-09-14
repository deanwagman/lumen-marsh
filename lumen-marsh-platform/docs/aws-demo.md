# AWS demo environment (Phases 10–12) — design only until enable_demo_resources=true

## Target architecture (inexpensive single-instance)

```text
S3 + CloudFront
├── Flutter guest app
└── React operator console

Public HTTPS → reverse proxy on EC2
├── VenueOps API
├── Environmental Monitor
└── PostgreSQL (self-managed)
    ├── venueops
    └── environmental_monitor
```

## Security posture

- Do not expose PostgreSQL publicly
- Prefer AWS Systems Manager over password SSH
- Accept public traffic only on HTTP/HTTPS; redirect HTTP → HTTPS
- Run containers as non-root
- Store secrets outside images (SSM / Secrets Manager references)
- Never commit AWS credentials or secret values in OpenTofu config
- Treat state as sensitive once a remote backend is enabled

## Database compromise

Self-managed PostgreSQL on EC2 is a cost choice. It lacks managed HA, failover,
patching, and PITR. Compensate with encrypted EBS, scheduled logical backups to
private S3, restore runbooks, and disk-usage monitoring.

## Apply workflow (when ready)

```bash
cd infrastructure/environments/demo
tofu init
tofu plan -var='enable_demo_resources=true'
# Review the plan, then:
# tofu apply -var='enable_demo_resources=true'
```

Phase 9 keeps `enable_demo_resources=false` so `tofu validate` creates no resources.

## Phase 13 (optional)

Managed evolution (ECS Fargate, ALB, RDS) is deferred until the inexpensive demo
is stable and the cost is justified. Before scaling VenueOps beyond one instance,
replace the in-memory SSE broadcaster and coordinate Environmental Monitor polling.
