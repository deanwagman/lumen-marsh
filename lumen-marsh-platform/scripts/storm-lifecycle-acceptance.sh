#!/usr/bin/env bash
set -euo pipefail
# Repeatable storm lifecycle acceptance: monitor → incident → hold → advisory →
# guest leak checks → clearance → recover → audit. Includes stale-version, unauthorized,
# disconnected SSE, and optional API restart checks.
# shellcheck source=./_lib.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_lib.sh"

require_cmd curl
require_cmd python3
load_env

VENUEOPS_URL="${VENUEOPS_PUBLIC_ORIGIN:-http://localhost:8080}"
MONITOR_URL="${ENVIRONMENTAL_MONITOR_PUBLIC_ORIGIN:-http://localhost:8000}"
GUEST_URL="${GUEST_APP_ORIGIN:-http://localhost:3000}"
INTERNAL_MARKER="INTERNAL ONLY — storm-lifecycle-acceptance"
GUEST_TITLE="Outdoor weather pause"
FORBIDDEN_KEYS='internalDescription assignedTo actor activity activities reason'
WORKDIR="$(mktemp -d "${TMPDIR:-/tmp}/lm-storm-lifecycle.XXXXXX")"
trap 'rm -rf "$WORKDIR"' EXIT

if curl -fsS --max-time 2 "http://127.0.0.1:5173/health" >/dev/null 2>&1; then
  ./scripts/wait-for-ready.sh --oidc
else
  ./scripts/wait-for-ready.sh
fi

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

http_code() {
  local out="$1"
  shift
  curl -sS -o "$out" -w '%{http_code}' "$@" || true
}

detect_auth_mode() {
  local code
  code="$(http_code "$WORKDIR/probe.json" \
    -H "Authorization: Bearer local-development-token" \
    "$VENUEOPS_URL/api/v1/operator/incidents")"
  if [[ "$code" == "200" ]]; then
    echo "LOCAL_JWT"
  else
    echo "OIDC"
  fi
}

AUTH_MODE="$(detect_auth_mode)"
if [[ "$AUTH_MODE" == "LOCAL_JWT" ]]; then
  SUPERVISOR_TOKEN="${SUPERVISOR_TOKEN:-${OPERATOR_TOKEN:-local-development-token}}"
  OPERATOR_LIMITED_TOKEN="${OPERATOR_LIMITED_TOKEN:-local-operator-token}"
else
  if [[ -z "${SUPERVISOR_TOKEN:-}" ]]; then
    cat <<'EOF' >&2
error: VenueOps is in OIDC mode and SUPERVISOR_TOKEN is not set.

Export a Cognito access token for a supervisors-group user (never paste tokens into chat):

  SUPERVISOR_TOKEN=... OPERATOR_LIMITED_TOKEN=... ./scripts/storm-lifecycle-acceptance.sh

OPERATOR_LIMITED_TOKEN must be an operators-group token without venueops/advisories.publish.
Create the local supervisor with ./scripts/create-dev-supervisor.sh, sign in to Control Tower,
and copy the access token from the browser session store.

LOCAL_JWT Compose remains the offline default: ./scripts/dev-up.sh && ./scripts/storm-lifecycle-acceptance.sh
EOF
    exit 1
  fi
  if [[ -z "${OPERATOR_LIMITED_TOKEN:-}" ]]; then
    echo "error: OIDC mode also requires OPERATOR_LIMITED_TOKEN for unauthorized-action checks" >&2
    exit 1
  fi
fi

SUPERVISOR_AUTH=( -H "Authorization: Bearer ${SUPERVISOR_TOKEN}" )
OPERATOR_AUTH=( -H "Authorization: Bearer ${OPERATOR_LIMITED_TOKEN}" )

post_json() {
  local url="$1"
  local body="$2"
  shift 2
  curl -fsS -X POST "$url" -H 'Content-Type: application/json' "$@" -d "$body"
}

incident_command() {
  local incident_id="$1"
  local body="$2"
  shift 2
  post_json "$VENUEOPS_URL/api/v1/operator/incidents/$incident_id/commands" "$(envelope_command "$body")" "$@"
}

attraction_command() {
  local attraction_id="$1"
  local body="$2"
  shift 2
  post_json "$VENUEOPS_URL/api/v1/operator/attractions/$attraction_id/commands" "$(envelope_command "$body")" "$@"
}

incident_version() {
  curl -fsS "$VENUEOPS_URL/api/v1/operator/incidents/$1" "${SUPERVISOR_AUTH[@]}" \
    | python3 -c 'import json,sys; print(json.load(sys.stdin)["version"])'
}

attraction_json() {
  curl -fsS "$VENUEOPS_URL/api/v1/operator/attractions/$1" "${SUPERVISOR_AUTH[@]}"
}

assert_guest_payload_sanitized() {
  local json_file="$1"
  FORBIDDEN_KEYS="$FORBIDDEN_KEYS" INTERNAL_MARKER="$INTERNAL_MARKER" python3 - "$json_file" <<'PY'
import json, os, sys
path = sys.argv[1]
raw = open(path).read()
data = json.loads(raw)
items = data if isinstance(data, list) else [data]
if isinstance(data, dict) and "advisory" in data:
    items = [data["advisory"]]
allowed = {"id", "severity", "title", "message", "affectedAttractionIds", "updatedAt", "version"}
forbidden = os.environ["FORBIDDEN_KEYS"].split()
marker = os.environ["INTERNAL_MARKER"]
assert marker not in raw, f"internal marker leaked in {path}"
for item in items:
    if not isinstance(item, dict):
        continue
    extra = set(item) - allowed
    assert not extra, f"unexpected guest fields: {extra}"
    for key in forbidden:
        assert key not in item, f"forbidden key present: {key}"
        assert f'"{key}"' not in json.dumps(item), f"forbidden key leaked: {key}"
print("ok")
PY
}

assert_monotonic_activity() {
  local json_file="$1"
  python3 - "$json_file" <<'PY'
import json, sys
events = json.load(open(sys.argv[1]))
assert events, "empty activity"
versions = [int(event["resultingVersion"]) for event in events]
assert versions == list(range(1, len(events) + 1)), versions
for event in events:
    assert int(event["resultingVersion"]) == int(event["previousVersion"]) + 1, event
    assert event.get("actor"), event
    assert event["actor"] != "spoofed-attacker"
    assert event["actor"] != "spoofed-supervisor"
print("ok")
PY
}

echo "Running storm lifecycle acceptance (auth: $AUTH_MODE)"

echo "== Failure: unauthenticated operator commands =="
UNAUTH_CODE="$(http_code "$WORKDIR/unauth.json" \
  -X POST "$VENUEOPS_URL/api/v1/operator/incidents" \
  -H 'Content-Type: application/json' \
  -d '{"title":"unauth","type":"WEATHER","severity":"MAJOR","internalDescription":"nope"}')"
check "unauthenticated incident report is 401" bash -c "[[ '$UNAUTH_CODE' == '401' ]]"

SSE_UNAUTH="$(http_code "$WORKDIR/sse-unauth.txt" \
  -H 'Accept: text/event-stream' \
  "$VENUEOPS_URL/api/v1/operator/events")"
check "unauthenticated operator SSE is 401" bash -c "[[ '$SSE_UNAUTH' == '401' ]]"

echo "== 1. Simulated hold conditions =="
post_json "$MONITOR_URL/api/v1/simulation/scenarios/clear" '{}' >/dev/null
post_json "$MONITOR_URL/api/v1/simulation/scenarios/hold-conditions" '{}' >/dev/null

REC_JSON=""
for _ in $(seq 1 30); do
  REC_JSON="$(curl -fsS "$VENUEOPS_URL/api/v1/operator/weather/recommendations" "${SUPERVISOR_AUTH[@]}")"
  ACTIVE="$(python3 -c 'import json,sys; items=json.load(sys.stdin); print(sum(1 for i in items if i.get("status")=="ACTIVE"))' <<<"$REC_JSON")"
  if [[ "$ACTIVE" -ge 1 ]]; then
    break
  fi
  sleep 2
done
ACTIVE="$(python3 -c 'import json,sys; items=json.load(sys.stdin); print(sum(1 for i in items if i.get("status")=="ACTIVE"))' <<<"$REC_JSON")"
check "weather recommendation arrived" bash -c "[[ '$ACTIVE' -ge 1 ]]"
REC_ID="$(python3 -c 'import json,sys; items=json.load(sys.stdin); print(next(i["id"] for i in items if i["status"]=="ACTIVE"))' <<<"$REC_JSON")"
REC_VERSION="$(python3 -c 'import json,sys; items=json.load(sys.stdin); print(next(i["version"] for i in items if i["id"]==sys.argv[1]))' "$REC_ID" <<<"$REC_JSON")"
echo "recommendation $REC_ID (version $REC_VERSION)"

echo "== 2-3. Review recommendation and create / acknowledge / assign / mitigate =="
INCIDENT_BODY="$(REC_JSON="$REC_JSON" REC_ID="$REC_ID" INTERNAL_MARKER="$INTERNAL_MARKER" python3 - <<'PY'
import json, os
items = json.loads(os.environ["REC_JSON"])
rec = next(i for i in items if i["id"] == os.environ["REC_ID"])
print(json.dumps({
    "title": rec["summary"],
    "type": "WEATHER",
    "severity": "MAJOR",
    "internalDescription": os.environ["INTERNAL_MARKER"] + " " + rec.get("evidence", ""),
    "attractionIds": ["mangrove-run", "cypress-coil"],
}))
PY
)"
INCIDENT="$(post_json "$VENUEOPS_URL/api/v1/operator/incidents" \
  "$INCIDENT_BODY" \
  "${SUPERVISOR_AUTH[@]}" \
  -H 'X-Actor: spoofed-supervisor')"
INCIDENT_ID="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])' <<<"$INCIDENT")"
check "incident created" bash -c "[[ -n '$INCIDENT_ID' ]]"

post_json "$VENUEOPS_URL/api/v1/operator/weather/recommendations/$REC_ID/commands" \
  "{\"type\":\"LINK_INCIDENT\",\"incidentId\":\"$INCIDENT_ID\",\"expectedVersion\":$REC_VERSION}" \
  "${SUPERVISOR_AUTH[@]}" >/dev/null

VER="$(incident_version "$INCIDENT_ID")"
incident_command "$INCIDENT_ID" "{\"type\":\"ACKNOWLEDGE\",\"expectedVersion\":$VER}" "${SUPERVISOR_AUTH[@]}" >/dev/null
VER="$(incident_version "$INCIDENT_ID")"
incident_command "$INCIDENT_ID" "{\"type\":\"ASSIGN\",\"assignee\":\"Control Tower\",\"expectedVersion\":$VER}" "${SUPERVISOR_AUTH[@]}" >/dev/null
VER="$(incident_version "$INCIDENT_ID")"
incident_command "$INCIDENT_ID" "{\"type\":\"START_MITIGATION\",\"expectedVersion\":$VER}" "${SUPERVISOR_AUTH[@]}" >/dev/null
STATUS="$(curl -fsS "$VENUEOPS_URL/api/v1/operator/incidents/$INCIDENT_ID" "${SUPERVISOR_AUTH[@]}" \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["status"])')"
check "incident is mitigating" bash -c "[[ '$STATUS' == 'MITIGATING' ]]"

echo "== Failure: operator cannot publish =="
VER="$(incident_version "$INCIDENT_ID")"
OP_PUB_CODE="$(http_code "$WORKDIR/op-pub.json" \
  -X POST "$VENUEOPS_URL/api/v1/operator/incidents/$INCIDENT_ID/commands" \
  -H 'Content-Type: application/json' \
  "${OPERATOR_AUTH[@]}" \
  -d "$(envelope_command "{\"type\":\"PUBLISH_GUEST_ADVISORY\",\"guestTitle\":\"$GUEST_TITLE\",\"guestMessage\":\"Should be forbidden.\",\"expectedVersion\":$VER}")")"
check "operator publish is 403" bash -c "[[ '$OP_PUB_CODE' == '403' ]]"

echo "== 4. Place weather hold (including stale version) =="

ensure_operating() {
  local id="$1"
  local status version
  status="$(attraction_json "$id" | python3 -c 'import json,sys; print(json.load(sys.stdin)["status"])')"
  version="$(attraction_json "$id" | python3 -c 'import json,sys; print(json.load(sys.stdin)["version"])')"
  case "$status" in
    OPERATING) return 0 ;;
    WEATHER_HOLD)
      attraction_command "$id" \
        "{\"type\":\"CLEAR_WEATHER_HOLD\",\"reason\":\"Reset before lifecycle acceptance\",\"expectedVersion\":$version}" \
        "${SUPERVISOR_AUTH[@]}" >/dev/null
      version=$((version + 1))
      attraction_command "$id" \
        "{\"type\":\"COMPLETE_TESTING\",\"expectedVersion\":$version}" "${SUPERVISOR_AUTH[@]}" >/dev/null
      version=$((version + 1))
      attraction_command "$id" \
        "{\"type\":\"APPROVE_RETURN_TO_SERVICE\",\"reason\":\"Reset before lifecycle acceptance\",\"expectedVersion\":$version}" \
        "${SUPERVISOR_AUTH[@]}" >/dev/null
      ;;
    CLOSED)
      attraction_command "$id" \
        "{\"type\":\"START_TESTING\",\"expectedVersion\":$version}" "${SUPERVISOR_AUTH[@]}" >/dev/null
      version=$((version + 1))
      attraction_command "$id" \
        "{\"type\":\"COMPLETE_TESTING\",\"expectedVersion\":$version}" "${SUPERVISOR_AUTH[@]}" >/dev/null
      version=$((version + 1))
      attraction_command "$id" \
        "{\"type\":\"APPROVE_RETURN_TO_SERVICE\",\"reason\":\"Opened for lifecycle acceptance\",\"expectedVersion\":$version}" \
        "${SUPERVISOR_AUTH[@]}" >/dev/null
      ;;
    TESTING)
      attraction_command "$id" \
        "{\"type\":\"COMPLETE_TESTING\",\"expectedVersion\":$version}" "${SUPERVISOR_AUTH[@]}" >/dev/null
      version=$((version + 1))
      attraction_command "$id" \
        "{\"type\":\"APPROVE_RETURN_TO_SERVICE\",\"reason\":\"Opened for lifecycle acceptance\",\"expectedVersion\":$version}" \
        "${SUPERVISOR_AUTH[@]}" >/dev/null
      ;;
    RETURNING_TO_SERVICE)
      attraction_command "$id" \
        "{\"type\":\"APPROVE_RETURN_TO_SERVICE\",\"reason\":\"Opened for lifecycle acceptance\",\"expectedVersion\":$version}" \
        "${SUPERVISOR_AUTH[@]}" >/dev/null
      ;;
    *)
      echo "error: unsupported attraction status for $id: $status" >&2
      return 1
      ;;
  esac
}

ensure_operating mangrove-run
ensure_operating cypress-coil

M_VER="$(attraction_json mangrove-run | python3 -c 'import json,sys; print(json.load(sys.stdin)["version"])')"
STALE_CODE="$(http_code "$WORKDIR/stale.json" \
  -X POST "$VENUEOPS_URL/api/v1/operator/attractions/mangrove-run/commands" \
  -H 'Content-Type: application/json' \
  "${SUPERVISOR_AUTH[@]}" \
  -d "$(envelope_command "{\"type\":\"PLACE_WEATHER_HOLD\",\"reason\":\"stale\",\"expectedVersion\":$((M_VER + 99))}")")"
check "stale weather hold is 409" bash -c "[[ '$STALE_CODE' == '409' ]]"

attraction_command mangrove-run \
  "{\"type\":\"PLACE_WEATHER_HOLD\",\"reason\":\"Lightning detected within operating radius\",\"expectedVersion\":$M_VER}" \
  "${SUPERVISOR_AUTH[@]}" >/dev/null
C_VER="$(attraction_json cypress-coil | python3 -c 'import json,sys; print(json.load(sys.stdin)["version"])')"
attraction_command cypress-coil \
  "{\"type\":\"PLACE_WEATHER_HOLD\",\"reason\":\"Lightning detected within operating radius\",\"expectedVersion\":$C_VER}" \
  "${SUPERVISOR_AUTH[@]}" >/dev/null
M_STATUS="$(attraction_json mangrove-run | python3 -c 'import json,sys; print(json.load(sys.stdin)["status"])')"
check "mangrove-run is on weather hold" bash -c "[[ '$M_STATUS' == 'WEATHER_HOLD' ]]"

echo "== 5. Supervisor publishes guest advisory =="
VER="$(incident_version "$INCIDENT_ID")"
incident_command "$INCIDENT_ID" \
  "{\"type\":\"PUBLISH_GUEST_ADVISORY\",\"guestTitle\":\"$GUEST_TITLE\",\"guestMessage\":\"Some outdoor attractions are temporarily paused.\",\"expectedVersion\":$VER}" \
  "${SUPERVISOR_AUTH[@]}" >/dev/null

curl -fsS "$VENUEOPS_URL/api/v1/advisories" -o "$WORKDIR/advisories.json"
check "guest REST list is sanitized" assert_guest_payload_sanitized "$WORKDIR/advisories.json"
check "guest REST omits internal marker" bash -c "! grep -q 'INTERNAL ONLY' '$WORKDIR/advisories.json'"

echo "== 6. Guest SSE receives advisory without operator fields =="
check "guest SSE snapshot is sanitized" bash -c "
  python3 - '$VENUEOPS_URL/api/v1/events' '$WORKDIR/guest-sse.txt' '$FORBIDDEN_KEYS' '$INTERNAL_MARKER' <<'PY'
import json, os, sys, urllib.request
url, out, forbidden, marker = sys.argv[1], sys.argv[2], sys.argv[3].split(), sys.argv[4]
req = urllib.request.Request(url, headers={'Accept': 'text/event-stream'})
with urllib.request.urlopen(req, timeout=8) as resp:
    chunk = resp.read(8192).decode('utf-8', errors='replace')
open(out, 'w').write(chunk)
assert 'advisories.snapshot' in chunk or 'advisory.published' in chunk, chunk[:300]
assert marker not in chunk, 'internal marker leaked on guest SSE'
for key in forbidden:
    assert f'\"{key}\"' not in chunk, f'forbidden key leaked on guest SSE: {key}'
print('ok')
PY
"

check "authenticated operator SSE connects" bash -c "
  python3 - '$VENUEOPS_URL/api/v1/operator/events' '$SUPERVISOR_TOKEN' '$WORKDIR/op-sse.txt' <<'PY'
import sys, urllib.request
url, token, out = sys.argv[1], sys.argv[2], sys.argv[3]
req = urllib.request.Request(url, headers={'Accept': 'text/event-stream', 'Authorization': f'Bearer {token}'})
with urllib.request.urlopen(req, timeout=8) as resp:
    chunk = resp.read(1024).decode('utf-8', errors='replace')
open(out, 'w').write(chunk)
assert 'event:' in chunk or 'data:' in chunk or ':' in chunk, chunk[:200]
print('ok')
PY
"

echo "== Failure: disconnected SSE reconnects with a fresh snapshot =="
check "guest SSE reconnects after disconnect" bash -c "
  python3 - '$VENUEOPS_URL/api/v1/events' <<'PY'
import urllib.request, sys
url = sys.argv[1]
req = urllib.request.Request(url, headers={'Accept': 'text/event-stream'})
with urllib.request.urlopen(req, timeout=8) as resp:
    first = resp.read(1024)
assert b'event:' in first or b'data:' in first
with urllib.request.urlopen(req, timeout=8) as resp:
    second = resp.read(1024)
assert b'attractions.snapshot' in second or b'advisories.snapshot' in second or b'event:' in second
print('ok')
PY
"

echo "== 7. Clearance conditions =="
post_json "$MONITOR_URL/api/v1/simulation/scenarios/clearance-period" '{}' >/dev/null
for _ in $(seq 1 30); do
  STATUS="$(curl -fsS "$VENUEOPS_URL/api/v1/operator/weather/recommendations/$REC_ID" "${SUPERVISOR_AUTH[@]}" \
    | python3 -c 'import json,sys; print(json.load(sys.stdin)["status"])')"
  if [[ "$STATUS" == "CLEARED" ]]; then
    break
  fi
  sleep 2
done
check "recommendation cleared" bash -c "[[ '$STATUS' == 'CLEARED' ]]"

echo "== 8. Restore attractions and resolve incident =="
M_VER="$(attraction_json mangrove-run | python3 -c 'import json,sys; print(json.load(sys.stdin)["version"])')"
attraction_command mangrove-run \
  "{\"type\":\"CLEAR_WEATHER_HOLD\",\"reason\":\"Storm cell moved out of radius\",\"expectedVersion\":$M_VER}" \
  "${SUPERVISOR_AUTH[@]}" >/dev/null
attraction_command mangrove-run \
  "{\"type\":\"COMPLETE_TESTING\",\"expectedVersion\":$((M_VER + 1))}" "${SUPERVISOR_AUTH[@]}" >/dev/null
attraction_command mangrove-run \
  "{\"type\":\"APPROVE_RETURN_TO_SERVICE\",\"reason\":\"Return to service approved\",\"expectedVersion\":$((M_VER + 2))}" \
  "${SUPERVISOR_AUTH[@]}" >/dev/null

C_VER="$(attraction_json cypress-coil | python3 -c 'import json,sys; print(json.load(sys.stdin)["version"])')"
attraction_command cypress-coil \
  "{\"type\":\"CLEAR_WEATHER_HOLD\",\"reason\":\"Storm cell moved out of radius\",\"expectedVersion\":$C_VER}" \
  "${SUPERVISOR_AUTH[@]}" >/dev/null
attraction_command cypress-coil \
  "{\"type\":\"COMPLETE_TESTING\",\"expectedVersion\":$((C_VER + 1))}" "${SUPERVISOR_AUTH[@]}" >/dev/null
attraction_command cypress-coil \
  "{\"type\":\"APPROVE_RETURN_TO_SERVICE\",\"reason\":\"Return to service approved\",\"expectedVersion\":$((C_VER + 2))}" \
  "${SUPERVISOR_AUTH[@]}" >/dev/null

INC_VER="$(incident_version "$INCIDENT_ID")"
incident_command "$INCIDENT_ID" \
  "{\"type\":\"RESOLVE\",\"reason\":\"Storm cell moved out of radius\",\"expectedVersion\":$INC_VER}" \
  "${SUPERVISOR_AUTH[@]}" >/dev/null
RESOLVED="$(curl -fsS "$VENUEOPS_URL/api/v1/operator/incidents/$INCIDENT_ID" "${SUPERVISOR_AUTH[@]}" \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["status"])')"
check "incident resolved" bash -c "[[ '$RESOLVED' == 'RESOLVED' ]]"

echo "== 9. Guest advisory removed =="
curl -fsS "$VENUEOPS_URL/api/v1/advisories" -o "$WORKDIR/advisories-after.json"
check "guest list no longer contains advisory" bash -c "
  GUEST_TITLE='$GUEST_TITLE' python3 - '$WORKDIR/advisories-after.json' <<'PY'
import json, os, sys
data = json.load(open(sys.argv[1]))
title = os.environ['GUEST_TITLE']
assert all(item.get('title') != title for item in data), data
print('ok')
PY
"

echo "== 10. Audit actors and monotonic versions =="
curl -fsS "$VENUEOPS_URL/api/v1/operator/incidents/$INCIDENT_ID/activity" "${SUPERVISOR_AUTH[@]}" \
  -o "$WORKDIR/incident-activity.json"
check "incident audit is authenticated and monotonic" assert_monotonic_activity "$WORKDIR/incident-activity.json"
curl -fsS "$VENUEOPS_URL/api/v1/operator/attractions/mangrove-run/activity" "${SUPERVISOR_AUTH[@]}" \
  -o "$WORKDIR/attraction-activity.json"
check "attraction audit ignores forged X-Actor" bash -c "
  python3 - '$WORKDIR/attraction-activity.json' <<'PY'
import json, sys
events = json.load(open(sys.argv[1]))
assert events
assert all(event.get('actor') not in {'spoofed-attacker', 'spoofed-supervisor'} for event in events)
print('ok')
PY
"

echo "== Failure: API restart keeps operator and guest state =="
if [[ "${SKIP_SERVICE_RESTART:-0}" == "1" ]]; then
  echo "SKIP  API process restart (SKIP_SERVICE_RESTART=1)"
elif docker compose -p lumen-marsh ps --status running --format '{{.Name}}' 2>/dev/null | grep -q venueops-api; then
  docker compose -p lumen-marsh restart venueops-api >/dev/null
  wait_http "$VENUEOPS_URL/actuator/health" "venueops-api after restart" 60
  AFTER="$(curl -fsS "$VENUEOPS_URL/api/v1/operator/incidents/$INCIDENT_ID" "${SUPERVISOR_AUTH[@]}" \
    | python3 -c 'import json,sys; print(json.load(sys.stdin)["status"])')"
  check "incident survives API restart" bash -c "[[ '$AFTER' == 'RESOLVED' ]]"
  curl -fsS "$VENUEOPS_URL/api/v1/advisories" -o "$WORKDIR/advisories-restart.json"
  check "guest advisories stay empty after restart" bash -c "
    python3 - '$WORKDIR/advisories-after.json' '$WORKDIR/advisories-restart.json' <<'PY'
import json, sys
assert json.load(open(sys.argv[1])) == json.load(open(sys.argv[2]))
print('ok')
PY
  "
else
  echo "SKIP  API process restart (compose project lumen-marsh is not running)"
fi

post_json "$MONITOR_URL/api/v1/simulation/scenarios/clear" '{}' >/dev/null

echo
echo "Storm lifecycle acceptance: $pass passed, $fail failed"
echo "  Recommendation: $REC_ID"
echo "  Incident:       $INCIDENT_ID"
echo "  Guest app:      $GUEST_URL"
if [[ "$fail" -gt 0 ]]; then
  exit 1
fi
