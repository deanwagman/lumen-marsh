# ADR 0001: Cognito OpenID Connect and JWT resource server

## Status

Accepted.

Current tree: audit identity is the JWT subject (`ActorResolver`); client-supplied `X-Actor` is ignored. Operator SSE uses fetch plus an `Authorization` header. When `enable_machine_client` is true, Cognito also has `reliability-integration` and `park-flow-intelligence` confidential clients. Context below is the original decision record.

## Context

Lumen Marsh will be demonstrated on AWS. The control plane (VenueOps Console, operator APIs, weather integration) must not remain open once exposed publicly. Guests still need anonymous park reads.

Constraints:

- Browser console cannot hold a client secret.
- Environmental Monitor is a machine caller with a narrow write permission.
- Activity history currently trusts the `X-Actor` header, which any client can forge.
- Browser `EventSource` cannot attach Authorization headers; operator SSE needs a different client approach.
- Automated tests must not depend on live Cognito.

## Decision

1. Use **Amazon Cognito** as the OIDC provider for humans and services.
2. Configure VenueOps API as an **OAuth2 resource server** validating Cognito access tokens (JWT). Require `token_use=access` and an allowed `client_id`. Do not require `aud=venueops`; Cognito access tokens identify the app with `client_id` unless resource binding is used.
3. Use **two Cognito app clients**:
   - `venueops-console` — public client, Authorization Code + PKCE, no secret
   - `environmental-monitor` — confidential client, Client Credentials, secret in managed storage
4. Authorize with **scopes** for capabilities and **Cognito groups** (`operators`, `supervisors`) for human elevation.
5. Record audit actors from the **verified token subject**, not from request headers.
6. Keep guest park routes anonymous; deny everything else by default.
7. Use **local test JWTs** for automated API tests; use the demo Cognito pool for manual local login.

## Consequences

### Positive

- Same identity provider in local demo and AWS demo.
- Clear separation between human and machine credentials.
- Narrow Environmental Monitor blast radius (write recommendations only).
- Audit trail becomes trustworthy after `X-Actor` removal.

### Negative / costs

- Cognito and M2M token requests add operational cost and configuration.
- Console must implement PKCE login and a fetch-based SSE client.
- OpenTofu state may contain a machine-client secret if that client is enabled; state must be treated as sensitive. Human user passwords are not stored in state.
- Severity-gated `RESOLVE` needs careful method-security tests.

### Follow-ups

- Phase 1: Cognito infra module
- Phase 2–4: Spring Security + authorization + verified `ActorIdentity`
- Phase 5–6: Console OIDC and Monitor client credentials
- Phase 7–10: Local test mode, hardening, acceptance matrix, public-deploy gate

## Alternatives considered

| Alternative | Why not chosen |
|---|---|
| API keys in headers for everything | Weak human UX; keys shareable; no group elevation |
| Shared Cognito app client for console and monitor | Cognito requires separate clients for PKCE vs client-credentials |
| Cookies + session for the API | Extra CSRF surface; poorer fit for SSE + multi-origin local ports |
| Local-only OIDC for all environments | Diverges from AWS demo; higher long-term drift |
| Keep `X-Actor` alongside JWT | Continues to allow spoofed audit identities |

## References

- [AWS Cognito PKCE](https://docs.aws.amazon.com/cognito/latest/developerguide/using-pkce-in-authorization-code.html)
- [AWS Cognito app clients](https://docs.aws.amazon.com/cognito/latest/developerguide/user-pool-settings-client-apps.html)
- [Spring Boot OAuth2 resource server](https://docs.spring.io/spring-boot/reference/security/oauth2.html)
- [security-contract.md](../security-contract.md)
