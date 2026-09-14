#!/usr/bin/env bash
set -euo pipefail
# Filesystem and local-image scans. Requires trivy (https://trivy.dev).
# CI runs the same filesystem/secret scan via aquasecurity/trivy-action.

# shellcheck source=./_lib.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_lib.sh"

require_cmd trivy

echo "Scanning platform filesystem for vulnerabilities and secrets"
trivy fs --scanners vuln,secret --severity CRITICAL,HIGH --exit-code 1 --ignore-unfixed .

images=(
  "${VENUEOPS_IMAGE:-venueops-api:local}"
  "${OPERATOR_CONSOLE_IMAGE:-venueops-console:local}"
  "${ENVIRONMENTAL_MONITOR_IMAGE:-environmental-monitor:local}"
  "${GUEST_APP_IMAGE:-lumen-marsh-app:local}"
)

scanned=0
for image in "${images[@]}"; do
  if docker image inspect "$image" >/dev/null 2>&1; then
    echo "Scanning container image $image"
    trivy image --severity CRITICAL,HIGH --exit-code 1 --ignore-unfixed "$image"
    scanned=$((scanned + 1))
  else
    echo "skip: image not present locally ($image)"
  fi
done

if [[ "$scanned" -eq 0 ]]; then
  echo "No local application images found. Build with ./scripts/dev-up.sh, then re-run this scan before a public demo."
fi
