from __future__ import annotations

import asyncio
import logging
import random
from dataclasses import dataclass
from email.utils import parsedate_to_datetime
from typing import Any

import httpx

from environmental_monitor.domain.errors import (
    ProviderClientError,
    ProviderInvalidResponseError,
    ProviderRateLimitedError,
    ProviderUnavailableError,
)

logger = logging.getLogger("environmental-monitor")

RETRYABLE_STATUS = frozenset({429, 500, 502, 503, 504})


@dataclass
class CachedBody:
    etag: str | None
    last_modified: str | None
    body: bytes
    json_payload: Any


class RetryingHttpClient:
    """HTTPX wrapper with timeout, retries, Retry-After, and conditional requests."""

    def __init__(
        self,
        *,
        base_url: str,
        user_agent: str,
        timeout_seconds: float,
        max_retries: int,
        accept: str = "application/geo+json, application/ld+json;q=0.9, application/json;q=0.8",
        rng: random.Random | None = None,
    ) -> None:
        self._max_retries = max_retries
        self._rng = rng or random.Random()
        self._cache: dict[str, CachedBody] = {}
        self._client = httpx.AsyncClient(
            base_url=base_url,
            timeout=httpx.Timeout(timeout_seconds),
            headers={
                "User-Agent": user_agent,
                "Accept": accept,
            },
            follow_redirects=True,
        )

    async def aclose(self) -> None:
        await self._client.aclose()

    async def get_json(self, url: str, *, operation: str) -> Any:
        last_error: Exception | None = None
        for attempt in range(self._max_retries + 1):
            headers: dict[str, str] = {}
            cached = self._cache.get(url)
            if cached is not None:
                if cached.etag:
                    headers["If-None-Match"] = cached.etag
                if cached.last_modified:
                    headers["If-Modified-Since"] = cached.last_modified
            try:
                response = await self._client.get(url, headers=headers)
            except httpx.TimeoutException:
                last_error = ProviderUnavailableError(f"{operation} timed out")
                await self._backoff(attempt, None)
                continue
            except httpx.TransportError as exc:
                last_error = ProviderUnavailableError(
                    f"{operation} network error: {exc.__class__.__name__}"
                )
                await self._backoff(attempt, None)
                continue

            if response.status_code == 304 and cached is not None:
                logger.info(
                    "provider cache hit",
                    extra={
                        "provider": "nws",
                        "operation": operation,
                        "success": True,
                        "duration_ms": response.elapsed.total_seconds() * 1000,
                    },
                )
                return cached.json_payload

            if response.status_code in RETRYABLE_STATUS:
                retry_after = _retry_after_seconds(response)
                if response.status_code == 429:
                    last_error = ProviderRateLimitedError(
                        f"{operation} rate limited",
                        retry_after_seconds=retry_after,
                    )
                else:
                    last_error = ProviderUnavailableError(
                        f"{operation} returned HTTP {response.status_code}"
                    )
                await self._backoff(attempt, retry_after)
                continue

            if response.status_code >= 400:
                raise ProviderClientError(f"{operation} returned HTTP {response.status_code}")

            try:
                payload = response.json()
            except ValueError as exc:
                raise ProviderInvalidResponseError(f"{operation} returned malformed JSON") from exc

            self._cache[url] = CachedBody(
                etag=response.headers.get("ETag"),
                last_modified=response.headers.get("Last-Modified"),
                body=response.content,
                json_payload=payload,
            )
            return payload

        assert last_error is not None
        raise last_error

    async def _backoff(self, attempt: int, retry_after: float | None) -> None:
        if attempt >= self._max_retries:
            return
        if retry_after is not None:
            delay = retry_after
        else:
            delay = (2**attempt) + self._rng.random()
        await asyncio.sleep(delay)


def _retry_after_seconds(response: httpx.Response) -> float | None:
    header = response.headers.get("Retry-After")
    if not header:
        return None
    try:
        return max(0.0, float(header))
    except ValueError:
        try:
            when = parsedate_to_datetime(header)
        except (TypeError, ValueError):
            return None
        from datetime import UTC, datetime

        return max(0.0, (when.astimezone(UTC) - datetime.now(UTC)).total_seconds())
