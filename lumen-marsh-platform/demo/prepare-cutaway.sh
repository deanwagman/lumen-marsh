#!/bin/sh
set -eu

: "${DEMO_ORIGIN:?DEMO_ORIGIN is required}"
origin="${DEMO_ORIGIN%/}"
root=/usr/share/nginx/html

# The exhibit is intentionally local-first. For the opt-in hosted package,
# rewrite its generated static URLs inside the disposable container only.
# Source under cutaway/ remains byte-for-byte unchanged.
find "$root" -type f \( -name '*.html' -o -name '*.js' \) -exec sed -i \
  -e "s|http://localhost:8080|$origin|g" \
  -e "s|http://127.0.0.1:8080|$origin|g" \
  -e "s|http://localhost:8000|$origin/demo/weather|g" \
  -e "s|http://127.0.0.1:8000|$origin/demo/weather|g" \
  -e "s|http://localhost:8100|$origin/demo/flow|g" \
  -e "s|http://127.0.0.1:8100|$origin/demo/flow|g" \
  -e "s|http://localhost:8200|$origin/demo/reliability|g" \
  -e "s|http://127.0.0.1:8200|$origin/demo/reliability|g" \
  -e "s|http://localhost:3000|$origin/guest|g" \
  -e "s|http://127.0.0.1:3000|$origin/guest|g" \
  -e "s|http://localhost:3001|$origin|g" \
  -e "s|http://127.0.0.1:3001|$origin|g" \
  -e 's|/tower/dashboard|/dashboard|g' \
  -e 's|/tower/history|/history|g' \
  {} +

exec nginx -g 'daemon off;'
