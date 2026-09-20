#!/usr/bin/env bash
set -euo pipefail
# Repeatable maintenance lifecycle acceptance: reliability ingest → accept →
# inspect → operations testing. Includes leak, stale-version, and unauthorized checks.
# Reliability Intelligence can also post ingest locally; this script posts
# the same payload the Java suite uses so the proof does not depend on the loop.
# shellcheck source=./_lib.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_lib.sh"

require_cmd curl
require_cmd python3
load_env

VENUEOPS_URL="${VENUEOPS_PUBLIC_ORIGIN:-http://localhost:8080}"
GUEST_URL="${GUEST_APP_ORIGIN:-http://localhost:3000}"
INTERNAL_MARKER="INTERNAL ONLY — maintenance-lifecycle-acceptance"
WORKDIR="$(mktemp -d "${TMPDIR:-/tmp}/lm-maintenance-lifecycle.XXXXXX")"
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

new_uuid() {
  python3 -c 'import uuid; print(uuid.uuid4())'
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
  RELIABILITY_TOKEN="${RELIABILITY_TOKEN:-local-reliability-token}"
else
  if [[ -z "${SUPERVISOR_TOKEN:-}" ]]; then
    cat <<'EOF' >&2
error: VenueOps is in OIDC mode and SUPERVISOR_TOKEN is not set.

Export a Cognito access token for a supervisors-group user (never paste tokens into chat):

  SUPERVISOR_TOKEN=... OPERATOR_LIMITED_TOKEN=... RELIABILITY_TOKEN=... \
    ./scripts/maintenance-lifecycle-acceptance.sh

OPERATOR_LIMITED_TOKEN must be an operators-group token without venueops/maintenance.inspect.
RELIABILITY_TOKEN must be a client-credentials token with only venueops/reliability.write.

LOCAL_JWT Compose remains the offline default:
  ./scripts/dev-up.sh && ./scripts/maintenance-lifecycle-acceptance.sh
EOF
    exit 1
  fi
  if [[ -z "${OPERATOR_LIMITED_TOKEN:-}" ]]; then
    echo "error: OIDC mode also requires OPERATOR_LIMITED_TOKEN for unauthorized-action checks" >&2
    exit 1
  fi
  if [[ -z "${RELIABILITY_TOKEN:-}" ]]; then
    echo "error: OIDC mode also requires RELIABILITY_TOKEN for reliability ingest" >&2
    exit 1
  fi
fi

SUPERVISOR_AUTH=( -H "Authorization: Bearer ${SUPERVISOR_TOKEN}" )
OPERATOR_AUTH=( -H "Authorization: Bearer ${OPERATOR_LIMITED_TOKEN}" )
RELIABILITY_AUTH=( -H "Authorization: Bearer ${RELIABILITY_TOKEN}" )

post_json() {
  local url="$1"
  local body="$2"
  shift 2
  curl -fsS -X POST "$url" -H 'Content-Type: application/json' "$@" -d "$body"
}

wo_version_from_cmd() {
  python3 -c 'import json,sys; print(json.load(sys.stdin)["workOrder"]["version"])'
}

attraction_json() {
  curl -fsS "$VENUEOPS_URL/api/v1/operator/attractions/$1" "${SUPERVISOR_AUTH[@]}"
}

wo_json() {
  curl -fsS "$VENUEOPS_URL/api/v1/operator/maintenance/work-orders/$1" "${SUPERVISOR_AUTH[@]}"
}

wo_cmd() {
  local work_order_id="$1"
  local body="$2"
  shift 2
  post_json "$VENUEOPS_URL/api/v1/operator/maintenance/work-orders/$work_order_id/commands" "$body" "$@"
}

wo_body() {
  local type="$1"
  local version="$2"
  shift 2
  TYPE="$type" VERSION="$version" python3 - "$@" <<'PY'
import json, os, sys, uuid
body = {
    "commandId": str(uuid.uuid4()),
    "type": os.environ["TYPE"],
    "expectedVersion": int(os.environ["VERSION"]),
}
args = sys.argv[1:]
i = 0
while i < len(args):
    if args[i] == "--command-id":
        body["commandId"] = args[i + 1]
        i += 2
        continue
    if args[i] == "--reason":
        body["reason"] = args[i + 1]
        i += 2
        continue
    if args[i] == "--data":
        body["data"] = json.loads(args[i + 1])
        i += 2
        continue
    raise SystemExit(f"unknown argument: {args[i]}")
print(json.dumps(body))
PY
}

assert_no_leak() {
  local path="$1"
  INTERNAL_MARKER="$INTERNAL_MARKER" python3 - "$path" <<'PY'
import os, sys
path = sys.argv[1]
raw = open(path).read()
marker = os.environ["INTERNAL_MARKER"]
assert marker not in raw, f"internal marker leaked in {path}"
assert "CC-TRAIN-01-WHEEL-A" not in raw, f"asset code leaked in {path}"
assert "LM-20" not in raw and '"LM-' not in raw, f"work-order number leaked in {path}"
assert "Lumen Marsh Fabrication" not in raw, f"fabrication source leaked in {path}"
for key in ("internalDescription", "assignedTo", "actorSubject", "actorDisplayName", "workOrderNumber", "assetCode"):
    assert f'"{key}"' not in raw, f"forbidden key leaked in {path}: {key}"
print("ok")
PY
}

ensure_closed() {
  local id="$1"
  local status version
  status="$(attraction_json "$id" | python3 -c 'import json,sys; print(json.load(sys.stdin)["status"])')"
  version="$(attraction_json "$id" | python3 -c 'import json,sys; print(json.load(sys.stdin)["version"])')"
  if [[ "$status" == "CLOSED" ]]; then
    return 0
  fi
  post_json "$VENUEOPS_URL/api/v1/operator/attractions/$id/commands" \
    "{\"type\":\"CLOSE_FOR_DAY\",\"reason\":\"Reset before maintenance lifecycle acceptance\",\"expectedVersion\":$version}" \
    "${SUPERVISOR_AUTH[@]}" >/dev/null
}

echo "Running maintenance lifecycle acceptance (auth: $AUTH_MODE)"

echo "== Failure: unauthenticated operator and ingest =="
UNAUTH_CODE="$(http_code "$WORKDIR/unauth.json" \
  "$VENUEOPS_URL/api/v1/operator/maintenance/work-orders")"
check "unauthenticated work-order list is 401" bash -c "[[ '$UNAUTH_CODE' == '401' ]]"

UNAUTH_INGEST="$(http_code "$WORKDIR/unauth-ingest.json" \
  -X POST "$VENUEOPS_URL/api/v1/integrations/reliability/recommendations" \
  -H 'Content-Type: application/json' \
  -d '{"observationId":"unauth","observedAt":"2026-09-20T00:00:00Z","assetCode":"CC-TRAIN-01-WHEEL-A","signalType":"VIBRATION","severity":"WARNING","value":1,"unit":"mm/s","evidence":"nope","recommendedAction":"nope"}')"
check "unauthenticated reliability ingest is 401" bash -c "[[ '$UNAUTH_INGEST' == '401' ]]"

MACHINE_CODE="$(http_code "$WORKDIR/machine-op.json" \
  "${RELIABILITY_AUTH[@]}" \
  "$VENUEOPS_URL/api/v1/operator/maintenance/assets")"
check "reliability token cannot list assets" bash -c "[[ '$MACHINE_CODE' == '403' ]]"

OBS_ID="vibration-cc-train-01-lifecycle-$(new_uuid)"
OBS_AT="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
INGEST_BODY="$(OBS_ID="$OBS_ID" OBS_AT="$OBS_AT" INTERNAL_MARKER="$INTERNAL_MARKER" python3 - <<'PY'
import json, os
print(json.dumps({
    "observationId": os.environ["OBS_ID"],
    "observedAt": os.environ["OBS_AT"],
    "assetCode": "CC-TRAIN-01-WHEEL-A",
    "signalType": "VIBRATION",
    "severity": "WARNING",
    "value": 8.4,
    "unit": "mm/s",
    "evidence": os.environ["INTERNAL_MARKER"] + " Fictional simulated vibration exceeded the demonstration threshold.",
    "recommendedAction": "Inspect the wheel assembly and consider reduced-capacity operation.",
}))
PY
)"

OP_INGEST="$(http_code "$WORKDIR/op-ingest.json" \
  -X POST "$VENUEOPS_URL/api/v1/integrations/reliability/recommendations" \
  -H 'Content-Type: application/json' \
  "${OPERATOR_AUTH[@]}" \
  -d "$INGEST_BODY")"
check "operator cannot ingest reliability" bash -c "[[ '$OP_INGEST' == '403' ]]"

echo "== 1. Reliability ingest (pending recommendation, not a work order) =="
INGEST="$(post_json "$VENUEOPS_URL/api/v1/integrations/reliability/recommendations" \
  "$INGEST_BODY" \
  "${RELIABILITY_AUTH[@]}")"
REC_ID="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["recommendationId"])' <<<"$INGEST")"
INGEST_STATUS="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["status"])' <<<"$INGEST")"
INGEST_DUP="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["duplicate"])' <<<"$INGEST")"
check "recommendation is pending review" bash -c "[[ '$INGEST_STATUS' == 'PENDING_REVIEW' ]]"
check "first ingest is not a duplicate" bash -c "[[ '$INGEST_DUP' == 'False' ]]"
check "recommendation id present" bash -c "[[ -n '$REC_ID' ]]"

DUP="$(post_json "$VENUEOPS_URL/api/v1/integrations/reliability/recommendations" \
  "$INGEST_BODY" \
  "${RELIABILITY_AUTH[@]}")"
DUP_FLAG="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["duplicate"])' <<<"$DUP")"
DUP_ID="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["recommendationId"])' <<<"$DUP")"
check "retried observationId is duplicate" bash -c "[[ '$DUP_FLAG' == 'True' ]]"
check "duplicate keeps the same recommendation" bash -c "[[ '$DUP_ID' == '$REC_ID' ]]"

REC="$(curl -fsS "$VENUEOPS_URL/api/v1/operator/maintenance/recommendations/$REC_ID" "${SUPERVISOR_AUTH[@]}")"
REC_VERSION="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["version"])' <<<"$REC")"
REC_WO="$(python3 -c 'import json,sys; print(json.load(sys.stdin).get("workOrderId") or "")' <<<"$REC")"
check "ingest did not open a work order" bash -c "[[ -z '$REC_WO' ]]"
echo "recommendation $REC_ID (version $REC_VERSION)"

echo "== 2. Accept recommendation =="
ACCEPT_ID="$(new_uuid)"
ACCEPT_BODY="$(wo_body ACCEPT "$REC_VERSION" --command-id "$ACCEPT_ID")"
ACCEPTED="$(post_json "$VENUEOPS_URL/api/v1/operator/maintenance/recommendations/$REC_ID/commands" \
  "$ACCEPT_BODY" \
  "${SUPERVISOR_AUTH[@]}" \
  -H 'X-Actor: spoofed-attacker')"
ACCEPT_STATUS="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["status"])' <<<"$ACCEPTED")"
WORK_ORDER_ID="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["workOrderId"])' <<<"$ACCEPTED")"
ACCEPT_VERSION="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["version"])' <<<"$ACCEPTED")"
check "accept created a work order" bash -c "[[ '$ACCEPT_STATUS' == 'WORK_ORDER_CREATED' && -n '$WORK_ORDER_ID' ]]"

REPLAY="$(post_json "$VENUEOPS_URL/api/v1/operator/maintenance/recommendations/$REC_ID/commands" \
  "$(wo_body ACCEPT 1 --command-id "$ACCEPT_ID")" \
  "${SUPERVISOR_AUTH[@]}")"
REPLAY_WO="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["workOrderId"])' <<<"$REPLAY")"
REPLAY_VER="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["version"])' <<<"$REPLAY")"
check "duplicate accept commandId replays" bash -c "[[ '$REPLAY_WO' == '$WORK_ORDER_ID' && '$REPLAY_VER' == '$ACCEPT_VERSION' ]]"

SECOND_ACCEPT="$(http_code "$WORKDIR/second-accept.json" \
  -X POST "$VENUEOPS_URL/api/v1/operator/maintenance/recommendations/$REC_ID/commands" \
  -H 'Content-Type: application/json' \
  "${SUPERVISOR_AUTH[@]}" \
  -d "$(wo_body ACCEPT 1)")"
SECOND_CODE="$(python3 -c 'import json,sys; print(json.load(sys.stdin).get("code",""))' < "$WORKDIR/second-accept.json")"
check "second accept intent is 409" bash -c "[[ '$SECOND_ACCEPT' == '409' && '$SECOND_CODE' == 'INVALID_TRANSITION' ]]"

echo "== 3. Incident link and work-order lifecycle =="
INCIDENT="$(post_json "$VENUEOPS_URL/api/v1/operator/incidents" \
  "$(INTERNAL_MARKER="$INTERNAL_MARKER" python3 - <<'PY'
import json, os
print(json.dumps({
    "title": "Cypress Coil vibration investigation",
    "type": "TECHNICAL",
    "severity": "MODERATE",
    "internalDescription": os.environ["INTERNAL_MARKER"] + " wheel vibration diagnosis",
    "attractionIds": ["cypress-coil"],
}))
PY
)" \
  "${SUPERVISOR_AUTH[@]}")"
INCIDENT_ID="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])' <<<"$INCIDENT")"
check "technical incident created" bash -c "[[ -n '$INCIDENT_ID' ]]"

wo_json "$WORK_ORDER_ID" > "$WORKDIR/wo.json"
WO_STATUS="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["status"])' < "$WORKDIR/wo.json")"
ASSET_CODE="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["asset"]["assetCode"])' < "$WORKDIR/wo.json")"
VER="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["version"])' < "$WORKDIR/wo.json")"
check "work order is draft on wheel A" bash -c "[[ '$WO_STATUS' == 'DRAFT' && '$ASSET_CODE' == 'CC-TRAIN-01-WHEEL-A' ]]"

START_BEFORE_OPEN="$(http_code "$WORKDIR/start-before-open.json" \
  -X POST "$VENUEOPS_URL/api/v1/operator/maintenance/work-orders/$WORK_ORDER_ID/commands" \
  -H 'Content-Type: application/json' \
  "${SUPERVISOR_AUTH[@]}" \
  -d "$(wo_body START_WORK "$VER")")"
START_BEFORE_CODE="$(python3 -c 'import json,sys; print(json.load(sys.stdin).get("code",""))' < "$WORKDIR/start-before-open.json")"
check "start work before open is 409" bash -c "[[ '$START_BEFORE_OPEN' == '409' && '$START_BEFORE_CODE' == 'INVALID_TRANSITION' ]]"

VER="$(wo_cmd "$WORK_ORDER_ID" "$(wo_body OPEN "$VER")" "${SUPERVISOR_AUTH[@]}" | wo_version_from_cmd)"

STALE_OPEN="$(http_code "$WORKDIR/stale-open.json" \
  -X POST "$VENUEOPS_URL/api/v1/operator/maintenance/work-orders/$WORK_ORDER_ID/commands" \
  -H 'Content-Type: application/json' \
  "${SUPERVISOR_AUTH[@]}" \
  -d "$(wo_body OPEN 0)")"
STALE_CODE="$(python3 -c 'import json,sys; print(json.load(sys.stdin).get("code",""))' < "$WORKDIR/stale-open.json")"
check "stale open is 409 STALE_VERSION" bash -c "[[ '$STALE_OPEN' == '409' && '$STALE_CODE' == 'STALE_VERSION' ]]"

VER="$(wo_cmd "$WORK_ORDER_ID" "$(wo_body LINK_INCIDENT "$VER" --reason "Linked to the operational incident." --data "{\"incidentId\":\"$INCIDENT_ID\"}")" "${SUPERVISOR_AUTH[@]}" | wo_version_from_cmd)"
VER="$(wo_cmd "$WORK_ORDER_ID" "$(wo_body ASSIGN "$VER" --data '{"teamId":"ride-maintenance-alpha"}')" "${SUPERVISOR_AUTH[@]}" | wo_version_from_cmd)"
VER="$(wo_cmd "$WORK_ORDER_ID" "$(wo_body START_WORK "$VER")" "${SUPERVISOR_AUTH[@]}" | wo_version_from_cmd)"
VER="$(wo_cmd "$WORK_ORDER_ID" "$(wo_body SET_ESTIMATED_RESTORE "$VER" --reason "Replacement and inspection work are progressing." --data '{"estimatedRestoreAt":"2099-01-01T00:00:00Z"}')" "${SUPERVISOR_AUTH[@]}" | wo_version_from_cmd)"

wo_json "$WORK_ORDER_ID" > "$WORKDIR/wo.json"
while IFS= read -r item_id; do
  [[ -z "$item_id" ]] && continue
  VER="$(wo_cmd "$WORK_ORDER_ID" "$(wo_body RECORD_CHECKLIST_RESULT "$VER" --reason "Physical inspection completed." --data "{\"checklistItemId\":\"$item_id\",\"result\":\"PASSED\",\"notes\":\"No visible damage; bearing replaced as a preventive measure.\"}")" "${SUPERVISOR_AUTH[@]}" | wo_version_from_cmd)"
done < <(python3 -c 'import json,sys; [print(i["id"]) for i in json.load(open(sys.argv[1]))["checklist"]]' "$WORKDIR/wo.json")

echo "== Failure: operator cannot inspect =="
OP_INSPECT="$(http_code "$WORKDIR/op-inspect.json" \
  -X POST "$VENUEOPS_URL/api/v1/operator/maintenance/work-orders/$WORK_ORDER_ID/commands" \
  -H 'Content-Type: application/json' \
  "${OPERATOR_AUTH[@]}" \
  -d "$(wo_body APPROVE_INSPECTION "$VER" --reason "Operators cannot approve inspection.")")"
check "operator approve inspection is 403" bash -c "[[ '$OP_INSPECT' == '403' ]]"

echo "== 4. Request and approve inspection =="
ATTR_STATUS_BEFORE="$(attraction_json cypress-coil | python3 -c 'import json,sys; print(json.load(sys.stdin)["status"])')"
VER="$(wo_cmd "$WORK_ORDER_ID" "$(wo_body REQUEST_INSPECTION "$VER")" "${SUPERVISOR_AUTH[@]}" | wo_version_from_cmd)"
APPROVED="$(wo_cmd "$WORK_ORDER_ID" "$(wo_body APPROVE_INSPECTION "$VER" --reason "Required inspection steps passed. Attraction is ready for operational testing.")" "${SUPERVISOR_AUTH[@]}")"
READY_STATUS="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["workOrder"]["status"])' <<<"$APPROVED")"
READY_VERSION="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["workOrder"]["version"])' <<<"$APPROVED")"
check "work order is ready for testing" bash -c "[[ '$READY_STATUS' == 'READY_FOR_TESTING' ]]"

wo_json "$WORK_ORDER_ID" > "$WORKDIR/wo.json"
REC_ACTION="$(python3 -c 'import json,sys; print((json.load(sys.stdin).get("recommendedAttractionAction") or {}).get("command",""))' < "$WORKDIR/wo.json")"
check "recommended attraction action is START_TESTING" bash -c "[[ '$REC_ACTION' == 'START_TESTING' ]]"

ATTR_STATUS_AFTER="$(attraction_json cypress-coil | python3 -c 'import json,sys; print(json.load(sys.stdin)["status"])')"
check "inspection did not change Cypress Coil status" bash -c "[[ '$ATTR_STATUS_AFTER' == '$ATTR_STATUS_BEFORE' ]]"

echo "== 5. Guest leak checks =="
curl -fsS "$VENUEOPS_URL/api/v1/attractions/cypress-coil" -o "$WORKDIR/guest-attraction.json"
curl -fsS "$VENUEOPS_URL/api/v1/advisories" -o "$WORKDIR/advisories.json"
check "guest attraction omits maintenance internals" assert_no_leak "$WORKDIR/guest-attraction.json"
check "guest advisories omit maintenance internals" assert_no_leak "$WORKDIR/advisories.json"
check "guest REST omits internal marker" bash -c "! grep -q 'INTERNAL ONLY' '$WORKDIR/guest-attraction.json' '$WORKDIR/advisories.json'"

check "guest SSE omits maintenance internals" bash -c "
  python3 - '$VENUEOPS_URL/api/v1/events' '$WORKDIR/guest-sse.txt' '$INTERNAL_MARKER' <<'PY'
import sys, urllib.request
url, out, marker = sys.argv[1], sys.argv[2], sys.argv[3]
req = urllib.request.Request(url, headers={'Accept': 'text/event-stream'})
with urllib.request.urlopen(req, timeout=8) as resp:
    chunk = resp.read(8192).decode('utf-8', errors='replace')
open(out, 'w').write(chunk)
assert 'maintenance.work-order' not in chunk, chunk[:400]
assert marker not in chunk, 'internal marker leaked on guest SSE'
assert 'CC-TRAIN-01-WHEEL-A' not in chunk, 'asset code leaked on guest SSE'
assert '\"internalDescription\"' not in chunk, 'internalDescription leaked on guest SSE'
assert '\"actorSubject\"' not in chunk, 'actorSubject leaked on guest SSE'
print('ok')
PY
"

check "authenticated operator SSE connects" bash -c "
  python3 - '$VENUEOPS_URL/api/v1/operator/events' '$SUPERVISOR_TOKEN' '$WORKDIR/op-sse.txt' <<'PY'
import sys, urllib.request
url, token, out = sys.argv[1], sys.argv[2], sys.argv[3]
req = urllib.request.Request(url, headers={'Accept': 'text/event-stream', 'Authorization': f'Bearer {token}'})
with urllib.request.urlopen(req, timeout=8) as resp:
    chunk = resp.read(2048).decode('utf-8', errors='replace')
open(out, 'w').write(chunk)
assert 'event:' in chunk or 'data:' in chunk or ':' in chunk, chunk[:200]
print('ok')
PY
"

echo "== 6. Operations testing; complete is blocked while testing =="
ensure_closed cypress-coil
C_VER="$(attraction_json cypress-coil | python3 -c 'import json,sys; print(json.load(sys.stdin)["version"])')"
post_json "$VENUEOPS_URL/api/v1/operator/attractions/cypress-coil/commands" \
  "{\"type\":\"START_TESTING\",\"expectedVersion\":$C_VER}" \
  "${SUPERVISOR_AUTH[@]}" >/dev/null
C_STATUS="$(attraction_json cypress-coil | python3 -c 'import json,sys; print(json.load(sys.stdin)["status"])')"
check "cypress-coil is testing" bash -c "[[ '$C_STATUS' == 'TESTING' ]]"

COMPLETE_WHILE_TESTING="$(http_code "$WORKDIR/complete-testing.json" \
  -X POST "$VENUEOPS_URL/api/v1/operator/maintenance/work-orders/$WORK_ORDER_ID/commands" \
  -H 'Content-Type: application/json' \
  "${SUPERVISOR_AUTH[@]}" \
  -d "$(wo_body COMPLETE "$READY_VERSION" --reason "Attraction testing and supervisor approval are complete.")")"
COMPLETE_CODE="$(python3 -c 'import json,sys; print(json.load(sys.stdin).get("code",""))' < "$WORKDIR/complete-testing.json")"
check "complete during testing is 422 MAINTENANCE_PREREQUISITE" bash -c "[[ '$COMPLETE_WHILE_TESTING' == '422' && '$COMPLETE_CODE' == 'MAINTENANCE_PREREQUISITE' ]]"

C_VER="$(attraction_json cypress-coil | python3 -c 'import json,sys; print(json.load(sys.stdin)["version"])')"
post_json "$VENUEOPS_URL/api/v1/operator/attractions/cypress-coil/commands" \
  "{\"type\":\"COMPLETE_TESTING\",\"expectedVersion\":$C_VER}" \
  "${SUPERVISOR_AUTH[@]}" >/dev/null
C_VER="$(attraction_json cypress-coil | python3 -c 'import json,sys; print(json.load(sys.stdin)["version"])')"
post_json "$VENUEOPS_URL/api/v1/operator/attractions/cypress-coil/commands" \
  "{\"type\":\"APPROVE_RETURN_TO_SERVICE\",\"reason\":\"Return to service after maintenance testing\",\"expectedVersion\":$C_VER}" \
  "${SUPERVISOR_AUTH[@]}" >/dev/null
C_STATUS="$(attraction_json cypress-coil | python3 -c 'import json,sys; print(json.load(sys.stdin)["status"])')"
check "cypress-coil returned to operating" bash -c "[[ '$C_STATUS' == 'OPERATING' ]]"

wo_cmd "$WORK_ORDER_ID" "$(wo_body COMPLETE "$READY_VERSION" --reason "Attraction testing and supervisor approval are complete.")" \
  "${SUPERVISOR_AUTH[@]}" >/dev/null
FINAL_STATUS="$(wo_json "$WORK_ORDER_ID" | python3 -c 'import json,sys; print(json.load(sys.stdin)["status"])')"
FINAL_ACTION="$(wo_json "$WORK_ORDER_ID" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("recommendedAttractionAction"))')"
check "work order completed" bash -c "[[ '$FINAL_STATUS' == 'COMPLETED' ]]"
check "completed work order has no recommended action" bash -c "[[ '$FINAL_ACTION' == 'None' ]]"

echo "== 7. Audit ignores forged X-Actor =="
curl -fsS "$VENUEOPS_URL/api/v1/operator/maintenance/work-orders/$WORK_ORDER_ID/activity" \
  "${SUPERVISOR_AUTH[@]}" -o "$WORKDIR/wo-activity.json"
check "work-order audit ignores forged X-Actor" bash -c "
  python3 - '$WORKDIR/wo-activity.json' <<'PY'
import json, sys
events = json.load(open(sys.argv[1]))
assert events, 'empty activity'
assert events[0]['eventType'] == 'WORK_ORDER_CREATED'
for event in events:
    assert event.get('actorSubject') not in {'spoofed-attacker', 'spoofed-supervisor'}
    assert event.get('actorDisplayName') not in {'spoofed-attacker', 'spoofed-supervisor'}
    assert event.get('actorSubject')
print('ok')
PY
"

echo "== Failure: API restart keeps maintenance state =="
if [[ "${SKIP_SERVICE_RESTART:-0}" == "1" ]]; then
  echo "SKIP  API process restart (SKIP_SERVICE_RESTART=1)"
elif docker compose -p lumen-marsh ps --status running --format '{{.Name}}' 2>/dev/null | grep -q venueops-api; then
  docker compose -p lumen-marsh restart venueops-api >/dev/null
  wait_http "$VENUEOPS_URL/actuator/health" "venueops-api after restart" 60
  AFTER="$(wo_json "$WORK_ORDER_ID" | python3 -c 'import json,sys; print(json.load(sys.stdin)["status"])')"
  check "work order survives API restart" bash -c "[[ '$AFTER' == 'COMPLETED' ]]"
else
  echo "SKIP  API process restart (compose project lumen-marsh is not running)"
fi

echo
echo "Maintenance lifecycle acceptance: $pass passed, $fail failed"
echo "  Recommendation: $REC_ID"
echo "  Work order:     $WORK_ORDER_ID"
echo "  Incident:       $INCIDENT_ID"
echo "  Guest app:      $GUEST_URL"
if [[ "$fail" -gt 0 ]]; then
  exit 1
fi
