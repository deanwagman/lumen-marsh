#!/usr/bin/env bash
set -euo pipefail

# Proves the hosted operating story: /history reaches Control Tower, the Cutaway
# control opens it, a spectator can GET the cited answers, and replay stays
# lease-gated. Static checks always run. Live HTTP checks run when a gateway or
# VenueOps origin is already up.

PLATFORM_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PLATFORM_ROOT"

fail() {
  echo "FAIL  $*" >&2
  exit 1
}

caddy="demo/Caddyfile"
grep -q '/history\*' "$caddy" || fail "Caddy console matcher does not route /history*"
grep -q 'id="operating-story"' cutaway/public/index.html || fail "Cutaway is missing the Operating story control"
grep -q 'const towerHistoryUrl = "/tower/history"' cutaway/public/cutaway.js || fail "Operating story does not point at history"
grep -q 's|/tower/history|/history|g' demo/prepare-cutaway.sh || fail "Hosted Cutaway does not rewrite /tower/history to /history"
echo "PASS  /history is a console route and the Cutaway control opens it"

if [[ -n "${DEMO_ORIGIN:-}" ]]; then
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
  health="$(curl -ksS "${resolve[@]}" -o /dev/null -w '%{http_code}' --max-time 3 \
    "$DEMO_ORIGIN/gateway-health" || true)"
  if [[ "$health" == "200" ]]; then
    history_page="$(curl -ksS "${resolve[@]}" --max-time 10 "$DEMO_ORIGIN/history")"
    grep -q 'Lumen Marsh Control' <<<"$history_page" || fail "/history did not reach Control Tower"
    echo "PASS  /history reaches Control Tower"

    summary="$(curl -ksS "${resolve[@]}" --max-time 15 -G \
      "$DEMO_ORIGIN/api/v1/operator/analytics/availability" \
      --data-urlencode "datasetId=lumen-marsh-availability-demo-v1" \
      --data-urlencode "from=2026-10-13" \
      --data-urlencode "to=2026-10-15")"
    python3 -c '
import json, sys
data = json.loads(sys.argv[1])
assert [item["question"] for item in data["answers"]] == [
    "Which attraction had the worst uptime?",
    "What caused it?",
    "Which day?",
], data["answers"]
' "$summary"
    echo "PASS  spectator GET returns the three cited answers"

    replay_code="$(curl -ksS "${resolve[@]}" -o /dev/null -w '%{http_code}' --max-time 15 \
      -X POST "$DEMO_ORIGIN/api/v1/operator/analytics/datasets/lumen-marsh-availability-demo-v1/replay" || true)"
    [[ "$replay_code" == "403" ]] || fail "replay without a lease returned $replay_code"
    echo "PASS  replay remains lease-gated"
    exit 0
  fi
  echo "SKIP  hosted HTTP proof (gateway health returned ${health:-none})"
else
  echo "SKIP  hosted HTTP proof (DEMO_ORIGIN is not set)"
fi
