# Security hardening checklist (Phase 9)

- [x] CORS restricted via `venueops.security.allowed-origins` (default localhost patterns; demo/prod must set exact console origins)
- [x] Preflight allowed through Spring Security CORS
- [x] JWT validation: issuer, `token_use=access`, allowed `client_id`, signature, expiration (OIDC or LOCAL_JWT keys)
- [x] Access-token lifetime: 60 minutes (Cognito console and machine clients)
- [x] CSRF explicitly disabled for bearer/stateless API (documented in `SecurityConfiguration`)
- [x] Actuator exposure limited to `health`
- [x] SpringDoc/Swagger denied unless `venueops.security.expose-api-docs=true`
- [x] Auth failures return Problem Details JSON without token contents
- [x] Structured `venueops.security` logs for 401 / 403 / 429 (exception class only; no token or header values)
- [x] Logback converters scrub Bearer tokens, client secrets, and compact JWTs from messages and stack traces
- [x] Weather ingest rate-limited in VenueOps (`VENUEOPS_WEATHER_INGEST_RATE_LIMIT_PER_MINUTE`, default 120)
- [x] Console nginx sets CSP, `frame-ancestors 'none'`, `X-Frame-Options: DENY`
- [x] Secret rotation documented in [secret-rotation.md](./secret-rotation.md)
- [x] Filesystem/secret scan in platform CI; local image scan via `./scripts/security-scan.sh`
- [ ] Rate-limit the Cognito token endpoint at the edge (WAF / AWS managed rules) before public demo
- [ ] Tighten console `connect-src` to the deployed API origin (replace localhost wildcards) before public demo

## CSRF decision

VenueOps API authentication is bearer-token based with stateless sessions. Cookie session CSRF does not apply; CSRF protection remains disabled and this decision is intentional.

## Application rate limit

`POST /api/v1/integrations/weather/recommendations` is limited per client address (first `X-Forwarded-For` hop, else remote address). Exceeded requests return `429` with `Retry-After: 60`. Set the limit to `0` only in tightly scoped automated tests. Behind a load balancer, enable Spring forwarded headers (`server.forward-headers-strategy=framework`) so the limiter is not shared across all clients.

Cognito's `/oauth2/token` endpoint is not owned by VenueOps; protect it with WAF when the user pool is public.
