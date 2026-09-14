#!/usr/bin/env bash
set -euo pipefail
# shellcheck source=./_lib.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_lib.sh"

require_cmd curl
require_cmd python3
load_env
./scripts/wait-for-ready.sh

VENUEOPS_URL="${VENUEOPS_PUBLIC_ORIGIN:-http://localhost:8080}"
MONITOR_URL="${ENVIRONMENTAL_MONITOR_PUBLIC_ORIGIN:-http://localhost:8000}"
# LOCAL_JWT Compose bridge token (console supervisor). Use a Cognito access token in demo/prod.
OPERATOR_TOKEN="${OPERATOR_TOKEN:-local-development-token}"
AUTH_HEADER=( -H "Authorization: Bearer ${OPERATOR_TOKEN}" )

pass=0
fail=0

check() {
  local name="$1"
  shift
  if "$@"; then
    echo "PASS  $name"
    pass=$((pass + 1))
  else
    echo "FAIL  $name" >&2
    fail=$((fail + 1))
  fi
}

http_ok() {
  local url="$1"
  curl -fsS "$url" >/dev/null
}

echo "Running platform smoke tests"

check "venueops health" http_ok "$VENUEOPS_URL/actuator/health"
check "monitor live" http_ok "$MONITOR_URL/health/live"
check "monitor ready" http_ok "$MONITOR_URL/health/ready"
check "guest attractions" http_ok "$VENUEOPS_URL/api/v1/attractions"
check "guest advisories" http_ok "$VENUEOPS_URL/api/v1/advisories"
check "operator weather inbox" bash -c "curl -fsS '$VENUEOPS_URL/api/v1/operator/weather/recommendations' -H 'Authorization: Bearer ${OPERATOR_TOKEN}' >/dev/null"
check "monitor weather status" http_ok "$MONITOR_URL/api/v1/weather/status"
check "console health" http_ok "${OPERATOR_CONSOLE_ORIGIN:-http://localhost:3001}/health"
check "guest app health" http_ok "${GUEST_APP_ORIGIN:-http://localhost:3000}/health"

# SSE connectivity: open stream briefly and expect an event or heartbeat comment
check "operator SSE connects" python3 - <<PY
import socket
import urllib.request
req = urllib.request.Request(
    "$VENUEOPS_URL/api/v1/operator/events",
    headers={"Accept": "text/event-stream", "Authorization": "Bearer $OPERATOR_TOKEN"},
)
with urllib.request.urlopen(req, timeout=8) as resp:
    chunk = resp.read(512)
    text = chunk.decode("utf-8", errors="replace")
    assert "event:" in text or "data:" in text or ":" in text, text[:200]
print("ok")
PY

# Idempotent recommendation ingest
REC_ID="bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
PAYLOAD=$(cat <<EOF
{
  "recommendationId": "$REC_ID",
  "ruleId": "simulated-lightning-hold",
  "status": "ACTIVE",
  "severity": "WARNING",
  "summary": "Smoke test weather hold recommendation",
  "evidence": "Simulated smoke-test lightning; not an NWS observation.",
  "recommendedAction": "Review Mangrove Run for weather hold",
  "affectedAttractionIds": ["mangrove-run"],
  "observedAt": "2026-09-02T17:00:00Z",
  "version": 1
}
EOF
)

FIRST="$(curl -fsS -o /tmp/lm-smoke-1.json -w '%{http_code}' \
  -X POST "$VENUEOPS_URL/api/v1/integrations/weather/recommendations" \
  -H 'Content-Type: application/json' \
  -d "$PAYLOAD")"
SECOND="$(curl -fsS -o /tmp/lm-smoke-2.json -w '%{http_code}' \
  -X POST "$VENUEOPS_URL/api/v1/integrations/weather/recommendations" \
  -H 'Content-Type: application/json' \
  -d "$PAYLOAD")"

check "recommendation create/update accepted" bash -c "[[ '$FIRST' == '200' || '$FIRST' == '201' ]]"
check "retried recommendation is duplicate-safe" bash -c "[[ '$SECOND' == '200' ]]"
check "duplicate keeps same source version" bash -c "
  python3 - <<'PY'
import json
a=json.load(open('/tmp/lm-smoke-1.json'))
b=json.load(open('/tmp/lm-smoke-2.json'))
assert a['sourceVersion']==b['sourceVersion']==1
assert a['id']==b['id']
print('ok')
PY
"

# Incident create + attraction command
INCIDENT="$(curl -fsS -X POST "$VENUEOPS_URL/api/v1/operator/incidents" \
  -H 'Content-Type: application/json' "${AUTH_HEADER[@]}" \
  -d '{"title":"Smoke weather incident","type":"WEATHER","severity":"MAJOR","internalDescription":"Smoke test","attractionIds":["mangrove-run"]}')"
INCIDENT_ID="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])' <<<"$INCIDENT")"
check "incident created" bash -c "[[ -n '$INCIDENT_ID' ]]"

VERSION="$(curl -fsS "$VENUEOPS_URL/api/v1/operator/attractions/mangrove-run" "${AUTH_HEADER[@]}" \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["version"])')"
HOLD="$(curl -fsS -o /tmp/lm-smoke-hold.json -w '%{http_code}' \
  -X POST "$VENUEOPS_URL/api/v1/operator/attractions/mangrove-run/commands" \
  -H 'Content-Type: application/json' "${AUTH_HEADER[@]}" \
  -d "{\"type\":\"PLACE_WEATHER_HOLD\",\"reason\":\"Smoke test hold\",\"expectedVersion\":$VERSION}")"
check "attraction weather hold command" bash -c "[[ '$HOLD' == '200' ]]"

# Advisory path: publish on the smoke incident
ACK="$(curl -fsS -o /dev/null -w '%{http_code}' \
  -X POST "$VENUEOPS_URL/api/v1/operator/incidents/$INCIDENT_ID/commands" \
  -H 'Content-Type: application/json' "${AUTH_HEADER[@]}" \
  -d '{"type":"ACKNOWLEDGE","expectedVersion":1}')"
PUB="$(curl -fsS -o /dev/null -w '%{http_code}' \
  -X POST "$VENUEOPS_URL/api/v1/operator/incidents/$INCIDENT_ID/commands" \
  -H 'Content-Type: application/json' "${AUTH_HEADER[@]}" \
  -d '{"type":"PUBLISH_GUEST_ADVISORY","guestTitle":"Smoke advisory","guestMessage":"Temporary pause for testing.","expectedVersion":2}')"
check "incident acknowledge" bash -c "[[ '$ACK' == '200' ]]"
check "guest advisory publish" bash -c "[[ '$PUB' == '200' ]]"
check "guest advisories list" http_ok "$VENUEOPS_URL/api/v1/advisories"
echo "Note: full sanitize + withdraw proof is scripts/advisory-demo.sh (see docs/guest-advisory-delivery-checklist.md)"

echo
echo "Smoke summary: $pass passed, $fail failed"
if [[ "$fail" -gt 0 ]]; then
  exit 1
fi
