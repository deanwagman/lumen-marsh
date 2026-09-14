# Security acceptance matrix (Phase 9)

Run against a stack with security enabled (LOCAL_JWT or Cognito).

| # | Scenario | Expected |
|---|---|---|
| 1 | Guest `GET /api/v1/attractions` | 200 |
| 2 | Guest `GET /api/v1/events` | SSE 200 |
| 3 | Guest attraction command | 401 |
| 4 | Operator changes attraction state | 200 |
| 5 | Operator publishes guest advisory | 403 |
| 6 | Supervisor publishes advisory | 200 |
| 7 | Weather service submits recommendation | 200/201 |
| 8 | Weather service calls operator endpoint | 403 |
| 9 | Forged `X-Actor` header | Ignored; audit uses token identity |
| 10 | Expired / unknown `client_id` / `token_use≠access` | 401 |
| 11 | Expired operator SSE | Stream ends; console prompts login |
| 12 | Activity history | Human / service / system / legacy distinguishable |

## Automated coverage

- VenueOps: `SecurityAuthorizationMatrixTest` + `LocalDevBearerAuthorizationTest` + integration suites with `TestAuth` / `LocalJwtTokenFactory`
- Console: auth route and bearer/SSE client tests
- Monitor: token provider + authenticated sink tests

## Scripted local check

```bash
cd lumen-marsh-platform
./scripts/dev-up.sh
./scripts/smoke-test.sh
./scripts/storm-lifecycle-acceptance.sh
```

For Cognito demo, export real `SUPERVISOR_TOKEN` and `OPERATOR_LIMITED_TOKEN` access tokens before running the lifecycle script. Do not paste tokens into chat.

The older `./scripts/storm-demo.sh` HTTP path remains as a shorter happy-path demo (supervisor-equivalent `OPERATOR_TOKEN`).
