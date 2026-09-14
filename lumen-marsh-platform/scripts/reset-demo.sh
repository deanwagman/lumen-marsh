#!/usr/bin/env bash
set -euo pipefail
# shellcheck source=./_lib.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_lib.sh"

require_cmd docker

CONFIRM=0
AUTH_MODE=local
for arg in "$@"; do
  case "$arg" in
    --confirm) CONFIRM=1 ;;
    --oidc) AUTH_MODE=oidc ;;
    -h|--help)
      cat <<'EOF'
Usage: ./scripts/reset-demo.sh --confirm [--oidc]

Stops the stack, deletes demo database volumes, and recreates a clean seed.

  --confirm   Required. Refuses to delete volumes without this flag.
  --oidc      Recreate with the Cognito-backed Compose overlay.
EOF
      exit 0
      ;;
    *)
      echo "error: unknown argument: $arg" >&2
      exit 1
      ;;
  esac
done

if [[ "$CONFIRM" -ne 1 ]]; then
  cat <<'EOF' >&2
error: refusing to delete demo volumes without confirmation.

This stops the stack and removes only:
  lumen-marsh_venueops-data
  lumen-marsh_environmental-data

Re-run:
  ./scripts/reset-demo.sh --confirm
  ./scripts/reset-demo.sh --confirm --oidc
EOF
  exit 1
fi

load_env
require_cmd curl

echo "Stopping stack and removing demo database volumes"
compose down
docker volume rm -f lumen-marsh_venueops-data lumen-marsh_environmental-data >/dev/null 2>&1 || true

echo "Rebuilding and starting a clean demo environment"
if [[ "$AUTH_MODE" == "oidc" ]]; then
  ./scripts/dev-up.sh --oidc
else
  ./scripts/dev-up.sh
fi

VENUEOPS_URL="${VENUEOPS_PUBLIC_ORIGIN:-http://localhost:8080}"
ATTRACTIONS="$(curl -fsS "$VENUEOPS_URL/api/v1/attractions")"
COUNT="$(python3 -c 'import json,sys; print(len(json.load(sys.stdin)))' <<<"$ATTRACTIONS")"
if [[ "$COUNT" -lt 1 ]]; then
  echo "error: expected seeded attractions after reset; got $COUNT" >&2
  exit 1
fi
echo "Seed check OK ($COUNT attractions)"
print_urls "$AUTH_MODE"
