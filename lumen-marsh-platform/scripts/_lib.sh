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

new_uuid() {
  python3 -c 'import uuid; print(uuid.uuid4())'
}

envelope_command() {
  python3 -c '
import json, sys, uuid
body = json.loads(sys.argv[1])
data_keys = (
    "waitMinutes",
    "assignee",
    "severity",
    "attractionId",
    "guestTitle",
    "guestMessage",
    "confirmActiveWorkOrders",
)
body.setdefault("commandId", str(uuid.uuid4()))
data = dict(body.get("data") or {})
for key in data_keys:
    if key in body:
        data[key] = body.pop(key)
if data:
    body["data"] = data
print(json.dumps(body))
' "$1"
}

# Write the opening guest SSE burst to a file.
# HTTPResponse.read(N) blocks until N bytes arrive or the peer closes. The park
# stream stays open after attractions.snapshot, advisories.snapshot, and
# guest.flow.updated, and that burst is smaller than 8192 bytes, so a fixed
# large read times out before sanitization checks run. read1 stops once those
# opening events have arrived.
read_sse_burst() {
  local url="$1"
  local outfile="$2"
  shift 2
  python3 - "$url" "$outfile" "$@" <<'PY'
import socket
import sys
import time
import urllib.request
from http.client import IncompleteRead

url, outfile = sys.argv[1], sys.argv[2]
headers = {"Accept": "text/event-stream"}
args = sys.argv[3:]
index = 0
while index < len(args):
    if args[index] == "--header":
        name, value = args[index + 1].split(":", 1)
        headers[name.strip()] = value.strip()
        index += 2
        continue
    raise SystemExit(f"unknown argument: {args[index]}")

OPENING = (
    b"attractions.snapshot",
    b"advisories.snapshot",
    b"guest.flow.updated",
)


def response_socket(resp):
    raw = getattr(resp.fp, "raw", None)
    sock = getattr(raw, "_sock", None) if raw is not None else None
    if sock is None:
        sock = getattr(resp.fp, "_sock", None)
    if sock is None:
        raise RuntimeError("SSE response has no socket")
    return sock


def opening_complete(buf: bytes) -> bool:
    if any(name not in buf for name in OPENING):
        return False
    last = max(buf.rfind(name) for name in OPENING)
    return b"\n\n" in buf[last:]


def read_burst(resp, overall_s=8.0, max_bytes=262144) -> bytes:
    # read1 returns whatever the current chunk already has. Stop once the
    # opening events are complete so an open stream cannot stall the read.
    # A timeout leaves chunked framing mid-header, so do not retry after one.
    sock = response_socket(resp)
    parts = []
    size = 0
    deadline = time.monotonic() + overall_s
    while size < max_bytes and time.monotonic() < deadline:
        remaining = deadline - time.monotonic()
        sock.settimeout(max(remaining, 0.1))
        try:
            piece = resp.read1(4096)
        except IncompleteRead:
            break
        except (TimeoutError, socket.timeout):
            break
        except OSError as exc:
            if "timed out" not in str(exc):
                raise
            break
        if not piece:
            break
        parts.append(piece)
        size += len(piece)
        if opening_complete(b"".join(parts)):
            break
    return b"".join(parts)


req = urllib.request.Request(url, headers=headers)
with urllib.request.urlopen(req, timeout=8) as resp:
    payload = read_burst(resp)
open(outfile, "w", encoding="utf-8").write(payload.decode("utf-8", errors="replace"))
missing = [name.decode() for name in OPENING if name not in payload]
if missing:
    raise SystemExit("guest SSE burst missing opening events: " + ", ".join(missing))
PY
}
export -f read_sse_burst

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
  Reliability Intelligence ${RELIABILITY_INTELLIGENCE_PUBLIC_ORIGIN:-http://localhost:8200}

  Health:
    curl ${VENUEOPS_PUBLIC_ORIGIN:-http://localhost:8080}/actuator/health
    curl ${ENVIRONMENTAL_MONITOR_PUBLIC_ORIGIN:-http://localhost:8000}/health/ready
    curl ${PARK_FLOW_INTELLIGENCE_PUBLIC_ORIGIN:-http://localhost:8100}/health/ready
    curl ${RELIABILITY_INTELLIGENCE_PUBLIC_ORIGIN:-http://localhost:8200}/health/ready
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
  Reliability Intelligence ${RELIABILITY_INTELLIGENCE_PUBLIC_ORIGIN:-http://localhost:8200}

  Health:
    curl ${VENUEOPS_PUBLIC_ORIGIN:-http://localhost:8080}/actuator/health
    curl ${ENVIRONMENTAL_MONITOR_PUBLIC_ORIGIN:-http://localhost:8000}/health/ready
    curl ${PARK_FLOW_INTELLIGENCE_PUBLIC_ORIGIN:-http://localhost:8100}/health/ready
    curl ${RELIABILITY_INTELLIGENCE_PUBLIC_ORIGIN:-http://localhost:8200}/health/ready

EOF
}
