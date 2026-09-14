# Secret rotation

Treat Cognito and database secrets as rotatable. Do not put replacement values in Git.

## What to rotate

| Secret | Storage | Typical lifetime | How |
|---|---|---|---|
| Environmental Monitor app-client secret | Secrets Manager (`…/environmental-monitor/oidc`) | 90 days, or immediately after suspected exposure | Create a new Cognito app client or rotate the client secret in Cognito, write the new JSON to Secrets Manager, restart Monitor, then retire the old secret |
| Operator / supervisor passwords | Cognito user pool (admin-created users) | On join/leave and after exposure | Reset via Cognito admin APIs; never store passwords in variables or Compose files |
| VenueOps Postgres password | Environment / SSM in demo | 90 days | Update the parameter, roll the API, then update the database role |
| Environmental Monitor Postgres password | Environment / SSM in demo | 90 days | Same as VenueOps database password |
| LOCAL_JWT opaque tokens | Compose `.env` only | Local development | Never used with `VENUEOPS_SECURITY_MODE=OIDC`; change if a shared laptop image leaks `.env` |

Access tokens themselves are short-lived (60 minutes) and are not rotated as stored secrets. Revoke refresh tokens / reset the user session if a console token is leaked.

## Rules

1. Rotate by writing the new value first, deploying consumers, then disabling the old value.
2. After rotation, confirm Monitor can ingest recommendations and console login still works.
3. Record the rotation in the deployment notes; do not paste secret values into tickets or logs.
4. OpenTofu state may contain the Monitor client secret — treat state as sensitive and restrict backend access.

## After suspected token leak

1. Revoke the Cognito user session or rotate the machine client secret.
2. Confirm the leaked token returns `401`.
3. Check activity history for the token `sub` during the exposure window.
