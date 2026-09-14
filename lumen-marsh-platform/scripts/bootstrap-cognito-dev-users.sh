#!/usr/bin/env bash
set -euo pipefail
# Create one development operator and one supervisor in the demo user pool.
# Passwords come from the environment — never from Git.

# shellcheck source=./_lib.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_lib.sh"

require_cmd aws
require_cmd tofu

: "${DEV_OPERATOR_EMAIL:?set DEV_OPERATOR_EMAIL}"
: "${DEV_SUPERVISOR_EMAIL:?set DEV_SUPERVISOR_EMAIL}"
: "${DEV_OPERATOR_TEMPORARY_PASSWORD:?set DEV_OPERATOR_TEMPORARY_PASSWORD}"
: "${DEV_SUPERVISOR_TEMPORARY_PASSWORD:?set DEV_SUPERVISOR_TEMPORARY_PASSWORD}"

DEMO_DIR="$PLATFORM_ROOT/infrastructure/environments/demo"
POOL_ID="${COGNITO_USER_POOL_ID:-}"
if [[ -z "$POOL_ID" ]]; then
  POOL_ID="$(cd "$DEMO_DIR" && tofu output -raw COGNITO_USER_POOL_ID)"
fi
if [[ -z "$POOL_ID" || "$POOL_ID" == "null" ]]; then
  echo "error: COGNITO_USER_POOL_ID is empty — apply identity first" >&2
  exit 1
fi
export AWS_DEFAULT_REGION="${AWS_DEFAULT_REGION:-$(cd "$DEMO_DIR" && tofu output -raw aws_region)}"

create_user() {
  local email="$1"
  local password="$2"
  local group="$3"
  aws cognito-idp admin-create-user \
    --user-pool-id "$POOL_ID" \
    --username "$email" \
    --user-attributes "Name=email,Value=${email}" "Name=email_verified,Value=true" \
    --temporary-password "$password" \
    --message-action SUPPRESS \
    >/dev/null
  aws cognito-idp admin-add-user-to-group \
    --user-pool-id "$POOL_ID" \
    --username "$email" \
    --group-name "$group" \
    >/dev/null
  if [[ "${DEV_COGNITO_PERMANENT_PASSWORD:-false}" == "true" ]]; then
    aws cognito-idp admin-set-user-password \
      --user-pool-id "$POOL_ID" \
      --username "$email" \
      --password "$password" \
      --permanent \
      >/dev/null
  fi
  echo "created $group user $email"
}

create_user "$DEV_OPERATOR_EMAIL" "$DEV_OPERATOR_TEMPORARY_PASSWORD" operators
create_user "$DEV_SUPERVISOR_EMAIL" "$DEV_SUPERVISOR_TEMPORARY_PASSWORD" supervisors

echo "Development users are in the pool. Temporary passwords are not printed."
if [[ "${DEV_COGNITO_PERMANENT_PASSWORD:-false}" != "true" ]]; then
  echo "First sign-in will require a password change unless DEV_COGNITO_PERMANENT_PASSWORD=true."
fi
