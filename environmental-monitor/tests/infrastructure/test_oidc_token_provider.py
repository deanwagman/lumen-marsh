from __future__ import annotations

import base64

import httpx
import pytest
import respx

from environmental_monitor.infrastructure.oidc import CognitoTokenProvider, TokenProviderError

TOKEN_URL = "https://auth.example.com/oauth2/token"


@pytest.mark.asyncio
async def test_token_is_cached_until_refresh_window() -> None:
    now = 1000.0
    async with httpx.AsyncClient() as client:
        provider = CognitoTokenProvider(
            token_url=TOKEN_URL,
            client_id="client-id",
            client_secret="client-secret",
            scope="venueops/weather-recommendations.write",
            timeout_seconds=1,
            client=client,
            monotonic=lambda: now,
        )
        with respx.mock(assert_all_called=True) as router:
            token_route = router.post(TOKEN_URL).mock(
                side_effect=[
                    httpx.Response(200, json={"access_token": "first", "expires_in": 100}),
                    httpx.Response(200, json={"access_token": "second", "expires_in": 100}),
                ]
            )

            assert await provider.get_access_token() == "first"
            now = 1089.0
            assert await provider.get_access_token() == "first"
            now = 1090.0
            assert await provider.get_access_token() == "second"

            assert token_route.call_count == 2
            first_request = token_route.calls[0].request
            expected_basic = base64.b64encode(b"client-id:client-secret").decode()
            assert first_request.headers["Authorization"] == f"Basic {expected_basic}"
            assert first_request.content == (
                b"grant_type=client_credentials&scope=venueops%2Fweather-recommendations.write"
            )


@pytest.mark.asyncio
async def test_token_endpoint_failure_has_distinct_error() -> None:
    async with httpx.AsyncClient() as client:
        provider = CognitoTokenProvider(
            token_url=TOKEN_URL,
            client_id="client-id",
            client_secret="client-secret",
            scope="venueops/weather-recommendations.write",
            timeout_seconds=1,
            client=client,
        )
        with respx.mock(assert_all_called=True) as router:
            router.post(TOKEN_URL).mock(return_value=httpx.Response(401))

            with pytest.raises(TokenProviderError, match="OIDC token endpoint returned HTTP 401"):
                await provider.get_access_token()
