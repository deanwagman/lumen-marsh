#!/usr/bin/env bash
set -euo pipefail
# Create the development supervisor in the lumen-marsh-dev user pool.
# Password is written to gitignored .env.cognito.local — never printed.

# shellcheck source=./_lib.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_lib.sh"

require_cmd aws
require_cmd tofu
require_cmd python3

IDENTITY_DIR="$PLATFORM_ROOT/infrastructure/environments/dev-identity"
ENV_FILE="$PLATFORM_ROOT/.env.cognito.local"
EMAIL="${DEV_SUPERVISOR_EMAIL:-supervisor@lumen-marsh.dev}"

cd "$IDENTITY_DIR"
POOL_ID="$(tofu output -raw COGNITO_USER_POOL_ID)"
REGION="$(tofu output -raw aws_region)"
ISSUER="$(tofu output -raw COGNITO_ISSUER_URI)"
DOMAIN="$(tofu output -raw COGNITO_DOMAIN)"
CLIENT_ID="$(tofu output -raw CONSOLE_CLIENT_ID)"
export AWS_DEFAULT_REGION="${AWS_DEFAULT_REGION:-$REGION}"

if [[ -z "$POOL_ID" || "$POOL_ID" == "null" ]]; then
  echo "error: apply infrastructure/environments/dev-identity first" >&2
  exit 1
fi

PASSWORD="${DEV_SUPERVISOR_PASSWORD:-}"
if [[ -z "$PASSWORD" ]]; then
  PASSWORD="$(python3 - <<'PY'
import secrets
import string
alphabet = string.ascii_letters + string.digits
body = "".join(secrets.choice(alphabet) for _ in range(16))
print(f"Sv-{body}#1")
PY
)"
fi

if aws cognito-idp admin-get-user --user-pool-id "$POOL_ID" --username "$EMAIL" >/dev/null 2>&1; then
  echo "supervisor already exists: $EMAIL"
else
  aws cognito-idp admin-create-user \
    --user-pool-id "$POOL_ID" \
    --username "$EMAIL" \
    --user-attributes "Name=email,Value=${EMAIL}" "Name=email_verified,Value=true" \
    --temporary-password "$PASSWORD" \
    --message-action SUPPRESS \
    >/dev/null
  echo "created supervisor $EMAIL"
fi

aws cognito-idp admin-add-user-to-group \
  --user-pool-id "$POOL_ID" \
  --username "$EMAIL" \
  --group-name supervisors \
  >/dev/null

aws cognito-idp admin-set-user-password \
  --user-pool-id "$POOL_ID" \
  --username "$EMAIL" \
  --password "$PASSWORD" \
  --permanent \
  >/dev/null

umask 077
merge_env_file "$ENV_FILE" \
  "COGNITO_ISSUER_URI=$ISSUER" \
  "COGNITO_DOMAIN=$DOMAIN" \
  "COGNITO_USER_POOL_ID=$POOL_ID" \
  "CONSOLE_CLIENT_ID=$CLIENT_ID" \
  "DEV_SUPERVISOR_EMAIL=$EMAIL" \
  "DEV_SUPERVISOR_PASSWORD=$PASSWORD"

echo "supervisor is in group supervisors"
echo "credentials written to .env.cognito.local (gitignored)"
