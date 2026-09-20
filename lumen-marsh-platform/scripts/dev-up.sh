#!/usr/bin/env bash
set -euo pipefail
# shellcheck source=./_lib.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_lib.sh"

require_cmd docker
require_cmd curl
require_cmd python3
load_env

DETACH=1
BUILD=1
AUTH_MODE=local
SERVICES=()

usage() {
  cat <<'EOF'
Usage: ./scripts/dev-up.sh [--oidc] [--debug] [--foreground] [--no-build]

Starts the integrated Lumen Marsh stack with Docker Compose.

  --oidc       Use the applied development Cognito user pool. Starts VenueOps,
               Environmental Monitor, Park Flow Intelligence, the operator console, and guest app.
               Reads the monitor client secret from AWS Secrets Manager.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --debug)
      EXTRA_COMPOSE_FILES+=(compose.debug.yaml)
      shift
      ;;
    --oidc)
      AUTH_MODE=oidc
      shift
      ;;
    --foreground)
      DETACH=0
      shift
      ;;
    --no-build)
      BUILD=0
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "error: unknown argument: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

if [[ "$AUTH_MODE" == "oidc" ]]; then
  require_cmd tofu
  require_cmd aws
  identity_dir="$PLATFORM_ROOT/infrastructure/environments/dev-identity"

  if ! tofu -chdir="$identity_dir" output -raw CONSOLE_CLIENT_ID >/dev/null 2>&1 || \
     ! tofu -chdir="$identity_dir" output -raw MONITOR_SECRET_ARN >/dev/null 2>&1 || \
     ! tofu -chdir="$identity_dir" output -raw FLOW_SECRET_ARN >/dev/null 2>&1; then
    echo "error: apply the development Cognito console and machine-client environment first" >&2
    echo "hint: cd infrastructure/environments/dev-identity && AWS_PROFILE=lumen-marsh tofu apply" >&2
    exit 1
  fi

  issuer="$(tofu -chdir="$identity_dir" output -raw COGNITO_ISSUER_URI)"
  client="$(tofu -chdir="$identity_dir" output -raw CONSOLE_CLIENT_ID)"
  monitor_secret_arn="$(tofu -chdir="$identity_dir" output -raw MONITOR_SECRET_ARN)"
  flow_secret_arn="$(tofu -chdir="$identity_dir" output -raw FLOW_SECRET_ARN)"
  region="$(tofu -chdir="$identity_dir" output -raw aws_region)"
  if [[ -z "$issuer" || "$issuer" == "null" || -z "$client" || "$client" == "null" || -z "$monitor_secret_arn" || "$monitor_secret_arn" == "null" || -z "$flow_secret_arn" || "$flow_secret_arn" == "null" ]]; then
    echo "error: development Cognito outputs are incomplete" >&2
    exit 1
  fi

  aws_profile="${AWS_PROFILE:-lumen-marsh}"
  if ! monitor_secret_json="$(aws secretsmanager get-secret-value \
    --secret-id "$monitor_secret_arn" \
    --region "$region" \
    --profile "$aws_profile" \
    --query SecretString \
    --output text)"; then
    echo "error: unable to read the Environmental Monitor OIDC secret with AWS profile '$aws_profile'" >&2
    exit 1
  fi

  monitor_client="$(printf '%s' "$monitor_secret_json" | json_field "['client_id']")"
  monitor_client_secret="$(printf '%s' "$monitor_secret_json" | json_field "['client_secret']")"
  monitor_token_url="$(printf '%s' "$monitor_secret_json" | json_field "['token_url']")"
  monitor_scope="$(printf '%s' "$monitor_secret_json" | json_field "['scope']")"
  unset monitor_secret_json
  if [[ -z "$monitor_client" || -z "$monitor_client_secret" || -z "$monitor_token_url" || -z "$monitor_scope" ]]; then
    echo "error: Environmental Monitor OIDC secret is incomplete" >&2
    exit 1
  fi

  if ! flow_secret_json="$(aws secretsmanager get-secret-value \
    --secret-id "$flow_secret_arn" \
    --region "$region" \
    --profile "$aws_profile" \
    --query SecretString \
    --output text)"; then
    echo "error: unable to read the Park Flow Intelligence OIDC secret with AWS profile '$aws_profile'" >&2
    exit 1
  fi

  flow_client="$(printf '%s' "$flow_secret_json" | json_field "['client_id']")"
  flow_client_secret="$(printf '%s' "$flow_secret_json" | json_field "['client_secret']")"
  flow_token_url="$(printf '%s' "$flow_secret_json" | json_field "['token_url']")"
  flow_scope="$(printf '%s' "$flow_secret_json" | json_field "['scope']")"
  unset flow_secret_json
  if [[ -z "$flow_client" || -z "$flow_client_secret" || -z "$flow_token_url" || -z "$flow_scope" ]]; then
    echo "error: Park Flow Intelligence OIDC secret is incomplete" >&2
    exit 1
  fi

  export VENUEOPS_SECURITY_MODE=OIDC
  export VENUEOPS_ISSUER_URI="$issuer"
  export VENUEOPS_ALLOWED_CLIENT_IDS="$client,$monitor_client,$flow_client"
  export VITE_AUTH_MODE=oidc
  export VITE_OIDC_AUTHORITY="$issuer"
  export VITE_OIDC_CLIENT_ID="$client"
  export VITE_OIDC_REDIRECT_URI=http://127.0.0.1:5173/auth/callback
  export VITE_OIDC_LOGOUT_URI=http://127.0.0.1:5173/
  export VITE_OIDC_SCOPES="openid profile email venueops/operator.read venueops/attractions.command venueops/incidents.command venueops/advisories.publish venueops/weather-recommendations.review venueops/maintenance.read venueops/maintenance.command venueops/maintenance.inspect venueops/flow.read venueops/flow.command venueops/flow.publish"
  export VITE_API_BASE_URL=http://127.0.0.1:8080
  export OPERATOR_CONSOLE_HOST_PORT=5173
  export OPERATOR_CONSOLE_ORIGIN=http://127.0.0.1:5173
  export OIDC_DISABLED=false
  export OIDC_TOKEN_URL="$monitor_token_url"
  export OIDC_CLIENT_ID="$monitor_client"
  export OIDC_CLIENT_SECRET="$monitor_client_secret"
  export OIDC_SCOPE="$monitor_scope"
  export VENUEOPS_BEARER_TOKEN=
  export FLOW_OIDC_DISABLED=false
  export FLOW_OIDC_TOKEN_URL="$flow_token_url"
  export FLOW_OIDC_CLIENT_ID="$flow_client"
  export FLOW_OIDC_CLIENT_SECRET="$flow_client_secret"
  export FLOW_OIDC_SCOPE="$flow_scope"
  export FLOW_VENUEOPS_BEARER_TOKEN=

  EXTRA_COMPOSE_FILES+=(compose.oidc.yaml)
  SERVICES=(venueops-api environmental-monitor park-flow-intelligence venueops-console lumen-marsh-app)
fi

args=(up)
if [[ "$BUILD" -eq 1 ]]; then
  args+=(--build)
fi
if [[ "$DETACH" -eq 1 ]]; then
  args+=(-d)
fi
if [[ "${#SERVICES[@]}" -gt 0 ]]; then
  args+=("${SERVICES[@]}")
fi

echo "Starting Lumen Marsh platform from $PLATFORM_ROOT (auth: $AUTH_MODE)"
compose "${args[@]}"
if [[ "$AUTH_MODE" == "oidc" ]]; then
  ./scripts/wait-for-ready.sh --oidc
else
  ./scripts/wait-for-ready.sh
fi
print_urls "$AUTH_MODE"
