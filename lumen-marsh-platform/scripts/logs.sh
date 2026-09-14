#!/usr/bin/env bash
set -euo pipefail
# shellcheck source=./_lib.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_lib.sh"

require_cmd docker
load_env

SERVICE="${1:-}"
if [[ -n "$SERVICE" ]]; then
  compose logs -f --tail=200 "$SERVICE"
else
  compose logs -f --tail=100
fi
