# Contributing

## Development principles

- Keep operational commands explicit, authorized, versioned, and auditable.
- Keep internal incident data out of guest-facing projections.
- Treat weather output as a recommendation requiring human review.
- Preserve the fictional-project and non-life-safety disclaimers.
- Never place credentials in source, examples, tests, URLs, screenshots, or logs.

## Local validation

Run the checks for the component you changed.

### VenueOps API

```bash
cd venueops-api
./gradlew test
```

### Environmental Monitor

```bash
cd environmental-monitor
uv sync --frozen
uv run ruff check .
uv run ruff format --check .
uv run mypy src
uv run pytest
```

### Operator console

```bash
cd venueops-console
npm ci
npm run lint
npm run typecheck
npm test
npm run build
```

### Guest app

```bash
cd lumen-marsh-app
flutter pub get
flutter analyze
flutter test
flutter build web --release
```

### Platform

```bash
cd lumen-marsh-platform
docker compose -f compose.yaml config >/dev/null
tofu fmt -check -recursive infrastructure
cd infrastructure/environments/demo
tofu init -backend=false
tofu validate
```

For a full local integration check, start the stack and run `lumen-marsh-platform/scripts/storm-lifecycle-acceptance.sh`.

## Pull requests

Keep changes focused and explain the operator or guest behavior they affect. Include tests for state transitions, authorization boundaries, mappings, and failure responses. Update the walkthrough or screenshots when a visible demo flow changes.
