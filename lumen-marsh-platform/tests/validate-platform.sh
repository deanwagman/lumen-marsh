#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

echo "== compose config =="
docker compose -f compose.yaml config >/dev/null
echo "compose.yaml OK"

VENUEOPS_ISSUER_URI=https://example.invalid/user-pool \
VENUEOPS_ALLOWED_CLIENT_IDS=validation-client \
VITE_OIDC_AUTHORITY=https://example.invalid/user-pool \
VITE_OIDC_CLIENT_ID=validation-client \
VITE_API_BASE_URL=http://127.0.0.1:8080 \
OPERATOR_CONSOLE_HOST_PORT=5173 \
OIDC_TOKEN_URL=https://example.invalid/oauth2/token \
OIDC_CLIENT_ID=validation-monitor \
OIDC_CLIENT_SECRET=validation-placeholder \
docker compose -f compose.yaml -f compose.override.yaml -f compose.oidc.yaml config >/dev/null
echo "OIDC compose overlay OK"

echo "== shell syntax =="
for script in scripts/*.sh; do
  bash -n "$script"
done
echo "scripts OK"

echo "== opentofu =="
cd "$ROOT/infrastructure/environments/demo"
tofu fmt -check -recursive ../..
tofu init -backend=false -input=false >/dev/null
tofu validate
echo "opentofu demo OK"

cd "$ROOT/infrastructure/environments/dev-identity"
tofu init -backend=false -input=false >/dev/null
tofu validate
echo "opentofu dev-identity OK"

echo "platform validation passed"
