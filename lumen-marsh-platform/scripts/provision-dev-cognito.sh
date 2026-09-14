#!/usr/bin/env bash
set -euo pipefail
# Apply development Cognito (pool, groups, scopes, console client) then create the operator.
# User passwords are created by the bootstrap script, never stored in OpenTofu state.

# shellcheck source=./_lib.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_lib.sh"

require_cmd tofu

IDENTITY_DIR="$PLATFORM_ROOT/infrastructure/environments/dev-identity"
cd "$IDENTITY_DIR"
tofu init -input=false
tofu apply -input=false -auto-approve

echo
echo "Cognito development pool applied."
tofu output
echo
"$PLATFORM_ROOT/scripts/create-dev-operator.sh"
echo
echo "Next:"
echo "  $PLATFORM_ROOT/scripts/print-cognito-local-env.sh"
echo "  python3 $PLATFORM_ROOT/scripts/verify-cognito-console-login.py"
