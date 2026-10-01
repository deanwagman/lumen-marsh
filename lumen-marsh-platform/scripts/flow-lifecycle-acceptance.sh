#!/usr/bin/env bash
set -euo pipefail
# Repeatable park-flow lifecycle acceptance: disruption → recommendation →
# supervisor publish → guest Best Next. Includes leak, stale-version, unauthorized,
# commandId replay, and optional API restart checks.
# shellcheck source=./_lib.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_lib.sh"

require_cmd curl
require_cmd python3
load_env

VENUEOPS_URL="${VENUEOPS_PUBLIC_ORIGIN:-http://localhost:8080}"
FLOW_URL="${PARK_FLOW_INTELLIGENCE_PUBLIC_ORIGIN:-http://localhost:8100}"
GUEST_URL="${GUEST_APP_ORIGIN:-http://localhost:3000}"
INTERNAL_MARKER="INTERNAL ONLY — flow-lifecycle-acceptance"
GUEST_MESSAGE="Cypress Coil and Stormglass Station currently have shorter waits."
FORBIDDEN_KEYS='explanation confidence actor queueLength relatedIncidentId'
WORKDIR="$(mktemp -d "${TMPDIR:-/tmp}/lm-flow-lifecycle.XXXXXX")"
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
  FLOW_TOKEN="${FLOW_TOKEN:-local-flow-token}"
else
  if [[ -z "${SUPERVISOR_TOKEN:-}" ]]; then
    cat <<'EOF' >&2
error: VenueOps is in OIDC mode and SUPERVISOR_TOKEN is not set.

Export a Cognito access token for a supervisors-group user (never paste tokens into chat):

  SUPERVISOR_TOKEN=... OPERATOR_LIMITED_TOKEN=... FLOW_TOKEN=... \
    ./scripts/flow-lifecycle-acceptance.sh

OPERATOR_LIMITED_TOKEN must be an operators-group token without venueops/flow.publish.
FLOW_TOKEN must be a client-credentials token with only venueops/flow-ingest.write.

LOCAL_JWT Compose remains the offline default:
  ./scripts/dev-up.sh && ./scripts/flow-lifecycle-acceptance.sh
EOF
    exit 1
  fi
  if [[ -z "${OPERATOR_LIMITED_TOKEN:-}" ]]; then
    echo "error: OIDC mode also requires OPERATOR_LIMITED_TOKEN for unauthorized-action checks" >&2
    exit 1
  fi
  if [[ -z "${FLOW_TOKEN:-}" ]]; then
    echo "error: OIDC mode also requires FLOW_TOKEN for machine-identity checks" >&2
    exit 1
  fi
fi

SUPERVISOR_AUTH=( -H "Authorization: Bearer ${SUPERVISOR_TOKEN}" )
OPERATOR_AUTH=( -H "Authorization: Bearer ${OPERATOR_LIMITED_TOKEN}" )
FLOW_AUTH=( -H "Authorization: Bearer ${FLOW_TOKEN}" )

post_json() {
  local url="$1"
  local body="$2"
  shift 2
  curl -fsS -X POST "$url" -H 'Content-Type: application/json' "$@" -d "$body"
}

flow_command() {
  local recommendation_id="$1"
  local body="$2"
  shift 2
  post_json "$VENUEOPS_URL/api/v1/operator/flow/recommendations/$recommendation_id/commands" \
    "$(envelope_command "$body")" "$@"
}

assert_guest_flow_sanitized() {
  local json_file="$1"
  FORBIDDEN_KEYS="$FORBIDDEN_KEYS" INTERNAL_MARKER="$INTERNAL_MARKER" python3 - "$json_file" <<'PY'
import json, os, sys

path = sys.argv[1]
raw = open(path).read()
data = json.loads(raw)
marker = os.environ["INTERNAL_MARKER"]
forbidden = os.environ["FORBIDDEN_KEYS"].split()
assert marker not in raw, f"internal marker leaked in {path}"
assert "INTERNAL" not in raw, f"INTERNAL leaked in {path}"

def walk(value, prefix="$"):
    if isinstance(value, dict):
        for key, child in value.items():
            assert key not in forbidden, f"forbidden key {key} at {prefix}"
            walk(child, f"{prefix}.{key}")
    elif isinstance(value, list):
        for index, child in enumerate(value):
            walk(child, f"{prefix}[{index}]")
    elif isinstance(value, str):
        assert marker not in value, f"internal marker leaked at {prefix}"

walk(data)
print("ok")
PY
}

echo "Running flow lifecycle acceptance (auth: $AUTH_MODE)"

echo "== Failure: unauthenticated operator flow =="
UNAUTH_CODE="$(http_code "$WORKDIR/unauth.json" \
  "$VENUEOPS_URL/api/v1/operator/flow/recommendations")"
check "unauthenticated flow list is 401" bash -c "[[ '$UNAUTH_CODE' == '401' ]]"

UNAUTH_CMD="$(http_code "$WORKDIR/unauth-cmd.json" \
  -X POST "$VENUEOPS_URL/api/v1/operator/flow/recommendations/00000000-0000-4000-8000-000000000000/commands" \
  -H 'Content-Type: application/json' \
  -d "$(envelope_command '{"type":"APPROVE","expectedVersion":1}')")"
check "unauthenticated flow command is 401" bash -c "[[ '$UNAUTH_CMD' == '401' ]]"

MACHINE_CODE="$(http_code "$WORKDIR/machine-op.json" \
  "${FLOW_AUTH[@]}" \
  "$VENUEOPS_URL/api/v1/operator/flow/overview")"
check "flow token cannot read operator flow" bash -c "[[ '$MACHINE_CODE' == '403' ]]"

echo "== 1. Simulated mangrove disruption =="
post_json "$FLOW_URL/simulation/scenarios/mangrove-disruption" '{}' >/dev/null

REC_JSON=""
REC_ID=""
REC_VERSION=""
for _ in $(seq 1 30); do
  curl -fsS "$VENUEOPS_URL/api/v1/operator/flow/recommendations" "${SUPERVISOR_AUTH[@]}" \
    -o "$WORKDIR/recs.json"
  REC_ID="$(python3 -c '
import json,sys
items = json.load(open(sys.argv[1])).get("items") or []
pending = [i for i in items if (i.get("recommendation") or {}).get("status") == "PENDING_REVIEW"]
print(pending[0]["recommendationId"] if pending else "")
' "$WORKDIR/recs.json")"
  if [[ -n "$REC_ID" ]]; then
    REC_JSON="$(cat "$WORKDIR/recs.json")"
    REC_VERSION="$(python3 -c '
import json,sys
items = json.load(open(sys.argv[1])).get("items") or []
rec = next(i for i in items if i["recommendationId"] == sys.argv[2])
print(rec["recommendation"]["version"])
' "$WORKDIR/recs.json" "$REC_ID")"
    break
  fi
  sleep 2
done
check "unpublished flow recommendation arrived" bash -c "[[ -n '$REC_ID' ]]"
if [[ -z "$REC_ID" ]]; then
  echo "hint: mangrove-disruption reuses a stable recommendation id; run ./scripts/reset-demo.sh --confirm on a spent database" >&2
  echo
  echo "Flow lifecycle acceptance: $pass passed, $fail failed"
  exit 1
fi
echo "recommendation $REC_ID (version $REC_VERSION)"

curl -fsS "$VENUEOPS_URL/api/v1/operator/dashboard" "${SUPERVISOR_AUTH[@]}" \
  -o "$WORKDIR/dashboard-pending.json"
check "dashboard lists unpublished flow attention" bash -c "
  python3 - '$WORKDIR/dashboard-pending.json' '$REC_ID' <<'PY'
import json, sys
data = json.load(open(sys.argv[1]))
rec_id = sys.argv[2]
kinds = [item.get('kind') for item in data.get('needsAttention') or []]
assert 'UNPUBLISHED_FLOW_RECOMMENDATION' in kinds, kinds
assert (data.get('summary') or {}).get('unpublishedFlowRecommendations', 0) >= 1
assert any(item.get('subjectId') == rec_id for item in data.get('needsAttention') or []), data.get('needsAttention')
print('ok')
PY
"

curl -fsS "$VENUEOPS_URL/api/v1/flow/recommendations" -o "$WORKDIR/guest-recs-before.json"
check "guest recommendations empty before publish" bash -c "
  python3 - '$WORKDIR/guest-recs-before.json' <<'PY'
import json, sys
data = json.load(open(sys.argv[1]))
assert data == [], data
print('ok')
PY
"

echo "== Failure: stale version =="
STALE_CODE="$(http_code "$WORKDIR/stale.json" \
  -X POST "$VENUEOPS_URL/api/v1/operator/flow/recommendations/$REC_ID/commands" \
  -H 'Content-Type: application/json' \
  "${SUPERVISOR_AUTH[@]}" \
  -d "$(envelope_command "{\"type\":\"DISMISS\",\"reason\":\"stale\",\"expectedVersion\":99}")")"
STALE_BODY_CODE="$(python3 -c 'import json,sys; print(json.load(sys.stdin).get("code",""))' < "$WORKDIR/stale.json")"
check "stale dismiss is 409 STALE_VERSION" bash -c "[[ '$STALE_CODE' == '409' && '$STALE_BODY_CODE' == 'STALE_VERSION' ]]"

echo "== 2. Operator approves (commandId replay) =="
APPROVE_ID="$(new_uuid)"
APPROVED="$(flow_command "$REC_ID" \
  "{\"commandId\":\"$APPROVE_ID\",\"type\":\"APPROVE\",\"expectedVersion\":$REC_VERSION}" \
  "${OPERATOR_AUTH[@]}" \
  -H 'X-Actor: spoofed-attacker')"
APPROVE_STATUS="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["recommendation"]["status"])' <<<"$APPROVED")"
APPROVE_VERSION="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["recommendation"]["version"])' <<<"$APPROVED")"
check "operator approve is APPROVED" bash -c "[[ '$APPROVE_STATUS' == 'APPROVED' ]]"

REPLAY="$(flow_command "$REC_ID" \
  "{\"commandId\":\"$APPROVE_ID\",\"type\":\"APPROVE\",\"expectedVersion\":1}" \
  "${OPERATOR_AUTH[@]}")"
REPLAY_STATUS="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["recommendation"]["status"])' <<<"$REPLAY")"
REPLAY_VERSION="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["recommendation"]["version"])' <<<"$REPLAY")"
check "duplicate approve commandId replays" bash -c "[[ '$REPLAY_STATUS' == 'APPROVED' && '$REPLAY_VERSION' == '$APPROVE_VERSION' ]]"

echo "== Failure: operator cannot publish =="
OP_PUB_CODE="$(http_code "$WORKDIR/op-pub.json" \
  -X POST "$VENUEOPS_URL/api/v1/operator/flow/recommendations/$REC_ID/commands" \
  -H 'Content-Type: application/json' \
  "${OPERATOR_AUTH[@]}" \
  -d "$(envelope_command "{\"type\":\"PUBLISH\",\"expectedVersion\":$APPROVE_VERSION,\"guestMessage\":\"Should be forbidden.\"}")")"
check "operator publish is 403" bash -c "[[ '$OP_PUB_CODE' == '403' ]]"

echo "== 3. Supervisor publishes guest guidance =="
PUBLISHED="$(flow_command "$REC_ID" \
  "{\"type\":\"PUBLISH\",\"expectedVersion\":$APPROVE_VERSION,\"reason\":\"$INTERNAL_MARKER supervisor publish\",\"guestMessage\":\"$GUEST_MESSAGE\"}" \
  "${SUPERVISOR_AUTH[@]}")"
PUBLISH_STATUS="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["recommendation"]["status"])' <<<"$PUBLISHED")"
check "supervisor publish is PUBLISHED" bash -c "[[ '$PUBLISH_STATUS' == 'PUBLISHED' ]]"

curl -fsS "$VENUEOPS_URL/api/v1/flow/overview" -o "$WORKDIR/guest-overview.json"
curl -fsS "$VENUEOPS_URL/api/v1/flow/recommendations" -o "$WORKDIR/guest-recs.json"
curl -fsS "$VENUEOPS_URL/api/v1/attractions/cypress-coil/wait-forecast" -o "$WORKDIR/guest-wait.json"
curl -fsS "$VENUEOPS_URL/api/v1/attractions/mangrove-run/wait-forecast" -o "$WORKDIR/guest-wait-mangrove.json"

check "guest overview is sanitized" assert_guest_flow_sanitized "$WORKDIR/guest-overview.json"
check "guest recommendation list is sanitized" assert_guest_flow_sanitized "$WORKDIR/guest-recs.json"
check "guest wait-forecast omits internals" assert_guest_flow_sanitized "$WORKDIR/guest-wait.json"
check "guest mangrove wait-forecast omits internals" assert_guest_flow_sanitized "$WORKDIR/guest-wait-mangrove.json"
check "guest Best Next carries published message" bash -c "
  GUEST_MESSAGE='$GUEST_MESSAGE' python3 - '$WORKDIR/guest-overview.json' '$WORKDIR/guest-recs.json' <<'PY'
import json, os, sys
overview = json.load(open(sys.argv[1]))
recs = json.load(open(sys.argv[2]))
message = os.environ['GUEST_MESSAGE']
guidance = overview.get('publishedGuidance') or []
assert guidance, overview
assert any(item.get('guestMessage') == message for item in guidance), guidance
assert any(item.get('guestMessage') == message for item in recs), recs
print('ok')
PY
"

echo "== 4. Guest SSE receives guidance without operator fields =="
check "guest SSE snapshot is sanitized" bash -c "
  python3 - '$VENUEOPS_URL/api/v1/events' '$WORKDIR/guest-sse.txt' '$FORBIDDEN_KEYS' '$INTERNAL_MARKER' <<'PY'
import sys, urllib.request
url, out, forbidden, marker = sys.argv[1], sys.argv[2], sys.argv[3].split(), sys.argv[4]
req = urllib.request.Request(url, headers={'Accept': 'text/event-stream'})
with urllib.request.urlopen(req, timeout=8) as resp:
    chunk = resp.read(8192).decode('utf-8', errors='replace')
open(out, 'w').write(chunk)
assert 'guest.flow' in chunk or 'flow.' in chunk or 'event:' in chunk, chunk[:400]
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

echo "== 5. Audit ignores forged X-Actor =="
curl -fsS "$VENUEOPS_URL/api/v1/operator/flow/recommendations/$REC_ID/activity" "${SUPERVISOR_AUTH[@]}" \
  -o "$WORKDIR/flow-activity.json"
check "flow audit ignores forged X-Actor" bash -c "
  python3 - '$WORKDIR/flow-activity.json' <<'PY'
import json, sys
events = json.load(open(sys.argv[1]))
assert events
for event in events:
    actor = event.get('actor') or event.get('actorDisplayName') or ''
    assert 'spoofed-attacker' not in str(event), event
    assert actor != 'spoofed-attacker', event
print('ok')
PY
"

echo "== Failure: API restart keeps published guidance =="
if [[ "${SKIP_SERVICE_RESTART:-0}" == "1" ]]; then
  echo "SKIP  API process restart (SKIP_SERVICE_RESTART=1)"
elif docker compose -p lumen-marsh ps --status running --format '{{.Name}}' 2>/dev/null | grep -q venueops-api; then
  docker compose -p lumen-marsh restart venueops-api >/dev/null
  wait_http "$VENUEOPS_URL/actuator/health" "venueops-api after restart" 60
  AFTER="$(curl -fsS "$VENUEOPS_URL/api/v1/operator/flow/recommendations/$REC_ID" "${SUPERVISOR_AUTH[@]}" \
    | python3 -c 'import json,sys; print(json.load(sys.stdin)["recommendation"]["status"])')"
  check "recommendation survives API restart" bash -c "[[ '$AFTER' == 'PUBLISHED' ]]"
  curl -fsS "$VENUEOPS_URL/api/v1/flow/overview" -o "$WORKDIR/guest-overview-restart.json"
  check "guest guidance stays published after restart" bash -c "
    GUEST_MESSAGE='$GUEST_MESSAGE' python3 - '$WORKDIR/guest-overview-restart.json' <<'PY'
import json, os, sys
data = json.load(open(sys.argv[1]))
message = os.environ['GUEST_MESSAGE']
guidance = data.get('publishedGuidance') or []
assert any(item.get('guestMessage') == message for item in guidance), data
print('ok')
PY
  "
else
  echo "SKIP  API process restart (compose project lumen-marsh is not running)"
fi

post_json "$FLOW_URL/simulation/scenarios/clear" '{}' >/dev/null

echo
echo "Flow lifecycle acceptance: $pass passed, $fail failed"
echo "  Recommendation: $REC_ID"
echo "  Guest app:      $GUEST_URL"
if [[ "$fail" -gt 0 ]]; then
  exit 1
fi
