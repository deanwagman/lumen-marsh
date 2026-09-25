# Development Cognito

Apply this environment to create the `lumen-marsh-dev` user pool without the rest of the AWS demo stack.

```bash
cd infrastructure/environments/dev-identity
tofu init
tofu apply
```

Outputs (non-secret):

- `COGNITO_ISSUER_URI`
- `COGNITO_DOMAIN`
- `COGNITO_USER_POOL_ID`
- `CONSOLE_CLIENT_ID`
- `MONITOR_SECRET_ARN`
- `FLOW_SECRET_ARN`

OpenTofu creates the pool, groups, scopes, the public console client, and — when `enable_machine_client` is true — confidential client-credentials apps for Environmental Monitor, Park Flow Intelligence, and Reliability Intelligence, each with a Secrets Manager secret. Local Compose runs Reliability Intelligence against that client. OpenTofu does not create users or store human passwords.

Then create the operator (password stays in gitignored `.env.cognito.local`):

```bash
./scripts/create-dev-operator.sh
./scripts/verify-cognito-console-login.py
```

The hosted-UI domain prefix includes the AWS account suffix because Cognito domains are globally unique. The user pool name remains `lumen-marsh-dev`.

`./scripts/dev-up.sh --oidc` reads machine secrets through the configured AWS CLI profile and starts the full Cognito-backed stack.
