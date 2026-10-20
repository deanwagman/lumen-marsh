#!/usr/bin/env bash
set -euo pipefail
# Repeatable Historical Operations Intelligence acceptance: seeded fixture →
# curated availability and citations → idempotent replay → live-state isolation.
# The reset guard is exercised without --confirm; this script never resets volumes.
# shellcheck source=./_lib.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_lib.sh"

require_cmd curl
require_cmd docker
require_cmd python3
require_cmd cmp
load_env

VENUEOPS_URL="${VENUEOPS_PUBLIC_ORIGIN:-http://localhost:8080}"
DATASET_ID="lumen-marsh-availability-demo-v1"
FROM_DATE="2026-10-13"
TO_DATE="2026-10-15"
DETAIL_DATE="2026-10-15"
WORKDIR="$(mktemp -d "${TMPDIR:-/tmp}/lm-historical-operations.XXXXXX")"
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
    "$VENUEOPS_URL/api/v1/operator/analytics/datasets")"
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

  SUPERVISOR_TOKEN=... OPERATOR_LIMITED_TOKEN=... \
    ./scripts/historical-operations-acceptance.sh

OPERATOR_LIMITED_TOKEN must be an operators-group token without the supervisor role.
Both tokens require venueops/operator.read.

LOCAL_JWT Compose remains the offline default:
  ./scripts/dev-up.sh && ./scripts/historical-operations-acceptance.sh
EOF
    exit 1
  fi
  if [[ -z "${OPERATOR_LIMITED_TOKEN:-}" ]]; then
    echo "error: OIDC mode also requires OPERATOR_LIMITED_TOKEN for unauthorized replay checks" >&2
    exit 1
  fi
fi

SUPERVISOR_AUTH=( -H "Authorization: Bearer ${SUPERVISOR_TOKEN}" )
OPERATOR_AUTH=( -H "Authorization: Bearer ${OPERATOR_LIMITED_TOKEN}" )

db_json_snapshot() {
  compose exec -T venueops-db psql \
    -v ON_ERROR_STOP=1 \
    -U "${VENUEOPS_DB_USER:-venueops}" \
    -d "${VENUEOPS_DB_NAME:-venueops}" \
    -Atc "
      select jsonb_build_object(
        'liveAttractionRows', (select count(*) from attraction),
        'liveAttractionVersions', (select coalesce(sum(version), 0) from attraction),
        'liveActivityRows', (select count(*) from attraction_activity),
        'liveActivityIds', (
          select coalesce(jsonb_agg(id order by id), '[]'::jsonb)
          from attraction_activity
        ),
        'historicalDatasetRows', (
          select count(*) from historical_dataset where id = '$DATASET_ID'
        ),
        'historicalCalendarRows', (
          select count(*) from historical_operating_calendar where dataset_id = '$DATASET_ID'
        ),
        'historicalPlanRows', (
          select count(*) from historical_attraction_plan where dataset_id = '$DATASET_ID'
        ),
        'historicalSnapshotRows', (
          select count(*) from historical_attraction_snapshot where dataset_id = '$DATASET_ID'
        ),
        'historicalSnapshotVersionTotal', (
          select coalesce(sum(aggregate_version), 0)
          from historical_attraction_snapshot where dataset_id = '$DATASET_ID'
        ),
        'historicalActivityRows', (
          select count(*) from historical_attraction_activity where dataset_id = '$DATASET_ID'
        ),
        'historicalActivityVersionTotal', (
          select coalesce(sum(resulting_version), 0)
          from historical_attraction_activity where dataset_id = '$DATASET_ID'
        ),
        'historicalActivityIds', (
          select coalesce(jsonb_agg(activity_id order by activity_id), '[]'::jsonb)
          from historical_attraction_activity where dataset_id = '$DATASET_ID'
        ),
        'historicalCommandRows', (
          select count(*) from historical_processed_command where dataset_id = '$DATASET_ID'
        ),
        'historicalCommandIds', (
          select coalesce(jsonb_agg(command_id order by command_id), '[]'::jsonb)
          from historical_processed_command where dataset_id = '$DATASET_ID'
        )
      );
    "
}

capture_live_api() {
  local target="$1"
  local prefix="$2"
  curl -fsS "$VENUEOPS_URL/api/v1/attractions" -o "$WORKDIR/$prefix-attractions.json"
  python3 - "$WORKDIR/$prefix-attractions.json" "$WORKDIR/$prefix-ids.txt" <<'PY'
import json, sys
items = json.load(open(sys.argv[1]))
with open(sys.argv[2], "w") as output:
    for item in sorted(items, key=lambda value: value["id"]):
        output.write(item["id"] + "\n")
PY
  while IFS= read -r attraction_id; do
    curl -fsS "$VENUEOPS_URL/api/v1/operator/attractions/$attraction_id" \
      "${SUPERVISOR_AUTH[@]}" -o "$WORKDIR/$prefix-state-$attraction_id.json"
    curl -fsS "$VENUEOPS_URL/api/v1/operator/attractions/$attraction_id/activity" \
      "${SUPERVISOR_AUTH[@]}" -o "$WORKDIR/$prefix-activity-$attraction_id.json"
  done < "$WORKDIR/$prefix-ids.txt"
  python3 - "$WORKDIR" "$prefix" "$target" <<'PY'
import json, pathlib, sys
root, prefix, target = pathlib.Path(sys.argv[1]), sys.argv[2], pathlib.Path(sys.argv[3])
attractions = json.load(open(root / f"{prefix}-attractions.json"))
states = {}
activity = {}
for item in attractions:
    attraction_id = item["id"]
    states[attraction_id] = json.load(open(root / f"{prefix}-state-{attraction_id}.json"))
    activity[attraction_id] = json.load(open(root / f"{prefix}-activity-{attraction_id}.json"))
target.write_text(json.dumps({
    "guestAttractions": sorted(attractions, key=lambda value: value["id"]),
    "operatorState": states,
    "operatorActivity": activity,
}, sort_keys=True, separators=(",", ":")))
PY
}

get_summary() {
  local target="$1"
  curl -fsS -G "$VENUEOPS_URL/api/v1/operator/analytics/availability" \
    "${SUPERVISOR_AUTH[@]}" \
    --data-urlencode "datasetId=$DATASET_ID" \
    --data-urlencode "from=$FROM_DATE" \
    --data-urlencode "to=$TO_DATE" \
    -o "$target"
}

get_detail() {
  local target="$1"
  curl -fsS -G "$VENUEOPS_URL/api/v1/operator/analytics/availability/attractions/cypress-coil" \
    "${SUPERVISOR_AUTH[@]}" \
    --data-urlencode "datasetId=$DATASET_ID" \
    --data-urlencode "date=$DETAIL_DATE" \
    -o "$target"
}

ask_question() {
  local target="$1"
  curl -fsS -X POST "$VENUEOPS_URL/api/v1/operator/analytics/questions" \
    -H 'Content-Type: application/json' \
    "${SUPERVISOR_AUTH[@]}" \
    -d "{
      \"datasetId\":\"$DATASET_ID\",
      \"from\":\"$FROM_DATE\",
      \"to\":\"$TO_DATE\",
      \"question\":\"Which attraction had the worst uptime?\"
    }" \
    -o "$target"
}

replay_fixture() {
  local target="$1"
  curl -fsS -X POST \
    "$VENUEOPS_URL/api/v1/operator/analytics/datasets/$DATASET_ID/replay?replace=false" \
    "${SUPERVISOR_AUTH[@]}" \
    -o "$target"
}

echo "Running Historical Operations Intelligence acceptance (auth: $AUTH_MODE)"

echo "== Failure: analytics and replay authorization =="
UNAUTH_READ="$(http_code "$WORKDIR/unauth-read.json" \
  "$VENUEOPS_URL/api/v1/operator/analytics/datasets")"
check "unauthenticated dataset read is 401" bash -c "[[ '$UNAUTH_READ' == '401' ]]"

OPERATOR_READ="$(http_code "$WORKDIR/operator-read.json" \
  "${OPERATOR_AUTH[@]}" \
  "$VENUEOPS_URL/api/v1/operator/analytics/datasets")"
check "operator can read historical analytics" bash -c "[[ '$OPERATOR_READ' == '200' ]]"

OPERATOR_REPLAY="$(http_code "$WORKDIR/operator-replay.json" \
  -X POST \
  "${OPERATOR_AUTH[@]}" \
  "$VENUEOPS_URL/api/v1/operator/analytics/datasets/$DATASET_ID/replay?replace=false")"
check "operator without supervisor role cannot replay" bash -c "[[ '$OPERATOR_REPLAY' == '403' ]]"

echo "== 1. Capture live and historical state =="
capture_live_api "$WORKDIR/live-api-before.json" before
db_json_snapshot > "$WORKDIR/db-before.json"
check "live attraction API is seeded" bash -c "
  python3 - '$WORKDIR/live-api-before.json' <<'PY'
import json, sys
data = json.load(open(sys.argv[1]))
assert len(data['guestAttractions']) >= 3, data
assert 'cypress-coil' in data['operatorState'], data['operatorState']
assert all(isinstance(events, list) for events in data['operatorActivity'].values())
print('ok')
PY
"
check "database snapshot includes live and historical identities" bash -c "
  python3 - '$WORKDIR/db-before.json' <<'PY'
import json, sys
data = json.load(open(sys.argv[1]))
assert data['liveAttractionRows'] >= 3, data
assert data['historicalDatasetRows'] == 1, data
assert data['historicalActivityRows'] > 0, data
assert len(data['historicalActivityIds']) == data['historicalActivityRows'], data
assert len(data['historicalCommandIds']) == data['historicalCommandRows'], data
print('ok')
PY
"

echo "== 2. Seeded dataset and curated ranking =="
curl -fsS "$VENUEOPS_URL/api/v1/operator/analytics/datasets" \
  "${SUPERVISOR_AUTH[@]}" -o "$WORKDIR/datasets.json"
check "fixture dataset is completed and seeded" bash -c "
  python3 - '$WORKDIR/datasets.json' '$DATASET_ID' <<'PY'
import json, sys
items, dataset_id = json.load(open(sys.argv[1])), sys.argv[2]
item = next(value for value in items if value['id'] == dataset_id)
assert item['version'] == '1', item
assert item['seed'] == 20261020, item
assert item['state'] == 'COMPLETED', item
assert item['fromDate'] == '2026-10-13' and item['toDate'] == '2026-10-15', item
assert len(item['manifestChecksum']) == 64, item
print('ok')
PY
"

get_summary "$WORKDIR/summary-before.json"
check "Cypress Coil is ranked worst with expected totals" bash -c "
  python3 - '$WORKDIR/summary-before.json' <<'PY'
import json, sys
data = json.load(open(sys.argv[1]))
assert data['datasetId'] == 'lumen-marsh-availability-demo-v1', data
assert data['zoneId'] == 'America/New_York', data
assert len(data['attractions']) == 3, data
worst = data['attractions'][0]
assert worst['attractionId'] == 'cypress-coil', data['attractions']
assert worst['scheduledMinutes'] == 1440, worst
assert worst['operatingMinutes'] == 1110, worst
assert float(worst['uptimePercentage']) == 77.08, worst
assert worst['longestOutageMinutes'] == 190, worst
assert all(
    float(item['uptimePercentage']) >= float(worst['uptimePercentage'])
    for item in data['attractions'][1:]
), data['attractions']
print('ok')
PY
"

echo "== 3. Stable detail segments and deterministic question citations =="
get_detail "$WORKDIR/detail-1.json"
get_detail "$WORKDIR/detail-2.json"
check "cited detail is byte-stable" cmp -s "$WORKDIR/detail-1.json" "$WORKDIR/detail-2.json"
check "detail segments carry stable, resolvable citations" bash -c "
  python3 - '$WORKDIR/detail-1.json' <<'PY'
import json, sys
data = json.load(open(sys.argv[1]))
assert data['attractionId'] == 'cypress-coil', data
assert float(data['uptimePercentage']) == 60.42, data
assert data['downtimeMinutes']['TECHNICAL_DELAY'] == 150, data
segments = data['segments']
assert len(segments) >= 4, segments
ids = [segment['citationId'] for segment in segments]
assert len(ids) == len(set(ids)), ids
for segment in segments:
    citation_id = segment['citationId']
    assert citation_id.startswith('segment-'), segment
    assert segment['datasetId'] == data['datasetId'], segment
    assert 'attractionId=cypress-coil' in segment['controlTowerUrl'], segment
    assert 'citation=' + citation_id in segment['controlTowerUrl'], segment
assert any(segment.get('activityId') and segment.get('commandId') for segment in segments), segments
print('ok')
PY
"

ask_question "$WORKDIR/question-1.json"
ask_question "$WORKDIR/question-2.json"
check "question answer and citations are deterministic" cmp -s "$WORKDIR/question-1.json" "$WORKDIR/question-2.json"
check "question cites curated Cypress Coil segments" bash -c "
  python3 - '$WORKDIR/question-1.json' '$WORKDIR/summary-before.json' <<'PY'
import json, sys
answer = json.load(open(sys.argv[1]))
summary = json.load(open(sys.argv[2]))
assert answer['provider'] == 'deterministic-curated-availability', answer
assert 'Cypress Coil had the worst uptime at 77.08%' in answer['answer'], answer
citations = answer['claims'][0]['citations']
assert citations, answer
expected = summary['attractions'][0]['citations']
assert citations == expected, (citations, expected)
for citation in citations:
    assert citation['id'].startswith('segment-'), citation
    assert citation['attractionId'] == 'cypress-coil', citation
    assert 'citation=' + citation['id'] in citation['controlTowerUrl'], citation
print('ok')
PY
"

echo "== 4. Fixture replay is idempotent =="
replay_fixture "$WORKDIR/replay-1.json"
replay_fixture "$WORKDIR/replay-2.json"
check "first replay reports unchanged" bash -c "
  python3 - '$WORKDIR/replay-1.json' '$DATASET_ID' <<'PY'
import json, sys
data = json.load(open(sys.argv[1]))
assert data['datasetId'] == sys.argv[2], data
assert data['changed'] is False, data
assert len(data['manifestChecksum']) == 64, data
print('ok')
PY
"
check "second replay reports the same unchanged fixture" cmp -s "$WORKDIR/replay-1.json" "$WORKDIR/replay-2.json"

db_json_snapshot > "$WORKDIR/db-after.json"
capture_live_api "$WORKDIR/live-api-after.json" after
get_summary "$WORKDIR/summary-after.json"
get_detail "$WORKDIR/detail-after.json"
ask_question "$WORKDIR/question-after.json"

check "historical rows, totals, and IDs are unchanged" cmp -s "$WORKDIR/db-before.json" "$WORKDIR/db-after.json"
check "historical availability totals are unchanged" cmp -s "$WORKDIR/summary-before.json" "$WORKDIR/summary-after.json"
check "historical detail citations are unchanged" cmp -s "$WORKDIR/detail-1.json" "$WORKDIR/detail-after.json"
check "question citations remain unchanged" cmp -s "$WORKDIR/question-1.json" "$WORKDIR/question-after.json"
check "live attraction state and activity APIs are unchanged" \
  cmp -s "$WORKDIR/live-api-before.json" "$WORKDIR/live-api-after.json"

echo "== 5. Destructive reset remains explicitly guarded =="
if ./scripts/reset-demo.sh >"$WORKDIR/reset-stdout.txt" 2>"$WORKDIR/reset-stderr.txt"; then
  check "reset without --confirm is refused" false
else
  check "reset without --confirm is refused" \
    grep -q "refusing to delete demo volumes without confirmation" "$WORKDIR/reset-stderr.txt"
fi

echo
echo "Historical Operations Intelligence acceptance: $pass passed, $fail failed"
echo "  Dataset: $DATASET_ID"
echo "  Dates:   $FROM_DATE through $TO_DATE"
echo "  Auth:    $AUTH_MODE"
if [[ "$fail" -gt 0 ]]; then
  exit 1
fi
