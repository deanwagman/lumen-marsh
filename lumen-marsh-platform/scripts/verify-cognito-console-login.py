#!/usr/bin/env python3
"""Independent Cognito hosted-UI check for the venueops-console public client.

Starts a one-shot listener on http://127.0.0.1:5173/auth/callback, prints the
authorize URL, exchanges the authorization code, and prints access-token claims.
Does not print the access token.
"""

from __future__ import annotations

import base64
import hashlib
import json
import os
import secrets
import subprocess
import sys
import threading
import urllib.parse
import urllib.request
import webbrowser
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ENV_FILE = ROOT / ".env.cognito.local"
CALLBACK = "http://127.0.0.1:5173/auth/callback"
SCOPES = "openid email profile venueops/operator.read venueops/attractions.command"


def open_login_url(url: str) -> bool:
    """Prefer a clean Chrome Incognito session, then use the default browser."""
    if sys.platform == "darwin":
        result = subprocess.run(
            ["open", "-na", "Google Chrome", "--args", "--incognito", url],
            check=False,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
        if result.returncode == 0:
            return True
    return webbrowser.open(url, new=2)


def load_env() -> dict[str, str]:
    if not ENV_FILE.exists():
        sys.stderr.write("error: missing .env.cognito.local — run ./scripts/create-dev-operator.sh\n")
        sys.exit(1)
    values: dict[str, str] = {}
    for raw in ENV_FILE.read_text().splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key] = value
    required = ["COGNITO_DOMAIN", "CONSOLE_CLIENT_ID"]
    missing = [key for key in required if not values.get(key)]
    if missing:
        sys.stderr.write(f"error: .env.cognito.local missing {', '.join(missing)}\n")
        sys.exit(1)
    return values


def pkce() -> tuple[str, str]:
    verifier = secrets.token_urlsafe(64)
    digest = hashlib.sha256(verifier.encode("ascii")).digest()
    challenge = base64.urlsafe_b64encode(digest).rstrip(b"=").decode("ascii")
    return verifier, challenge


def decode_claims(token: str) -> dict:
    payload = token.split(".")[1]
    padded = payload + "=" * (-len(payload) % 4)
    return json.loads(base64.urlsafe_b64decode(padded.encode("ascii")))


def authorize_url(domain: str, client_id: str, challenge: str, state: str) -> str:
    query = urllib.parse.urlencode(
        {
            "client_id": client_id,
            "response_type": "code",
            "scope": SCOPES,
            "redirect_uri": CALLBACK,
            "code_challenge": challenge,
            "code_challenge_method": "S256",
            "state": state,
            "identity_provider": "COGNITO",
        }
    )
    return f"{domain.rstrip('/')}/oauth2/authorize?{query}"


def exchange_code(domain: str, client_id: str, code: str, verifier: str) -> dict:
    body = urllib.parse.urlencode(
        {
            "grant_type": "authorization_code",
            "client_id": client_id,
            "code": code,
            "redirect_uri": CALLBACK,
            "code_verifier": verifier,
        }
    ).encode("ascii")
    request = urllib.request.Request(
        f"{domain.rstrip('/')}/oauth2/token",
        data=body,
        headers={"Content-Type": "application/x-www-form-urlencoded"},
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=20) as response:
        return json.loads(response.read().decode("utf-8"))


def main() -> int:
    env = load_env()
    verifier, challenge = pkce()
    state = secrets.token_urlsafe(16)
    captured: dict[str, str] = {}
    ready = threading.Event()

    class Handler(BaseHTTPRequestHandler):
        def do_GET(self) -> None:  # noqa: N802
            parsed = urllib.parse.urlparse(self.path)
            if parsed.path != "/auth/callback":
                self.send_error(404)
                return
            params = urllib.parse.parse_qs(parsed.query)
            captured["query"] = parsed.query
            captured["code"] = params.get("code", [""])[0]
            captured["state"] = params.get("state", [""])[0]
            captured["error"] = params.get("error", [""])[0]
            body = b"Cognito callback received. You can return to the terminal."
            self.send_response(200)
            self.send_header("Content-Type", "text/plain; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            ready.set()

        def log_message(self, format: str, *args: object) -> None:
            return

    server = HTTPServer(("127.0.0.1", 5173), Handler)
    thread = threading.Thread(target=server.handle_request, daemon=True)
    thread.start()

    url = authorize_url(env["COGNITO_DOMAIN"], env["CONSOLE_CLIENT_ID"], challenge, state)
    print("Open this hosted login URL and sign in as the development operator:\n")
    print(url)
    if not open_login_url(url):
        print("\nThe browser did not open automatically; copy the URL above into your browser.")
    print(f"\nWaiting for {CALLBACK} ...")
    if not ready.wait(timeout=300):
        sys.stderr.write("error: timed out waiting for the Cognito callback\n")
        return 1
    server.server_close()

    if captured.get("error"):
        sys.stderr.write(f"error: Cognito returned {captured['error']}\n")
        return 1
    if captured.get("state") != state:
        sys.stderr.write("error: state mismatch on callback\n")
        return 1
    if not captured.get("code"):
        sys.stderr.write("error: callback did not include an authorization code\n")
        return 1

    tokens = exchange_code(env["COGNITO_DOMAIN"], env["CONSOLE_CLIENT_ID"], captured["code"], verifier)
    access = tokens.get("access_token")
    if not isinstance(access, str) or access.count(".") != 2:
        sys.stderr.write("error: token endpoint did not return a JWT access token\n")
        return 1

    claims = decode_claims(access)
    interesting = {
        "sub": claims.get("sub"),
        "client_id": claims.get("client_id"),
        "aud": claims.get("aud"),
        "scope": claims.get("scope"),
        "cognito:groups": claims.get("cognito:groups"),
        "exp": claims.get("exp"),
        "iss": claims.get("iss"),
        "token_use": claims.get("token_use"),
    }
    print("\nAccess token claims:")
    print(json.dumps(interesting, indent=2))

    groups = interesting.get("cognito:groups") or []
    scope = interesting.get("scope") or ""
    ok = True
    if "operators" not in groups:
        sys.stderr.write("error: cognito:groups does not include operators\n")
        ok = False
    for required in ("venueops/operator.read", "venueops/attractions.command"):
        if required not in str(scope).split():
            sys.stderr.write(f"error: missing scope {required}\n")
            ok = False
    if interesting.get("iss") != env.get("COGNITO_ISSUER_URI"):
        sys.stderr.write("error: iss does not match COGNITO_ISSUER_URI\n")
        ok = False
    if interesting.get("client_id") != env.get("CONSOLE_CLIENT_ID"):
        sys.stderr.write("error: client_id does not match CONSOLE_CLIENT_ID\n")
        ok = False
    if not ok:
        return 1
    print("\nCognito console login verified.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
