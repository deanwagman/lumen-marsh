# Local development and automated testing

See [rollout.md](./rollout.md) for the Cognito apply and login sequence.

## Manual local development

### Option A — Cognito on port 5173 (login UX)

The one-command Compose path reads public issuer/client values from the applied OpenTofu state:

```bash
./scripts/dev-up.sh --oidc
```

This starts the full stack: VenueOps validates console, monitor, park-flow, and reliability ingest client IDs, the operator console runs on `http://127.0.0.1:5173`, and Environmental Monitor, Park Flow Intelligence, and Reliability Intelligence exchange client credentials for cached access tokens. The launcher reads machine secrets from AWS Secrets Manager with `AWS_PROFILE` (default: `lumen-marsh`) and does not write secrets to `.env`.

For a faster native-code loop instead:

1. Apply identity: `./scripts/provision-dev-cognito.sh` (applies `infrastructure/environments/dev-identity`, then creates the operator).
2. `./scripts/print-cognito-local-env.sh` and copy public values into `venueops-console/.env.local` and the API process env.
3. Run VenueOps with `VENUEOPS_SECURITY_MODE=OIDC`, the printed issuer, and `VENUEOPS_ALLOWED_CLIENT_IDS` set to the console client ID.
4. `cd ../venueops-console && npm run dev` — Vite is pinned to **5173**.
5. Sign in. Development builds stop on `/auth/session` so you can confirm issuer, `client_id`, `token_use`, `sub`, groups, and scopes. Cognito access tokens often omit `aud`. The access token is not shown.
6. Continue and issue `POST /api/v1/operator/attractions/{id}/commands`.

The generated local configuration uses the explicit IPv4 loopback address to avoid browser-specific `localhost` IPv6 resolution. Callback: `http://127.0.0.1:5173/auth/callback`. Logout: `http://127.0.0.1:5173/`. The equivalent `localhost` URLs remain registered with Cognito.

`VITE_COGNITO_CLIENT_ID` / `VITE_COGNITO_REDIRECT_URI` / `VITE_COGNITO_LOGOUT_URI` are accepted as aliases. The OIDC **authority** is still the issuer (`VITE_OIDC_AUTHORITY`), not the hosted-UI domain.

OpenTofu does not create users or store passwords. `create-dev-operator.sh` and `create-dev-supervisor.sh` write credentials to gitignored `.env.cognito.local`.

### Option B — LOCAL_JWT Compose bridge (offline stack)

`compose.override.yaml` enables:

| Component | Setting |
|---|---|
| VenueOps API | `VENUEOPS_SECURITY_MODE=LOCAL_JWT` |
| Console image build | `VITE_AUTH_MODE=local` → bearer `local-development-token` |
| Environmental Monitor | `OIDC_DISABLED=true` + `VENUEOPS_BEARER_TOKEN=local-weather-token` |
| Park Flow Intelligence | `OIDC_DISABLED=true` + `VENUEOPS_BEARER_TOKEN=local-flow-token` |
| Reliability Intelligence | `OIDC_DISABLED=true` + `VENUEOPS_BEARER_TOKEN=local-reliability-token` |

These opaque tokens are accepted **only** when the API runs in `LOCAL_JWT` mode. They are not Cognito tokens and must never be used with `OIDC` mode. Reliability Intelligence and the maintenance scripts use `local-reliability-token` for ingest.

Smoke/storm/maintenance/flow scripts default to `OPERATOR_TOKEN=local-development-token` (supervisor-equivalent). `storm-lifecycle-acceptance.sh`, `maintenance-lifecycle-acceptance.sh`, and `flow-lifecycle-acceptance.sh` also use `OPERATOR_LIMITED_TOKEN=local-operator-token` for operator-only 403 checks. The maintenance script uses `RELIABILITY_TOKEN=local-reliability-token` for ingest. The flow script uses `FLOW_TOKEN=local-flow-token` for machine-identity checks.

## Automated testing

- VenueOps: `./gradlew test` mints JWT authorities via Spring Security test support / an ephemeral local RSA key. **No live Cognito calls.**
- Console: Vitest injects authenticated and unauthenticated sessions.
- Monitor: pytest uses mocked token providers; OIDC may be omitted in `APP_ENV=test`.

There is no silent “security disabled” production fallback. Missing issuer or allowed-client-id configuration fails startup validation.
