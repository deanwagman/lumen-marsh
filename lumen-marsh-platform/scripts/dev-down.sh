#!/usr/bin/env bash
set -euo pipefail
# shellcheck source=./_lib.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_lib.sh"

require_cmd docker
load_env

echo "Stopping Lumen Marsh stack (volumes preserved)"
compose down
echo "Done. Demo data volumes were not deleted."
echo "To wipe demo databases: ./scripts/reset-demo.sh --confirm"
