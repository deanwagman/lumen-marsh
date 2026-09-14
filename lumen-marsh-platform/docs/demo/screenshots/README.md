# Demo evidence

Walkthrough frames for [`../storm-lifecycle-demo.md`](../storm-lifecycle-demo.md).

Captured against the local Cognito OIDC stack (Control Tower on `:5173`, guest app on `:3000`):

| File | Moment |
| --- | --- |
| `00-console-login.png` | Control Tower sign-in wall |
| `00-cognito-hosted-ui.png` | Cognito hosted UI (no credentials entered) |
| `05-guest-today.png` | Flutter Today with **Park advisories** |
| `06-guest-detail.png` | Advisory detail — title, message, severity, affected attractions only |

Replace the remaining slots after a **supervisor** sign-in on a clean seed (`./scripts/reset-demo.sh --confirm --oidc`). The current database still contains leftover smoke/advisory-demo notices; guest JSON for those items stayed on the allowlist (`id`, `severity`, `title`, `message`, `affectedAttractionIds`, `updatedAt`, `version`).
