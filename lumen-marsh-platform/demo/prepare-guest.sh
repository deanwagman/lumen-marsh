#!/bin/sh
set -eu

index=/usr/share/nginx/html/index.html
if ! grep -q '<base href="/">' "$index"; then
  echo "error: guest build does not contain the expected root base href" >&2
  exit 1
fi

# Keep Flutter assets and path routing under the gateway's /guest mount.
sed -i 's|<base href="/">|<base href="/guest/">|' "$index"

exec nginx -g 'daemon off;'
