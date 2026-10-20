#!/usr/bin/env bash
set -euo pipefail

# Proves the disposable hosted-demo boundary: one TLS gateway, no direct host
# ports, bounded/restarting services, ephemeral databases, and working
# same-origin Cutaway surfaces and producer triggers.

PLATFORM_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PLATFORM_ROOT"

COMPOSE=(docker compose -f compose.yaml -f compose.demo.yaml)
MANAGE_STACK=0
CONFIG_ONLY=0

usage() {
  cat <<'EOF'
Usage: ./scripts/cutaway-sandbox-acceptance.sh [--manage-stack] [--config-only]

  --manage-stack  Replace the local stack with the demo overlay before testing.
  --config-only   Validate rendered packaging without requiring running services.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --manage-stack) MANAGE_STACK=1 ;;
    --config-only) CONFIG_ONLY=1 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "error: unknown argument: $1" >&2; usage >&2; exit 1 ;;
  esac
  shift
done

for command in docker curl python3; do
  command -v "$command" >/dev/null 2>&1 || {
    echo "error: required command not found: $command" >&2
    exit 1
  }
done

: "${DEMO_HOST:?set DEMO_HOST}"
: "${DEMO_ORIGIN:?set DEMO_ORIGIN}"
: "${NWS_USER_AGENT:?set NWS_USER_AGENT}"
: "${DEMO_SECURITY_MODE:?set DEMO_SECURITY_MODE}"
: "${DEMO_RESET_SECRET:?set DEMO_RESET_SECRET}"
: "${DEMO_VENUEOPS_DB_PASSWORD:?set DEMO_VENUEOPS_DB_PASSWORD}"
: "${DEMO_ENVIRONMENTAL_DB_PASSWORD:?set DEMO_ENVIRONMENTAL_DB_PASSWORD}"
: "${DEMO_WEATHER_TOKEN:?set DEMO_WEATHER_TOKEN}"
: "${DEMO_FLOW_TOKEN:?set DEMO_FLOW_TOKEN}"
: "${DEMO_RELIABILITY_TOKEN:?set DEMO_RELIABILITY_TOKEN}"

if [[ "${#DEMO_RESET_SECRET}" -lt 32 ]]; then
  echo "error: DEMO_RESET_SECRET must contain at least 32 characters" >&2
  exit 1
fi
if [[ "$NWS_USER_AGENT" != *"@"* ]]; then
  echo "error: NWS_USER_AGENT must identify the application and include a contact address" >&2
  exit 1
fi

CONFIG_JSON="$(mktemp "${TMPDIR:-/tmp}/lm-demo-compose.XXXXXX.json")"
trap 'rm -f "$CONFIG_JSON"' EXIT
"${COMPOSE[@]}" config --format json >"$CONFIG_JSON"

python3 - "$CONFIG_JSON" <<'PY'
import json
import sys

config = json.load(open(sys.argv[1]))
services = config["services"]
one_shot = {"environmental-migrate"}

assert set(services) >= {
    "gateway", "cutaway", "venueops-api", "venueops-db", "environmental-db",
    "environmental-monitor", "park-flow-intelligence",
    "reliability-intelligence", "venueops-console", "lumen-marsh-app",
}

published = {
    name: service.get("ports", [])
    for name, service in services.items()
    if service.get("ports")
}
assert set(published) == {"gateway"}, f"only gateway may publish ports: {published}"

for name, service in services.items():
    if name not in one_shot:
        assert service.get("restart") == "unless-stopped", (name, service.get("restart"))
        assert service.get("healthcheck"), f"{name} has no healthcheck"
    assert service.get("cpus"), f"{name} has no CPU limit"
    assert service.get("mem_limit"), f"{name} has no memory limit"

for name in ("venueops-db", "environmental-db"):
    service = services[name]
    assert not service.get("volumes"), f"{name} must not use persistent data volumes"
    assert any("/var/lib/postgresql/data" in item for item in service.get("tmpfs", []))
    labels = service.get("labels", {})
    assert labels.get("io.lumen-marsh.demo.disposable") == "true"
    assert labels.get("io.lumen-marsh.demo.backup") == "disabled"

for name in ("environmental-monitor", "park-flow-intelligence", "reliability-intelligence"):
    env = services[name]["environment"]
    assert env["APP_ENV"] == "development"
    assert env["SIMULATION_ENABLED"] == "true"
    assert env["DEMO_SANDBOX_ENABLED"] == "true"
    assert "demo-gateway" not in services[name].get("networks", {})

assert services["venueops-api"]["environment"]["VENUEOPS_DEMO_SANDBOX_ENABLED"] == "true"
assert "@" in services["environmental-monitor"]["environment"]["NWS_USER_AGENT"]
assert services["gateway"]["environment"]["DEMO_ORIGIN"] == config["services"]["cutaway"]["environment"]["DEMO_ORIGIN"]
assert config["networks"]["demo-gateway"]["internal"] is True
print("PASS  rendered demo boundary")
PY

if [[ "$CONFIG_ONLY" -eq 1 ]]; then
  echo "Cutaway sandbox acceptance: config passed"
  exit 0
fi

if [[ "$MANAGE_STACK" -eq 1 ]]; then
  docker compose -f compose.yaml -f compose.override.yaml down --remove-orphans
  "${COMPOSE[@]}" up --build -d
fi

origin_parts="$(
  python3 - "$DEMO_ORIGIN" <<'PY'
from urllib.parse import urlsplit
import sys
url = urlsplit(sys.argv[1])
assert url.scheme == "https", "DEMO_ORIGIN must use https"
assert url.hostname, "DEMO_ORIGIN must include a host"
print(url.hostname, url.port or 443)
PY
)"
read -r origin_host origin_port <<<"$origin_parts"
resolve=(--resolve "${origin_host}:${origin_port}:127.0.0.1")
curl_tls=(-kfsS "${resolve[@]}")

for _ in $(seq 1 90); do
  if [[ "$(curl -ksS "${resolve[@]}" -o /dev/null -w '%{http_code}' \
    "$DEMO_ORIGIN/gateway-health" 2>/dev/null)" == "200" ]]; then
    break
  fi
  sleep 2
done
gateway_health="$(curl -ksS "${resolve[@]}" -w $'\n%{http_code}' \
  "$DEMO_ORIGIN/gateway-health")"
[[ "$gateway_health" == $'ok\n200' ]]
echo "PASS  TLS gateway health"

curl "${curl_tls[@]}" "$DEMO_ORIGIN/" | grep -qi 'cutaway'
curl "${curl_tls[@]}" "$DEMO_ORIGIN/guest/today" >/dev/null
curl "${curl_tls[@]}" "$DEMO_ORIGIN/dashboard" >/dev/null
curl "${curl_tls[@]}" "$DEMO_ORIGIN/api/v1/attractions" \
  | python3 -c 'import json,sys; assert len(json.load(sys.stdin)) > 0'
echo "PASS  same-origin exhibit, apps, and API"

cookie_a="$(mktemp "${TMPDIR:-/tmp}/lm-demo-cookie-a.XXXXXX")"
cookie_b="$(mktemp "${TMPDIR:-/tmp}/lm-demo-cookie-b.XXXXXX")"
trap 'rm -f "$CONFIG_JSON" "$cookie_a" "$cookie_b"' EXIT

curl "${curl_tls[@]}" "$DEMO_ORIGIN/api/v1/demo/readiness" \
  | python3 -c 'import json,sys; assert json.load(sys.stdin)["ready"] is True'

quiet_dashboard() {
  curl "${curl_tls[@]}" -b "$1" "$DEMO_ORIGIN/api/v1/operator/dashboard" |
    python3 -c '
import json, sys
d = json.load(sys.stdin)
for key in ("pendingWeatherRecommendations", "attractionsNeedingAttention",
            "publishedGuestAdvisories", "openIncidents", "needsAttention"):
    assert not (d.get(key) or []), (key, d.get(key))
assert not d.get("activeWeatherWorkflow"), d.get("activeWeatherWorkflow")
'
}

non_quiet_dashboard() {
  local cookie="$1"
  for _ in $(seq 1 30); do
    if curl "${curl_tls[@]}" -b "$cookie" "$DEMO_ORIGIN/api/v1/operator/dashboard" |
      python3 -c '
import json, sys
d = json.load(sys.stdin)
keys = ("pendingWeatherRecommendations", "attractionsNeedingAttention",
        "publishedGuestAdvisories", "openIncidents", "needsAttention")
assert any(d.get(key) for key in keys) or d.get("activeWeatherWorkflow")
'; then
      return 0
    fi
    sleep 1
  done
  return 1
}

pending_subject() {
  local cookie="$1"
  local kind="$2"
  for _ in $(seq 1 30); do
    local subject
    subject="$(
      curl "${curl_tls[@]}" -b "$cookie" "$DEMO_ORIGIN/api/v1/operator/dashboard" |
        KIND="$kind" python3 -c '
import json, os, sys
items = json.load(sys.stdin).get("needsAttention") or []
print(next((item.get("subjectId", "") for item in items if item.get("kind") == os.environ["KIND"]), ""))
'
    )"
    if [[ -n "$subject" ]]; then
      printf '%s\n' "$subject"
      return 0
    fi
    sleep 1
  done
  return 1
}

reset_and_prove_quiet() {
  curl "${curl_tls[@]}" -b "$1" -X POST "$DEMO_ORIGIN/api/v1/demo/reset" \
    | python3 -c 'import json,sys; result=json.load(sys.stdin)["reset"]; assert set(result) == {"environmental","flow","reliability","venueops"}'
  quiet_dashboard "$1"
}

lease_code="$(curl -ksS "${resolve[@]}" -c "$cookie_a" -o /dev/null -w '%{http_code}' \
  -X POST "$DEMO_ORIGIN/api/v1/demo/leases")"
[[ "$lease_code" == "201" ]]
quiet_dashboard "$cookie_a"
occupied_code="$(curl -ksS "${resolve[@]}" -c "$cookie_b" -o /dev/null -w '%{http_code}' \
  -X POST "$DEMO_ORIGIN/api/v1/demo/leases")"
[[ "$occupied_code" == "409" ]]
curl "${curl_tls[@]}" -b "$cookie_a" -X POST \
  "$DEMO_ORIGIN/api/v1/demo/leases/heartbeat" >/dev/null

if [[ "$MANAGE_STACK" -eq 1 ]]; then
  # A partial producer failure must retain the non-quiet VenueOps state and
  # failed lease for an idempotent retry with the server-owned reset identity.
  reset_failure="$(mktemp "${TMPDIR:-/tmp}/lm-demo-reset-failure.XXXXXX")"
  reset_retry="$(mktemp "${TMPDIR:-/tmp}/lm-demo-reset-retry.XXXXXX")"
  trap 'rm -f "$CONFIG_JSON" "$cookie_a" "$cookie_b" "$reset_failure" "$reset_retry"' EXIT

  curl "${curl_tls[@]}" -b "$cookie_a" -X POST \
    "$DEMO_ORIGIN/api/v1/demo/scenarios/lightning-hold" >/dev/null
  non_quiet_dashboard "$cookie_a"
  "${COMPOSE[@]}" stop reliability-intelligence

  failure_code="$(curl -ksS "${resolve[@]}" -b "$cookie_a" -o "$reset_failure" \
    -w '%{http_code}' -X POST "$DEMO_ORIGIN/api/v1/demo/reset")"
  [[ "$failure_code" == "503" ]]
  failed_reset_id="$(
    python3 - "$reset_failure" <<'PY'
import json, sys
body = json.load(open(sys.argv[1]))
assert body["component"] == "reliability", body
assert body["status"] == 503, body
print(body["resetId"])
PY
  )"
  curl "${curl_tls[@]}" -b "$cookie_a" "$DEMO_ORIGIN/api/v1/demo/lease" |
    python3 -c 'import json,sys; body=json.load(sys.stdin); assert body["state"] == "FAILED" and body["holder"] is True and body["available"] is False, body'
  non_quiet_dashboard "$cookie_a"

  "${COMPOSE[@]}" start reliability-intelligence
  for _ in $(seq 1 60); do
    if curl "${curl_tls[@]}" "$DEMO_ORIGIN/api/v1/demo/readiness" |
      python3 -c 'import json,sys; body=json.load(sys.stdin); assert any(c["name"] == "Reliability" and c["state"] == "READY" for c in body["components"])' \
      >/dev/null 2>&1; then
      break
    fi
    sleep 1
  done
  curl "${curl_tls[@]}" "$DEMO_ORIGIN/api/v1/demo/readiness" |
    python3 -c 'import json,sys; body=json.load(sys.stdin); assert any(c["name"] == "Reliability" and c["state"] == "READY" for c in body["components"])'

  curl "${curl_tls[@]}" -b "$cookie_a" -X POST \
    "$DEMO_ORIGIN/api/v1/demo/reset" -o "$reset_retry"
  python3 - "$reset_retry" "$failed_reset_id" <<'PY'
import json, sys
body = json.load(open(sys.argv[1]))
assert body["resetId"] == sys.argv[2], body
assert body["reset"]["environmental"] == "REPLAY", body
assert body["reset"]["flow"] == "REPLAY", body
assert body["reset"]["reliability"] == "COMPLETED", body
assert body["reset"]["venueops"] == "COMPLETED", body
PY
  quiet_dashboard "$cookie_a"
  echo "PASS  partial reset preserves state and retries the same reset identity"
fi

# Mid-weather state must reset without shell or volume deletion.
curl "${curl_tls[@]}" -b "$cookie_a" -X POST \
  "$DEMO_ORIGIN/api/v1/demo/scenarios/lightning-hold" >/dev/null
reset_and_prove_quiet "$cookie_a"

# Spend the reliability episode, then prove the producer and VenueOps both reset.
curl "${curl_tls[@]}" -b "$cookie_a" -X POST \
  "$DEMO_ORIGIN/api/v1/demo/scenarios/cypress-vibration" >/dev/null
reliability_id="$(pending_subject "$cookie_a" PENDING_RELIABILITY_RECOMMENDATION)"
reliability_version="$(
  curl "${curl_tls[@]}" -b "$cookie_a" \
    "$DEMO_ORIGIN/api/v1/operator/maintenance/recommendations/$reliability_id" |
    python3 -c 'import json,sys; print(json.load(sys.stdin)["version"])'
)"
curl "${curl_tls[@]}" -b "$cookie_a" -X POST -H 'Content-Type: application/json' \
  "$DEMO_ORIGIN/api/v1/operator/maintenance/recommendations/$reliability_id/commands" \
  -d "{\"commandId\":\"$(python3 -c 'import uuid; print(uuid.uuid4())')\",\"type\":\"DISMISS\",\"expectedVersion\":$reliability_version,\"reason\":\"Hosted sandbox acceptance\"}" \
  >/dev/null
reset_and_prove_quiet "$cookie_a"

# Spend Mangrove's stable recommendation, reset, and prove the same scenario is reusable.
curl "${curl_tls[@]}" -b "$cookie_a" -X POST \
  "$DEMO_ORIGIN/api/v1/demo/scenarios/mangrove-disruption" >/dev/null
flow_id="$(pending_subject "$cookie_a" UNPUBLISHED_FLOW_RECOMMENDATION)"
flow_detail="$(
  curl "${curl_tls[@]}" -b "$cookie_a" \
    "$DEMO_ORIGIN/api/v1/operator/flow/recommendations/$flow_id"
)"
flow_version="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["recommendation"]["version"])' <<<"$flow_detail")"
curl "${curl_tls[@]}" -b "$cookie_a" -X POST -H 'Content-Type: application/json' \
  "$DEMO_ORIGIN/api/v1/operator/flow/recommendations/$flow_id/commands" \
  -d "{\"commandId\":\"$(python3 -c 'import uuid; print(uuid.uuid4())')\",\"type\":\"DISMISS\",\"expectedVersion\":$flow_version,\"reason\":\"Hosted sandbox acceptance\"}" \
  >/dev/null
reset_and_prove_quiet "$cookie_a"
curl "${curl_tls[@]}" -b "$cookie_a" -X POST \
  "$DEMO_ORIGIN/api/v1/demo/scenarios/mangrove-disruption" >/dev/null
replayed_flow_id="$(pending_subject "$cookie_a" UNPUBLISHED_FLOW_RECOMMENDATION)"
[[ "$replayed_flow_id" == "$flow_id" ]]
reset_and_prove_quiet "$cookie_a"

# Let the short acceptance lease expire. Its stale cookie cannot mutate, and
# the next visitor receives another quiet reset park.
sleep "${DEMO_ACCEPTANCE_EXPIRY_WAIT_SECONDS:-14}"
forbidden_code="$(curl -ksS "${resolve[@]}" -b "$cookie_a" -o /dev/null -w '%{http_code}' \
  -X POST "$DEMO_ORIGIN/api/v1/demo/scenarios/lightning-hold")"
[[ "$forbidden_code" == "403" ]]
reacquire_code="$(curl -ksS "${resolve[@]}" -c "$cookie_b" -o /dev/null -w '%{http_code}' \
  -X POST "$DEMO_ORIGIN/api/v1/demo/leases")"
[[ "$reacquire_code" == "201" ]]
quiet_dashboard "$cookie_b"
curl "${curl_tls[@]}" -b "$cookie_b" -X DELETE \
  "$DEMO_ORIGIN/api/v1/demo/leases/current" >/dev/null
echo "PASS  quiet acquisition, spectator, spent scenarios, reset replay, expiry, and reacquisition"

for service in venueops-api environmental-monitor park-flow-intelligence \
  reliability-intelligence venueops-console lumen-marsh-app venueops-db environmental-db; do
  if "${COMPOSE[@]}" port "$service" 8080 >/dev/null 2>&1 ||
     "${COMPOSE[@]}" port "$service" 5432 >/dev/null 2>&1; then
    echo "FAIL  $service unexpectedly publishes a host port" >&2
    exit 1
  fi
done
echo "PASS  service, producer, and database ports are private"

http_port="${DEMO_HTTP_PORT:-80}"
redirect_headers="$(mktemp "${TMPDIR:-/tmp}/lm-demo-redirect.XXXXXX")"
trap 'rm -f "$CONFIG_JSON" "$cookie_a" "$cookie_b" "$redirect_headers"' EXIT
curl -sS --resolve "${origin_host}:${http_port}:127.0.0.1" \
  -H "Host: $DEMO_HOST" -D "$redirect_headers" -o /dev/null \
  "http://${origin_host}:${http_port}/"
grep -Eq '^HTTP/[^ ]+ 30[18]' "$redirect_headers"
redirect_location="$(
  sed -n 's/^[Ll]ocation: \(.*\)\r$/\1/p' "$redirect_headers"
)"
[[ "$redirect_location" == "${DEMO_ORIGIN}/" ]]
echo "PASS  plaintext gateway redirects to TLS"

echo "Cutaway sandbox acceptance: all checks passed"
