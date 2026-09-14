#!/usr/bin/env bash
set -euo pipefail
# Guest advisory delivery proof: publish → sanitize guest list → withdraw → gone.
# Contract: docs/guest-advisory-contract.md
# shellcheck source=./_lib.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_lib.sh"

require_cmd curl
require_cmd python3
load_env
./scripts/wait-for-ready.sh

VENUEOPS_URL="${VENUEOPS_PUBLIC_ORIGIN:-http://localhost:8080}"
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

FORBIDDEN_KEYS='internalDescription assignedTo actor activity activities reason'

assert_guest_advisory_sanitized() {
  local json_file="$1"
  local expected_title="$2"
  FORBIDDEN_KEYS="$FORBIDDEN_KEYS" EXPECTED_TITLE="$expected_title" python3 - "$json_file" <<'PY'
import json, os, sys
path = sys.argv[1]
data = json.load(open(path))
assert isinstance(data, list), data
title = os.environ["EXPECTED_TITLE"]
match = next((item for item in data if item.get("title") == title), None)
assert match is not None, f"missing advisory titled {title!r} in {data!r}"
allowed = {"id", "severity", "title", "message", "affectedAttractionIds", "updatedAt", "version"}
extra = set(match) - allowed
assert not extra, f"unexpected guest fields: {extra}"
forbidden = os.environ["FORBIDDEN_KEYS"].split()
blob = json.dumps(match)
for key in forbidden:
    assert key not in match, f"forbidden key present: {key}"
    assert f'"{key}"' not in blob, f"forbidden key leaked in JSON: {key}"
print("ok")
PY
}

assert_advisory_absent() {
  local json_file="$1"
  local expected_title="$2"
  EXPECTED_TITLE="$expected_title" python3 - "$json_file" <<'PY'
import json, os, sys
data = json.load(open(sys.argv[1]))
title = os.environ["EXPECTED_TITLE"]
assert all(item.get("title") != title for item in data), data
print("ok")
PY
}

echo "Running guest advisory delivery demo"

INCIDENT="$(curl -fsS -X POST "$VENUEOPS_URL/api/v1/operator/incidents" \
  -H 'Content-Type: application/json' "${AUTH_HEADER[@]}" \
  -d '{"title":"Advisory demo incident","type":"WEATHER","severity":"MAJOR","internalDescription":"INTERNAL ONLY — must never reach guests","attractionIds":["mangrove-run"]}')"
INCIDENT_ID="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])' <<<"$INCIDENT")"
check "incident created" bash -c "[[ -n '$INCIDENT_ID' ]]"

ACK="$(curl -fsS -o /dev/null -w '%{http_code}' \
  -X POST "$VENUEOPS_URL/api/v1/operator/incidents/$INCIDENT_ID/commands" \
  -H 'Content-Type: application/json' "${AUTH_HEADER[@]}" \
  -d '{"type":"ACKNOWLEDGE","expectedVersion":1}')"
check "acknowledge" bash -c "[[ '$ACK' == '200' ]]"

PUB="$(curl -fsS -o /tmp/lm-advisory-pub.json -w '%{http_code}' \
  -X POST "$VENUEOPS_URL/api/v1/operator/incidents/$INCIDENT_ID/commands" \
  -H 'Content-Type: application/json' "${AUTH_HEADER[@]}" \
  -d '{"type":"PUBLISH_GUEST_ADVISORY","guestTitle":"Advisory demo notice","guestMessage":"Temporary guest-safe pause for demo.","expectedVersion":2}')"
check "publish guest advisory" bash -c "[[ '$PUB' == '200' ]]"

curl -fsS "$VENUEOPS_URL/api/v1/advisories" -o /tmp/lm-advisories-published.json
check "guest list is sanitized" assert_guest_advisory_sanitized /tmp/lm-advisories-published.json "Advisory demo notice"
check "guest list omits internal description text" bash -c \
  "! grep -q 'INTERNAL ONLY' /tmp/lm-advisories-published.json"

VERSION="$(python3 -c 'import json; print(json.load(open("/tmp/lm-advisory-pub.json"))["version"])')"
WD="$(curl -fsS -o /tmp/lm-advisory-wd.json -w '%{http_code}' \
  -X POST "$VENUEOPS_URL/api/v1/operator/incidents/$INCIDENT_ID/commands" \
  -H 'Content-Type: application/json' "${AUTH_HEADER[@]}" \
  -d "{\"type\":\"WITHDRAW_GUEST_ADVISORY\",\"reason\":\"Demo complete\",\"expectedVersion\":$VERSION}")"
check "withdraw guest advisory" bash -c "[[ '$WD' == '200' ]]"

curl -fsS "$VENUEOPS_URL/api/v1/advisories" -o /tmp/lm-advisories-withdrawn.json
check "guest list no longer contains advisory" assert_advisory_absent /tmp/lm-advisories-withdrawn.json "Advisory demo notice"

echo
echo "Advisory demo summary: $pass passed, $fail failed"
if [[ "$fail" -gt 0 ]]; then
  exit 1
fi
