# Security policy

## Reporting a vulnerability

Please do not open a public issue containing vulnerability details, credentials, tokens, or personal data. Use GitHub's private vulnerability reporting feature for this repository. If private reporting is unavailable, open a minimal issue asking the maintainer to enable a private communication channel without including exploit details.

## Supported version

Only the current `main` branch is maintained. This portfolio project does not currently publish supported production releases.

## Development modes

`LOCAL_JWT` is an offline convenience mode. Its fixed bearer values are deliberately recognizable and its RSA signing key is generated when the API starts. Never expose this mode to an untrusted network or deploy it publicly.

OIDC mode validates Cognito access tokens, audiences/client IDs, scopes, and groups. Using OIDC does not by itself make the system production-ready; complete the [public deployment gate](./lumen-marsh-platform/docs/security/public-deployment-gate.md) first.

## Repository hygiene

Never commit:

- `.env` or `.env.*` files other than documented examples and the non-secret test fixture
- AWS access keys or local AWS credential files
- Cognito client secrets, passwords, access tokens, or refresh tokens
- OpenTofu/Terraform state or variable files containing environment values
- private keys, certificates with private material, or database exports

Browser variables prefixed with `VITE_` are embedded in the delivered JavaScript and must never contain secrets. Cognito SPA client IDs and issuer URLs are public configuration; machine-client secrets are not.

If a credential is committed, revoke or rotate it immediately before attempting to rewrite Git history. Removing a secret in a later commit does not remove it from earlier commits.

## Scope disclaimer

Lumen Marsh is a fictional portfolio demonstration. It is not a production safety, dispatch, emergency-management, or ride-control system. Its simulated weather thresholds must not be interpreted as real operating guidance.
