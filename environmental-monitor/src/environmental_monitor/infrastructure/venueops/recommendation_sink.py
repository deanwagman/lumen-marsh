from __future__ import annotations

import logging

from environmental_monitor.domain.errors import ProviderClientError
from environmental_monitor.domain.recommendation import WeatherRecommendation
from environmental_monitor.infrastructure.http import RetryingHttpClient
from environmental_monitor.infrastructure.oidc import (
    CognitoTokenProvider,
    StaticTokenProvider,
    TokenProvider,
)
from environmental_monitor.infrastructure.venueops.mapper import to_venueops_payload
from environmental_monitor.settings import Settings

logger = logging.getLogger("environmental-monitor")


class VenueOpsRecommendationSink:
    """Delivers recommendations to VenueOps. Never calls attraction-command endpoints."""

    def __init__(
        self,
        settings: Settings,
        http: RetryingHttpClient | None = None,
        token_provider: TokenProvider | None = None,
    ) -> None:
        self._settings = settings
        self._http = http or RetryingHttpClient(
            base_url=settings.venueops_base_url,
            user_agent=settings.nws_user_agent,
            timeout_seconds=settings.venueops_timeout_seconds,
            max_retries=settings.venueops_max_retries,
            accept="application/json",
        )
        self._owns_token_provider = token_provider is None and (
            settings.oidc_enabled or bool(settings.venueops_bearer_token.strip())
        )
        self._token_provider = token_provider
        if self._token_provider is None and settings.oidc_enabled:
            self._token_provider = CognitoTokenProvider(
                token_url=settings.oidc_token_url,
                client_id=settings.oidc_client_id,
                client_secret=settings.oidc_client_secret,
                scope=settings.oidc_scope,
                timeout_seconds=settings.venueops_timeout_seconds,
            )
        elif self._token_provider is None and settings.venueops_bearer_token.strip():
            self._token_provider = StaticTokenProvider(settings.venueops_bearer_token.strip())

    async def aclose(self) -> None:
        await self._http.aclose()
        if self._owns_token_provider and self._token_provider is not None:
            await self._token_provider.aclose()

    async def publish(self, recommendation: WeatherRecommendation) -> None:
        payload = to_venueops_payload(recommendation)
        logger.info(
            "publishing recommendation to venueops",
            extra={
                "provider": "venueops",
                "operation": "publish_recommendation",
                "recommendation_id": recommendation.id,
                "success": True,
            },
        )
        await self._post(payload)

    async def _post(self, payload: dict[str, object]) -> None:
        headers: dict[str, str] = {}
        if self._token_provider is not None:
            token = await self._token_provider.get_access_token()
            headers["Authorization"] = f"Bearer {token}"

        last_error: Exception | None = None
        for attempt in range(self._settings.venueops_max_retries + 1):
            try:
                response = await self._http._client.post(
                    "/api/v1/integrations/weather/recommendations",
                    json=payload,
                    headers=headers,
                )
            except Exception as exc:
                last_error = exc
                await self._http._backoff(attempt, None)
                continue
            if response.status_code in {200, 201, 202, 204}:
                return
            if response.status_code in {400, 409, 422}:
                raise ProviderClientError(
                    f"VenueOps rejected recommendation with HTTP {response.status_code}"
                )
            if response.status_code in {429, 500, 502, 503, 504}:
                last_error = ProviderClientError(f"VenueOps returned HTTP {response.status_code}")
                await self._http._backoff(attempt, None)
                continue
            last_error = ProviderClientError(f"VenueOps returned HTTP {response.status_code}")
            break
        assert last_error is not None
        raise last_error


class LoggingRecommendationSink:
    async def publish(self, recommendation: WeatherRecommendation) -> None:
        logger.info(
            "recommendation ready for venueops",
            extra={
                "provider": "venueops",
                "operation": "publish_recommendation",
                "recommendation_id": recommendation.id,
                "success": True,
            },
        )
