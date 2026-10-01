from __future__ import annotations

import asyncio
import time
from collections.abc import Callable
from typing import Protocol

import httpx


class TokenProvider(Protocol):
    async def get_access_token(self) -> str: ...

    async def aclose(self) -> None: ...


class TokenProviderError(Exception):
    """Cognito token acquisition failed."""


class StaticTokenProvider:
    """Fixed bearer token for LOCAL_JWT Compose bridges (never for production Cognito)."""

    def __init__(self, access_token: str) -> None:
        self._access_token = access_token

    async def get_access_token(self) -> str:
        return self._access_token

    async def aclose(self) -> None:
        return None


class CognitoTokenProvider:
    """Gets and caches OAuth client-credentials tokens from Cognito."""

    def __init__(
        self,
        *,
        token_url: str,
        client_id: str,
        client_secret: str,
        scope: str,
        timeout_seconds: float,
        refresh_skew_seconds: float = 30.0,
        client: httpx.AsyncClient | None = None,
        monotonic: Callable[[], float] = time.monotonic,
    ) -> None:
        self._token_url = token_url
        self._client_id = client_id
        self._client_secret = client_secret
        self._scope = scope
        self._refresh_skew_seconds = refresh_skew_seconds
        self._monotonic = monotonic
        self._client = client or httpx.AsyncClient(timeout=httpx.Timeout(timeout_seconds))
        self._owns_client = client is None
        self._access_token: str | None = None
        self._refresh_at = 0.0
        self._lock = asyncio.Lock()

    async def get_access_token(self) -> str:
        if self._access_token is not None and self._monotonic() < self._refresh_at:
            return self._access_token
        async with self._lock:
            if self._access_token is not None and self._monotonic() < self._refresh_at:
                return self._access_token
            return await self._request_token()

    async def aclose(self) -> None:
        if self._owns_client:
            await self._client.aclose()

    async def _request_token(self) -> str:
        try:
            response = await self._client.post(
                self._token_url,
                data={
                    "grant_type": "client_credentials",
                    "scope": self._scope,
                },
                auth=httpx.BasicAuth(self._client_id, self._client_secret),
                headers={"Accept": "application/json"},
            )
        except (httpx.TimeoutException, httpx.TransportError) as exc:
            raise TokenProviderError(
                f"OIDC token request failed: {exc.__class__.__name__}"
            ) from exc
        if response.status_code >= 400:
            raise TokenProviderError(f"OIDC token endpoint returned HTTP {response.status_code}")
        try:
            payload = response.json()
            token = payload["access_token"]
            expires_in = float(payload["expires_in"])
        except (KeyError, TypeError, ValueError) as exc:
            raise TokenProviderError("OIDC token endpoint returned an invalid response") from exc
        if not isinstance(token, str) or not token or expires_in <= 0:
            raise TokenProviderError("OIDC token endpoint returned an invalid response")
        refresh_skew = min(self._refresh_skew_seconds, expires_in * 0.1)
        self._access_token = token
        self._refresh_at = self._monotonic() + expires_in - refresh_skew
        return token
