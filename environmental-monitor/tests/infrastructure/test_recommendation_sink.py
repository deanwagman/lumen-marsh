from __future__ import annotations

from typing import NoReturn

import httpx
import pytest
import respx
from tests.conftest import make_settings

from environmental_monitor.domain.errors import ProviderClientError
from environmental_monitor.infrastructure.http import RetryingHttpClient
from environmental_monitor.infrastructure.oidc import TokenProviderError
from environmental_monitor.infrastructure.venueops.recommendation_sink import (
    VenueOpsRecommendationSink,
)

VENUEOPS_URL = "https://venueops.example.com"


class StaticTokenProvider:
    async def get_access_token(self) -> str:
        return "access-token"

    async def aclose(self) -> None:
        pass


class FailingTokenProvider:
    async def get_access_token(self) -> NoReturn:
        raise TokenProviderError("token unavailable")

    async def aclose(self) -> None:
        pass


def _http_client() -> RetryingHttpClient:
    return RetryingHttpClient(
        base_url=VENUEOPS_URL,
        user_agent="LumenMarshEnvironmentalMonitor/0.1 (test@example.com)",
        timeout_seconds=1,
        max_retries=0,
    )


@pytest.mark.asyncio
async def test_recommendation_post_includes_bearer_token() -> None:
    settings = make_settings(venueops_base_url=VENUEOPS_URL, venueops_max_retries=0)
    sink = VenueOpsRecommendationSink(
        settings,
        http=_http_client(),
        token_provider=StaticTokenProvider(),
    )
    with respx.mock(assert_all_called=True) as router:
        route = router.post(f"{VENUEOPS_URL}/api/v1/integrations/weather/recommendations").mock(
            return_value=httpx.Response(202)
        )

        await sink._post({"recommendationId": "recommendation-1"})

        assert route.calls[0].request.headers["Authorization"] == "Bearer access-token"
    await sink.aclose()


@pytest.mark.asyncio
async def test_token_failure_is_not_reported_as_venueops_delivery_failure() -> None:
    settings = make_settings(venueops_base_url=VENUEOPS_URL, venueops_max_retries=0)
    sink = VenueOpsRecommendationSink(
        settings,
        http=_http_client(),
        token_provider=FailingTokenProvider(),
    )

    with pytest.raises(TokenProviderError, match="token unavailable"):
        await sink._post({"recommendationId": "recommendation-1"})
    await sink.aclose()


@pytest.mark.asyncio
async def test_venueops_failure_remains_provider_client_error() -> None:
    settings = make_settings(venueops_base_url=VENUEOPS_URL, venueops_max_retries=0)
    sink = VenueOpsRecommendationSink(
        settings,
        http=_http_client(),
        token_provider=StaticTokenProvider(),
    )
    with respx.mock(assert_all_called=True) as router:
        router.post(f"{VENUEOPS_URL}/api/v1/integrations/weather/recommendations").mock(
            return_value=httpx.Response(400)
        )

        with pytest.raises(ProviderClientError, match="VenueOps rejected"):
            await sink._post({"recommendationId": "recommendation-1"})
    await sink.aclose()
