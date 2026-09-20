# Public deployment gate (Phase 10)

**The AWS public demo remains blocked until every item below is complete.**

- [ ] Run [storm-lifecycle-demo.md](../storm-lifecycle-demo.md) and `./scripts/storm-lifecycle-acceptance.sh` against a clean local seed
- [ ] Run [maintenance-lifecycle-demo.md](../maintenance-lifecycle-demo.md) and `./scripts/maintenance-lifecycle-acceptance.sh` against a clean local seed
- [ ] Apply Cognito identity OpenTofu module (`enable_demo_resources=true`)
- [ ] Create initial supervisor through a secure admin process (no passwords in Git)
- [ ] Store Environmental Monitor and Park Flow Intelligence client secrets in Secrets Manager / SSM
- [ ] Configure API `VENUEOPS_SECURITY_MODE=OIDC`, issuer, `VENUEOPS_ALLOWED_CLIENT_IDS` (console, monitor, park-flow, and reliability ingest clients)
- [ ] Configure console Cognito client id, domain/authority, exact callback + logout URLs
- [ ] Deploy API, console, monitor, and park-flow with non-LOCAL_JWT credentials
- [ ] Run [acceptance-matrix.md](./acceptance-matrix.md) against the deployed environment
- [ ] Verify CORS from the real console origin only
- [ ] Verify authenticated operator SSE through the deployed proxy (Authorization header, not query)
- [ ] Confirm unclassified routes return 401/403
- [ ] Rotate any temporary bootstrap credentials
- [ ] Confirm no tokens/secrets in URLs, logs, Git, or images
- [ ] Run `./scripts/security-scan.sh` against built demo images

Sign-off: ______________________ Date: __________
