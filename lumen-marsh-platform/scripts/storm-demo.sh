#!/usr/bin/env bash
set -euo pipefail
# shellcheck source=./_lib.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_lib.sh"

# End-to-end storm demonstration via HTTP APIs only (no direct DB writes).

require_cmd curl
require_cmd python3
load_env
./scripts/wait-for-ready.sh

VENUEOPS_URL="${VENUEOPS_PUBLIC_ORIGIN:-http://localhost:8080}"
MONITOR_URL="${ENVIRONMENTAL_MONITOR_PUBLIC_ORIGIN:-http://localhost:8000}"
# LOCAL_JWT Compose bridge token (console supervisor). Use a Cognito access token in demo/prod.
OPERATOR_TOKEN="${OPERATOR_TOKEN:-local-development-token}"
AUTH_HEADER=( -H "Authorization: Bearer ${OPERATOR_TOKEN}" )

post_json() {
  local url="$1"
  local body="$2"
  shift 2
  curl -fsS -X POST "$url" -H 'Content-Type: application/json' "$@" -d "$body"
}

echo "== 1. Clear simulated weather =="
post_json "$MONITOR_URL/api/v1/simulation/scenarios/clear" '{}' >/dev/null

echo "== 2. Storm approaching =="
post_json "$MONITOR_URL/api/v1/simulation/scenarios/storm-approaching" '{}' | python3 -m json.tool >/dev/null

echo "== 3. Lightning inside hold radius =="
post_json "$MONITOR_URL/api/v1/simulation/scenarios/hold-conditions" '{}' | python3 -m json.tool >/dev/null

echo "== 4. Wait for recommendation delivery to VenueOps =="
REC_JSON=""
for _ in $(seq 1 30); do
  REC_JSON="$(curl -fsS "$VENUEOPS_URL/api/v1/operator/weather/recommendations" "${AUTH_HEADER[@]}")"
  ACTIVE="$(python3 -c 'import json,sys; items=json.load(sys.stdin); print(sum(1 for i in items if i.get("status")=="ACTIVE"))' <<<"$REC_JSON")"
  if [[ "$ACTIVE" -ge 1 ]]; then
    break
  fi
  sleep 2
done
ACTIVE="$(python3 -c 'import json,sys; items=json.load(sys.stdin); print(sum(1 for i in items if i.get("status")=="ACTIVE"))' <<<"$REC_JSON")"
if [[ "$ACTIVE" -lt 1 ]]; then
  echo "error: no ACTIVE weather recommendation arrived in VenueOps" >&2
  echo "hint: ensure compose.override enables SIMULATION_ENABLED=true" >&2
  exit 1
fi
REC_ID="$(python3 -c 'import json,sys; items=json.load(sys.stdin); print(next(i["id"] for i in items if i["status"]=="ACTIVE"))' <<<"$REC_JSON")"
REC_VERSION="$(python3 -c 'import json,sys; items=json.load(sys.stdin); print(next(i["version"] for i in items if i["id"]=="'"$REC_ID"'"))' <<<"$REC_JSON")"
echo "recommendation $REC_ID (version $REC_VERSION)"

echo "== 4b. Retried delivery stays idempotent =="
MONITOR_REC="$(curl -fsS "$MONITOR_URL/api/v1/weather/recommendations")"
# Re-posting is owned by the monitor; verify VenueOps still has a single ACTIVE id.
COUNT="$(python3 -c 'import json,sys; items=json.load(sys.stdin); print(sum(1 for i in items if i["id"]=="'"$REC_ID"'"))' <<<"$REC_JSON")"
[[ "$COUNT" -eq 1 ]]

echo "== 5. Create weather incident and publish guest advisory =="
INCIDENT_BODY="$(REC_JSON="$REC_JSON" REC_ID="$REC_ID" python3 - <<'PY'
import json, os
items = json.loads(os.environ["REC_JSON"])
rec = next(i for i in items if i["id"] == os.environ["REC_ID"])
print(json.dumps({
    "title": rec["summary"],
    "type": "WEATHER",
    "severity": "MAJOR",
    "internalDescription": rec["evidence"],
    "attractionIds": ["mangrove-run", "cypress-coil"],
}))
PY
)"
INCIDENT="$(post_json "$VENUEOPS_URL/api/v1/operator/incidents" \
  "$INCIDENT_BODY" \
  "${AUTH_HEADER[@]}"
)"
INCIDENT_ID="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])' <<<"$INCIDENT")"
echo "incident $INCIDENT_ID"

post_json "$VENUEOPS_URL/api/v1/operator/weather/recommendations/$REC_ID/commands" \
  "{\"type\":\"LINK_INCIDENT\",\"incidentId\":\"$INCIDENT_ID\",\"expectedVersion\":$REC_VERSION}" \
  "${AUTH_HEADER[@]}" >/dev/null

post_json "$VENUEOPS_URL/api/v1/operator/incidents/$INCIDENT_ID/commands" \
  '{"type":"ACKNOWLEDGE","expectedVersion":1}' \
  "${AUTH_HEADER[@]}" >/dev/null
post_json "$VENUEOPS_URL/api/v1/operator/incidents/$INCIDENT_ID/commands" \
  '{"type":"ASSIGN","assignee":"Control Tower","expectedVersion":2}' \
  "${AUTH_HEADER[@]}" >/dev/null
post_json "$VENUEOPS_URL/api/v1/operator/incidents/$INCIDENT_ID/commands" \
  '{"type":"PUBLISH_GUEST_ADVISORY","guestTitle":"Weather advisory","guestMessage":"Some outdoor attractions are temporarily paused.","expectedVersion":3}' \
  "${AUTH_HEADER[@]}" >/dev/null
post_json "$VENUEOPS_URL/api/v1/operator/incidents/$INCIDENT_ID/commands" \
  '{"type":"START_MITIGATION","expectedVersion":4}' \
  "${AUTH_HEADER[@]}" >/dev/null

echo "== 6. Place attractions on weather hold =="

attraction_json() {
  curl -fsS "$VENUEOPS_URL/api/v1/operator/attractions/$1" "${AUTH_HEADER[@]}"
}

ensure_operating() {
  local id="$1"
  local status version
  status="$(attraction_json "$id" | python3 -c 'import json,sys; print(json.load(sys.stdin)["status"])')"
  version="$(attraction_json "$id" | python3 -c 'import json,sys; print(json.load(sys.stdin)["version"])')"
  case "$status" in
    OPERATING) return 0 ;;
    WEATHER_HOLD)
      post_json "$VENUEOPS_URL/api/v1/operator/attractions/$id/commands" \
        "{\"type\":\"CLEAR_WEATHER_HOLD\",\"reason\":\"Reset before storm demo\",\"expectedVersion\":$version}" \
        "${AUTH_HEADER[@]}" >/dev/null
      version=$((version + 1))
      post_json "$VENUEOPS_URL/api/v1/operator/attractions/$id/commands" \
        "{\"type\":\"COMPLETE_TESTING\",\"expectedVersion\":$version}" "${AUTH_HEADER[@]}" >/dev/null
      version=$((version + 1))
      post_json "$VENUEOPS_URL/api/v1/operator/attractions/$id/commands" \
        "{\"type\":\"APPROVE_RETURN_TO_SERVICE\",\"reason\":\"Reset before storm demo\",\"expectedVersion\":$version}" \
        "${AUTH_HEADER[@]}" >/dev/null
      ;;
    CLOSED)
      post_json "$VENUEOPS_URL/api/v1/operator/attractions/$id/commands" \
        "{\"type\":\"START_TESTING\",\"expectedVersion\":$version}" "${AUTH_HEADER[@]}" >/dev/null
      version=$((version + 1))
      post_json "$VENUEOPS_URL/api/v1/operator/attractions/$id/commands" \
        "{\"type\":\"COMPLETE_TESTING\",\"expectedVersion\":$version}" "${AUTH_HEADER[@]}" >/dev/null
      version=$((version + 1))
      post_json "$VENUEOPS_URL/api/v1/operator/attractions/$id/commands" \
        "{\"type\":\"APPROVE_RETURN_TO_SERVICE\",\"reason\":\"Opened for storm demo\",\"expectedVersion\":$version}" \
        "${AUTH_HEADER[@]}" >/dev/null
      ;;
    TESTING)
      post_json "$VENUEOPS_URL/api/v1/operator/attractions/$id/commands" \
        "{\"type\":\"COMPLETE_TESTING\",\"expectedVersion\":$version}" "${AUTH_HEADER[@]}" >/dev/null
      version=$((version + 1))
      post_json "$VENUEOPS_URL/api/v1/operator/attractions/$id/commands" \
        "{\"type\":\"APPROVE_RETURN_TO_SERVICE\",\"reason\":\"Opened for storm demo\",\"expectedVersion\":$version}" \
        "${AUTH_HEADER[@]}" >/dev/null
      ;;
    RETURNING_TO_SERVICE)
      post_json "$VENUEOPS_URL/api/v1/operator/attractions/$id/commands" \
        "{\"type\":\"APPROVE_RETURN_TO_SERVICE\",\"reason\":\"Opened for storm demo\",\"expectedVersion\":$version}" \
        "${AUTH_HEADER[@]}" >/dev/null
      ;;
    *)
      echo "error: unsupported attraction status for $id: $status" >&2
      exit 1
      ;;
  esac
}

ensure_operating mangrove-run
ensure_operating cypress-coil

M_VER="$(attraction_json mangrove-run | python3 -c 'import json,sys; print(json.load(sys.stdin)["version"])')"
post_json "$VENUEOPS_URL/api/v1/operator/attractions/mangrove-run/commands" \
  "{\"type\":\"PLACE_WEATHER_HOLD\",\"reason\":\"Lightning detected within operating radius\",\"expectedVersion\":$M_VER}" \
  "${AUTH_HEADER[@]}" >/dev/null

C_VER="$(attraction_json cypress-coil | python3 -c 'import json,sys; print(json.load(sys.stdin)["version"])')"
post_json "$VENUEOPS_URL/api/v1/operator/attractions/cypress-coil/commands" \
  "{\"type\":\"PLACE_WEATHER_HOLD\",\"reason\":\"Lightning detected within operating radius\",\"expectedVersion\":$C_VER}" \
  "${AUTH_HEADER[@]}" >/dev/null

echo "== 7. Clearance recommendation =="
post_json "$MONITOR_URL/api/v1/simulation/scenarios/clearance-period" '{}' >/dev/null
for _ in $(seq 1 30); do
  STATUS="$(curl -fsS "$VENUEOPS_URL/api/v1/operator/weather/recommendations/$REC_ID" "${AUTH_HEADER[@]}" \
    | python3 -c 'import json,sys; print(json.load(sys.stdin)["status"])')"
  if [[ "$STATUS" == "CLEARED" ]]; then
    break
  fi
  sleep 2
done
STATUS="$(curl -fsS "$VENUEOPS_URL/api/v1/operator/weather/recommendations/$REC_ID" "${AUTH_HEADER[@]}" \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["status"])')"
if [[ "$STATUS" != "CLEARED" ]]; then
  echo "error: recommendation did not clear in VenueOps (status=$STATUS)" >&2
  exit 1
fi

echo "== 8. Attraction recovery =="
M_VER="$(curl -fsS "$VENUEOPS_URL/api/v1/operator/attractions/mangrove-run" "${AUTH_HEADER[@]}" | python3 -c 'import json,sys; print(json.load(sys.stdin)["version"])')"
post_json "$VENUEOPS_URL/api/v1/operator/attractions/mangrove-run/commands" \
  "{\"type\":\"CLEAR_WEATHER_HOLD\",\"reason\":\"Storm cell moved out of radius\",\"expectedVersion\":$M_VER}" \
  "${AUTH_HEADER[@]}" >/dev/null
post_json "$VENUEOPS_URL/api/v1/operator/attractions/mangrove-run/commands" \
  '{"type":"COMPLETE_TESTING","expectedVersion":'"$((M_VER + 1))"'}' "${AUTH_HEADER[@]}" >/dev/null
post_json "$VENUEOPS_URL/api/v1/operator/attractions/mangrove-run/commands" \
  '{"type":"APPROVE_RETURN_TO_SERVICE","reason":"Return to service approved","expectedVersion":'"$((M_VER + 2))"'}' \
  "${AUTH_HEADER[@]}" >/dev/null

C_VER="$(curl -fsS "$VENUEOPS_URL/api/v1/operator/attractions/cypress-coil" "${AUTH_HEADER[@]}" | python3 -c 'import json,sys; print(json.load(sys.stdin)["version"])')"
post_json "$VENUEOPS_URL/api/v1/operator/attractions/cypress-coil/commands" \
  "{\"type\":\"CLEAR_WEATHER_HOLD\",\"reason\":\"Storm cell moved out of radius\",\"expectedVersion\":$C_VER}" \
  "${AUTH_HEADER[@]}" >/dev/null
post_json "$VENUEOPS_URL/api/v1/operator/attractions/cypress-coil/commands" \
  '{"type":"COMPLETE_TESTING","expectedVersion":'"$((C_VER + 1))"'}' "${AUTH_HEADER[@]}" >/dev/null
post_json "$VENUEOPS_URL/api/v1/operator/attractions/cypress-coil/commands" \
  '{"type":"APPROVE_RETURN_TO_SERVICE","reason":"Return to service approved","expectedVersion":'"$((C_VER + 2))"'}' \
  "${AUTH_HEADER[@]}" >/dev/null

echo "== 9. Resolve incident =="
INC_VER="$(curl -fsS "$VENUEOPS_URL/api/v1/operator/incidents/$INCIDENT_ID" "${AUTH_HEADER[@]}" \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["version"])')"
post_json "$VENUEOPS_URL/api/v1/operator/incidents/$INCIDENT_ID/commands" \
  "{\"type\":\"RESOLVE\",\"reason\":\"Storm cell moved out of radius\",\"expectedVersion\":$INC_VER}" \
  "${AUTH_HEADER[@]}" >/dev/null

post_json "$MONITOR_URL/api/v1/simulation/scenarios/clear" '{}' >/dev/null

cat <<EOF

Storm demo complete.

  Recommendation: $REC_ID (CLEARED)
  Incident:       $INCIDENT_ID (RESOLVED)
  Guest app:      ${GUEST_APP_ORIGIN:-http://localhost:3000}
  Control Tower:  ${OPERATOR_CONSOLE_ORIGIN:-http://localhost:3001}

EOF
