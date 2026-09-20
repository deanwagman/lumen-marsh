#!/usr/bin/env bash
# Shared helpers for platform scripts. Sourced by other scripts; not executed alone.
set -euo pipefail

PLATFORM_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PLATFORM_ROOT"

COMPOSE=(docker compose)
EXTRA_COMPOSE_FILES=()

require_cmd() {
  local cmd="$1"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "error: required command not found: $cmd" >&2
    exit 1
  fi
}

load_env() {
  if [[ ! -f .env ]]; then
    echo "error: missing .env — copy .env.example and set NWS_USER_AGENT" >&2
    exit 1
  fi
  # Parse KEY=VALUE without bash-sourcing (values may contain parentheses).
  eval "$(
    python3 - <<'PY'
from pathlib import Path
import shlex
for raw in Path(".env").read_text().splitlines():
    line = raw.strip()
    if not line or line.startswith("#") or "=" not in line:
        continue
    key, value = line.split("=", 1)
    key = key.strip()
    value = value.strip()
    if value[:1] == value[-1:] and value[:1] in {"'", '"'}:
        value = value[1:-1]
    if key:
        print(f"export {key}={shlex.quote(value)}")
PY
  )"
  if [[ -z "${NWS_USER_AGENT:-}" || "${NWS_USER_AGENT}" == *"replace-with-contact"* || "${NWS_USER_AGENT}" == *"you@example.com"* ]]; then
    echo "error: set NWS_USER_AGENT in .env to an identifier that includes your contact information" >&2
    exit 1
  fi
}

compose() {
  local files=(-f compose.yaml)
  if [[ -f compose.override.yaml ]]; then
    files+=(-f compose.override.yaml)
  fi
  local extra
  for extra in "${EXTRA_COMPOSE_FILES[@]:-}"; do
    [[ -n "$extra" ]] && files+=(-f "$extra")
  done
  "${COMPOSE[@]}" "${files[@]}" "$@"
}

json_field() {
  local field="$1"
  python3 -c 'import json,sys; data=json.load(sys.stdin); print(data'"$field"')'
}

wait_http() {
  local url="$1"
  local label="${2:-$url}"
  local attempts="${3:-60}"
  local i
  for ((i = 1; i <= attempts; i++)); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      echo "ready: $label"
      return 0
    fi
    sleep 2
  done
  echo "error: timed out waiting for $label ($url)" >&2
  return 1
}

merge_env_file() {
  local file="$1"
  shift
  python3 - "$file" "$@" <<'PY'
from pathlib import Path
import sys

path = Path(sys.argv[1])
existing = {}
if path.exists():
    for raw in path.read_text().splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        existing[key.strip()] = value
for item in sys.argv[2:]:
    key, value = item.split("=", 1)
    existing[key] = value
path.parent.mkdir(parents=True, exist_ok=True)
path.write_text("".join(f"{key}={existing[key]}\n" for key in existing) + "")
PY
}

print_urls() {
  local auth_mode="${1:-local}"
  if [[ "$auth_mode" == "oidc" ]]; then
    cat <<EOF

Lumen Marsh is up (Cognito OIDC):

  Guest app               ${GUEST_APP_ORIGIN:-http://localhost:3000}
  Operator console        ${OPERATOR_CONSOLE_ORIGIN:-http://127.0.0.1:5173}
  VenueOps API            ${VENUEOPS_PUBLIC_ORIGIN:-http://localhost:8080}
  Environmental Monitor   ${ENVIRONMENTAL_MONITOR_PUBLIC_ORIGIN:-http://localhost:8000}
  Park Flow Intelligence  ${PARK_FLOW_INTELLIGENCE_PUBLIC_ORIGIN:-http://localhost:8100}

  Health:
    curl ${VENUEOPS_PUBLIC_ORIGIN:-http://localhost:8080}/actuator/health
    curl ${ENVIRONMENTAL_MONITOR_PUBLIC_ORIGIN:-http://localhost:8000}/health/ready
    curl ${PARK_FLOW_INTELLIGENCE_PUBLIC_ORIGIN:-http://localhost:8100}/health/ready
    curl ${OPERATOR_CONSOLE_ORIGIN:-http://127.0.0.1:5173}/health

EOF
    return
  fi

  cat <<EOF

Lumen Marsh is up:

  Guest app               ${GUEST_APP_ORIGIN:-http://localhost:3000}
  Operator console        ${OPERATOR_CONSOLE_ORIGIN:-http://localhost:3001}
  VenueOps API            ${VENUEOPS_PUBLIC_ORIGIN:-http://localhost:8080}
  Environmental Monitor   ${ENVIRONMENTAL_MONITOR_PUBLIC_ORIGIN:-http://localhost:8000}
  Park Flow Intelligence  ${PARK_FLOW_INTELLIGENCE_PUBLIC_ORIGIN:-http://localhost:8100}

  Health:
    curl ${VENUEOPS_PUBLIC_ORIGIN:-http://localhost:8080}/actuator/health
    curl ${ENVIRONMENTAL_MONITOR_PUBLIC_ORIGIN:-http://localhost:8000}/health/ready
    curl ${PARK_FLOW_INTELLIGENCE_PUBLIC_ORIGIN:-http://localhost:8100}/health/ready

EOF
}
