# Lumen Marsh Control

Operator console for **Lumen Marsh**, a fictional eco-futurist wetlands destination. It is a separate Vite/React/TypeScript application alongside [VenueOps API](../venueops-api) and the [guest companion](../lumen-marsh-app).

The first operator feature is the **Mangrove Run command workspace**: current state, valid commands, wait-time updates, version conflicts, and the activity timeline. Live SSE updates write through the TanStack Query cache rather than a second operational store.

## Requirements

- Node.js 20.19+ or 22.12+ (pinned in `.nvmrc` to 24.18.0)
- VenueOps API running locally for live data (`./gradlew bootRun` in `venueops-api`)

## Local startup

```bash
# Terminal 1 — Spring Boot
cd ../venueops-api
./gradlew bootRun

# Terminal 2 — operator console
cd ../venueops-console
npm install
cp .env.example .env.local # then set your Cognito values
npm run dev
```

The console starts at [http://localhost:5173](http://localhost:5173). `/` redirects to `/dashboard`. Open `/attractions/mangrove-run` for the command workspace.

Production-style static container:

```bash
docker build \
  --build-arg VITE_API_BASE_URL=http://localhost:8080 \
  --build-arg VITE_OIDC_AUTHORITY=https://cognito-idp.us-east-1.amazonaws.com/us-east-1_EXAMPLE \
  --build-arg VITE_OIDC_CLIENT_ID=examplepublicclientid \
  --build-arg VITE_OIDC_REDIRECT_URI=http://localhost:3001/auth/callback \
  --build-arg VITE_OIDC_LOGOUT_URI=http://localhost:3001/login \
  -t venueops-console:local .
docker run --rm -p 3001:8080 venueops-console:local
# health: GET /health
```

`VITE_API_BASE_URL` is baked in at build time until a runtime `config.json` path exists.

During development the Vite server proxies:

```text
/api   → http://localhost:8080
/media → http://localhost:8080
```

The app therefore calls same-origin `/api` and `/media` paths.

## Environment configuration

Copy `.env.example` when you need a non-default origin. Vite only exposes `VITE_*` variables to the browser bundle. **Never put credentials in them.**

| Variable | Development | Production |
| --- | --- | --- |
| `VITE_API_BASE_URL` | Empty (`.env.development`) so requests stay same-origin and use the proxy | Spring Boot origin, e.g. `http://localhost:8080` |
| `VITE_AUTH_MODE` | `oidc` | `oidc` |
| `VITE_OIDC_AUTHORITY` | Cognito issuer/authority | Cognito issuer/authority |
| `VITE_OIDC_CLIENT_ID` | Public app client ID | Public app client ID |
| `VITE_OIDC_REDIRECT_URI` | `http://localhost:5173/auth/callback` | Deployed callback URL |
| `VITE_OIDC_LOGOUT_URI` | `http://localhost:5173/login` | Deployed signed-out URL |
| `VITE_OIDC_SCOPES` | `openid profile email` | Allowed client scopes |

All `import.meta.env` access is centralized in `src/config/environment.ts`. Invalid values fail at startup.

Configure Cognito with an app client that has no secret, enables authorization code grant, and allows the callback/logout URLs above. Tokens are held in memory; only short-lived OAuth transaction state (including the PKCE verifier) uses `sessionStorage`. API and SSE requests send the access token as `Authorization: Bearer …`; tokens are never put in URLs.

Roles are read from `cognito:groups`, `custom:role`, or `role`. A `supervisor` can issue state-changing and weather commands; an `operator` can update wait times. The API remains the authorization boundary.

`VITE_AUTH_MODE=local` enables an explicit development-only session with a mock token. Do not use it in deployed builds. Automated tests inject authenticated or unauthenticated sessions directly.

## Quality checks

```bash
npm run lint
npm run typecheck
npm test
npm run build
```

Watch tests with `npm run test:watch`. Format with `npm run format`.

## Architecture

```text
src/
├── app/                 # shell providers, router, fallback page
├── auth/                # Cognito OIDC session, routes, and role model
├── config/              # validated environment configuration
├── features/attractions # catalog, command workspace, SSE
├── features/dashboard   # park-wide status, attention queue, shift handoff
├── features/docs        # contextual operator procedures
├── features/incidents   # incident center, commands, advisories, activity
├── features/weather     # recommendation inbox, review, incident prefill
├── shared/              # API client, layout, tokens, small UI
└── test/                # Vitest / MSW setup
```

- **React Router** owns console navigation.
- **TanStack Query** owns server data, loading, refresh, and cache updates.
- **Zod** validates Spring Boot payloads before they reach the UI.
- **MSW** stands in for Java in tests.

The operations dashboard summarizes park conditions and links into the Attractions, Incidents, weather, maintenance, and park-flow workspaces. Live operator SSE (`GET /api/v1/operator/events`) writes attraction, incident, weather, maintenance, and flow updates through the Query cache and triggers a debounced dashboard refresh. Guest Flutter streams are not used.

Live events:

```text
SSE operational update
        ↓
queryClient.setQueryData(...)
        ↓
debounced dashboard invalidation
        ↓
list and detail rerender
```

## Routes

| Path | Screen |
| --- | --- |
| `/` | Redirects to `/dashboard` |
| `/dashboard` | Operations dashboard (`GET /api/v1/operator/dashboard`) |
| `/attractions` | Attraction overview (`GET /api/v1/attractions`) |
| `/attractions/:id` | Command workspace (`GET` operator attraction + activity, `POST` commands) |
| `/incidents` | Incident center |
| `/incidents/:incidentId` | Incident detail |
| `/maintenance` | Maintenance workspace |
| `/park-flow` | Park flow overview and recommendation inbox |
| `/park-flow/:attractionId` | Attraction queue and forecast detail |
| `/docs` | Operator procedures and contextual help |
| `/docs/:slug` | Procedure article |
| `/not-found` | Unknown-route fallback |

The shell shows **Lumen Marsh Control**, a left navigation rail, the authenticated operator and role, and an API connection indicator. Narrow viewports collapse the rail into a compact header.
