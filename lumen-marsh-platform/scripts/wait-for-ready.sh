#!/usr/bin/env bash
set -euo pipefail
# shellcheck source=./_lib.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_lib.sh"

require_cmd docker
require_cmd curl
load_env

AUTH_MODE=local
if [[ "${1:-}" == "--oidc" ]]; then
  AUTH_MODE=oidc
elif [[ $# -gt 0 ]]; then
  echo "usage: ./scripts/wait-for-ready.sh [--oidc]" >&2
  exit 1
fi

VENUEOPS_URL="${VENUEOPS_PUBLIC_ORIGIN:-http://localhost:8080}"
MONITOR_URL="${ENVIRONMENTAL_MONITOR_PUBLIC_ORIGIN:-http://localhost:8000}"
CONSOLE_URL="${OPERATOR_CONSOLE_ORIGIN:-http://localhost:3001}"
GUEST_URL="${GUEST_APP_ORIGIN:-http://localhost:3000}"

if [[ "$AUTH_MODE" == "oidc" ]]; then
  CONSOLE_URL=http://127.0.0.1:5173
fi

wait_http "$VENUEOPS_URL/actuator/health" "venueops-api"
wait_http "$MONITOR_URL/health/ready" "environmental-monitor"
wait_http "$CONSOLE_URL/health" "venueops-console"
wait_http "$GUEST_URL/health" "lumen-marsh-app"

echo "all services ready"
