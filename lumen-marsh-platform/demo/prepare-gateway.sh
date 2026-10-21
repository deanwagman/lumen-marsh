#!/bin/sh
set -eu

# Optional DEMO_ACME_EMAIL becomes Caddy's certificate account contact.
# An empty value leaves the Caddyfile unchanged, so demo.localhost still uses
# its local certificate.

valid_acme_email() {
  case "$1" in
    *[!A-Za-z0-9._%+@-]*) return 1 ;;
  esac
  case "$1" in
    ?*@?*.?*) return 0 ;;
    *) return 1 ;;
  esac
}

render_caddyfile() {
  source_file="${CADDYFILE_SOURCE:-/etc/caddy/Caddyfile}"
  email="${DEMO_ACME_EMAIL:-}"
  if [ -n "$email" ]; then
    if ! valid_acme_email "$email"; then
      echo "DEMO_ACME_EMAIL must be one email address" >&2
      exit 1
    fi
  fi
  awk -v email="$email" '
    $0 ~ /^[[:space:]]*auto_https disable_redirects[[:space:]]*$/ && email != "" {
      print
      print "\temail " email
      inserted = 1
      next
    }
    { print }
    END {
      if (email != "" && inserted != 1) {
        print "Caddyfile has no auto_https disable_redirects line for the certificate account" > "/dev/stderr"
        exit 1
      }
    }
  ' "$source_file"
}

if [ "${1:-}" = "--render" ]; then
  render_caddyfile
  exit 0
fi

render_caddyfile > /tmp/Caddyfile
exec caddy run --config /tmp/Caddyfile --adapter caddyfile
