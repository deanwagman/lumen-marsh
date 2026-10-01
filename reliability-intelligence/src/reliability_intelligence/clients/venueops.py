from __future__ import annotations

import asyncio
import logging
from typing import Any, Protocol

import httpx

from reliability_intelligence.clients.oidc import (
    CognitoTokenProvider,
    StaticTokenProvider,
    TokenProvider,
)
from reliability_intelligence.domain.models import ReliabilitySample, new_correlation_id
from reliability_intelligence.settings import Settings

logger = logging.getLogger("reliability-intelligence")


class VenueOpsClientError(Exception):
    """VenueOps rejected or failed an ingest request."""


class VenueOpsClient(Protocol):
    async def submit_recommendation(self, sample: ReliabilitySample) -> dict[str, Any]: ...

    async def aclose(self) -> None: ...


class HttpVenueOpsClient:
    """Posts reliability recommendations. Never calls maintenance or attraction commands."""

    def __init__(
        self,
        settings: Settings,
        *,
        client: httpx.AsyncClient | None = None,
        token_provider: TokenProvider | None = None,
    ) -> None:
        self._settings = settings
        self._client = client or httpx.AsyncClient(
            base_url=settings.venueops_base_url,
            timeout=httpx.Timeout(settings.venueops_timeout_seconds),
            headers={
                "Accept": "application/json",
                "User-Agent": "LumenMarshReliabilityIntelligence/0.1",
            },
        )
        self._owns_client = client is None
        self._owns_token_provider = token_provider is None
        self._token_provider: TokenProvider | None
        if token_provider is not None:
            self._token_provider = token_provider
        elif settings.oidc_enabled:
            self._token_provider = CognitoTokenProvider(
                token_url=settings.oidc_token_url,
                client_id=settings.oidc_client_id,
                client_secret=settings.oidc_client_secret,
                scope=settings.oidc_scope,
                timeout_seconds=settings.venueops_timeout_seconds,
            )
        elif settings.venueops_bearer_token.strip():
            self._token_provider = StaticTokenProvider(settings.venueops_bearer_token.strip())
        else:
            self._token_provider = None

    async def aclose(self) -> None:
        if self._owns_client:
            await self._client.aclose()
        if self._owns_token_provider and self._token_provider is not None:
            await self._token_provider.aclose()

    async def submit_recommendation(self, sample: ReliabilitySample) -> dict[str, Any]:
        payload = {
            "observationId": sample.observation_id,
            "observedAt": sample.observed_at.isoformat().replace("+00:00", "Z"),
            "assetCode": sample.asset_code,
            "signalType": sample.signal_type.value,
            "severity": sample.severity.value,
            "value": sample.value,
            "unit": sample.unit,
            "evidence": sample.evidence,
            "recommendedAction": sample.recommended_action,
        }
        return await self._post("/api/v1/integrations/reliability/recommendations", payload)

    async def _post(self, path: str, payload: dict[str, Any]) -> dict[str, Any]:
        headers: dict[str, str] = {"X-Correlation-Id": str(new_correlation_id())}
        if self._token_provider is not None:
            token = await self._token_provider.get_access_token()
            headers["Authorization"] = f"Bearer {token}"
        last_error: Exception | None = None
        for attempt in range(self._settings.venueops_max_retries + 1):
            try:
                response = await self._client.post(path, json=payload, headers=headers)
            except (httpx.TimeoutException, httpx.TransportError) as exc:
                last_error = VenueOpsClientError(f"{path} failed: {exc.__class__.__name__}")
                await self._backoff(attempt)
                continue
            if response.status_code in {200, 201}:
                try:
                    body = response.json()
                except ValueError:
                    return {}
                return body if isinstance(body, dict) else {}
            if response.status_code in {400, 409, 422}:
                raise VenueOpsClientError(
                    f"VenueOps rejected {path} with HTTP {response.status_code}"
                )
            if response.status_code in {429, 500, 502, 503, 504}:
                last_error = VenueOpsClientError(f"VenueOps returned HTTP {response.status_code}")
                await self._backoff(attempt)
                continue
            last_error = VenueOpsClientError(f"VenueOps returned HTTP {response.status_code}")
            break
        assert last_error is not None
        logger.warning(
            "venueops ingest failed",
            extra={"operation": path, "success": False, "error_category": "venueops"},
        )
        raise last_error

    async def _backoff(self, attempt: int) -> None:
        if attempt >= self._settings.venueops_max_retries:
            return
        await asyncio.sleep(0.01 * (2**attempt))
