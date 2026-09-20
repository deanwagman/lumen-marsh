#!/usr/bin/env bash
set -euo pipefail
# Print non-secret Cognito values for the native console and VenueOps API.
# Reads the applied development identity environment (not the full demo stack).

# shellcheck source=./_lib.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_lib.sh"

require_cmd tofu

IDENTITY_DIR="$PLATFORM_ROOT/infrastructure/environments/dev-identity"
cd "$IDENTITY_DIR"

if ! tofu output -raw CONSOLE_CLIENT_ID >/dev/null 2>&1; then
  echo "error: apply infrastructure/environments/dev-identity first (./scripts/provision-dev-cognito.sh)" >&2
  exit 1
fi

issuer="$(tofu output -raw COGNITO_ISSUER_URI)"
domain="$(tofu output -raw COGNITO_DOMAIN)"
pool="$(tofu output -raw COGNITO_USER_POOL_ID)"
client="$(tofu output -raw CONSOLE_CLIENT_ID)"
region="$(tofu output -raw aws_region)"

if [[ -z "$issuer" || "$issuer" == "null" || -z "$client" || "$client" == "null" ]]; then
  echo "error: Cognito outputs are empty — apply infrastructure/environments/dev-identity first" >&2
  exit 1
fi

cat <<EOF
# Console (venueops-console/.env.local) — public values only
VITE_AUTH_MODE=oidc
VITE_OIDC_AUTHORITY=${issuer}
VITE_OIDC_CLIENT_ID=${client}
VITE_OIDC_REDIRECT_URI=http://127.0.0.1:5173/auth/callback
VITE_OIDC_LOGOUT_URI=http://127.0.0.1:5173/
VITE_OIDC_SCOPES=openid profile email venueops/operator.read venueops/attractions.command venueops/incidents.command venueops/advisories.publish venueops/weather-recommendations.review venueops/maintenance.read venueops/maintenance.command venueops/maintenance.inspect venueops/flow.read venueops/flow.command venueops/flow.publish

# VenueOps API (OIDC mode; do not mix with LOCAL_JWT)
VENUEOPS_SECURITY_MODE=OIDC
VENUEOPS_ISSUER_URI=${issuer}
VENUEOPS_ALLOWED_CLIENT_IDS=${client}

# Reference
COGNITO_DOMAIN=${domain}
COGNITO_USER_POOL_ID=${pool}
AWS_REGION=${region}
# Add the Environmental Monitor and Park Flow Intelligence client_ids to VENUEOPS_ALLOWED_CLIENT_IDS when those paths are in scope.
EOF
