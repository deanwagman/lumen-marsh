from __future__ import annotations

import json
from datetime import UTC, datetime
from pathlib import Path

import httpx
import pytest
import respx
from tests.conftest import make_settings

from environmental_monitor.clock import FrozenClock
from environmental_monitor.domain.errors import (
    ProviderClientError,
    ProviderInvalidResponseError,
    ProviderRateLimitedError,
    ProviderUnavailableError,
)
from environmental_monitor.infrastructure.http import RetryingHttpClient
from environmental_monitor.infrastructure.providers.nws.client import NwsWeatherProvider

FIXTURES = Path(__file__).parent.parent / "fixtures" / "nws"
NOW = datetime(2026, 9, 1, 18, 55, tzinfo=UTC)
BASE = "https://api.weather.gov"


def _load(name: str) -> dict[str, object]:
    return json.loads((FIXTURES / name).read_text())


def _provider() -> NwsWeatherProvider:
    settings = make_settings()
    http = RetryingHttpClient(
        base_url=settings.nws_base_url,
        user_agent=settings.nws_user_agent,
        timeout_seconds=1,
        max_retries=1,
        rng=__import__("random").Random(0),
    )
    return NwsWeatherProvider(settings, FrozenClock(NOW), http=http)


def _mount_happy_path(router: respx.MockRouter) -> None:
    router.get(f"{BASE}/points/28.474,-81.466").mock(
        return_value=httpx.Response(200, json=_load("points.json"))
    )
    router.get(f"{BASE}/gridpoints/MLB/26,69/stations").mock(
        return_value=httpx.Response(200, json=_load("stations.json"))
    )
    observation = _load("observation_complete.json")
    router.get(f"{BASE}/stations/KMCO/observations/latest").mock(
        return_value=httpx.Response(200, json=observation)
    )
    router.get(f"{BASE}/stations/KORL/observations/latest").mock(
        return_value=httpx.Response(200, json=observation)
    )
    router.get(f"{BASE}/gridpoints/MLB/26,69/forecast/hourly").mock(
        return_value=httpx.Response(200, json=_load("forecast_hourly.json"))
    )
    router.get(f"{BASE}/alerts/active").mock(
        return_value=httpx.Response(200, json=_load("alerts_multiple.json"))
    )


@pytest.mark.asyncio
async def test_point_discovery_and_station_selection() -> None:
    with respx.mock(base_url=BASE, assert_all_called=False) as router:
        _mount_happy_path(router)
        provider = _provider()
        location = await provider.resolve_location()
        assert location.grid_id == "MLB"
        assert location.selected_station is not None
        assert location.selected_station.station_id in {"KMCO", "KORL"}
        observation = await provider.fetch_latest_observation()
        assert observation.station_id == "KMCO"
        forecast = await provider.fetch_hourly_forecast()
        assert forecast
        alerts = await provider.fetch_active_alerts()
        assert len(alerts) == 1
        await provider.aclose()


@pytest.mark.asyncio
async def test_rate_limit_and_server_error() -> None:
    settings = make_settings()
    http = RetryingHttpClient(
        base_url=BASE,
        user_agent=settings.nws_user_agent,
        timeout_seconds=1,
        max_retries=0,
        rng=__import__("random").Random(0),
    )
    with respx.mock(base_url=BASE) as router:
        router.get(f"{BASE}/points/28.474,-81.466").mock(
            return_value=httpx.Response(429, headers={"Retry-After": "1"}, json=_load("error.json"))
        )
        provider = NwsWeatherProvider(settings, FrozenClock(NOW), http=http)
        with pytest.raises(ProviderRateLimitedError):
            await provider.resolve_location()
        await provider.aclose()

    http = RetryingHttpClient(
        base_url=BASE,
        user_agent=settings.nws_user_agent,
        timeout_seconds=1,
        max_retries=0,
        rng=__import__("random").Random(0),
    )
    with respx.mock(base_url=BASE) as router:
        router.get(f"{BASE}/points/28.474,-81.466").mock(
            return_value=httpx.Response(500, json=_load("error.json"))
        )
        provider = NwsWeatherProvider(settings, FrozenClock(NOW), http=http)
        with pytest.raises(ProviderUnavailableError):
            await provider.resolve_location()
        await provider.aclose()


@pytest.mark.asyncio
async def test_malformed_and_client_error() -> None:
    settings = make_settings()
    http = RetryingHttpClient(
        base_url=BASE,
        user_agent=settings.nws_user_agent,
        timeout_seconds=1,
        max_retries=0,
        rng=__import__("random").Random(0),
    )
    with respx.mock(base_url=BASE) as router:
        router.get(f"{BASE}/points/28.474,-81.466").mock(
            return_value=httpx.Response(200, json={"nope": True})
        )
        provider = NwsWeatherProvider(settings, FrozenClock(NOW), http=http)
        with pytest.raises(ProviderInvalidResponseError):
            await provider.resolve_location()
        await provider.aclose()

    http = RetryingHttpClient(
        base_url=BASE,
        user_agent=settings.nws_user_agent,
        timeout_seconds=1,
        max_retries=1,
        rng=__import__("random").Random(0),
    )
    with respx.mock(base_url=BASE) as router:
        router.get(f"{BASE}/points/28.474,-81.466").mock(return_value=httpx.Response(404))
        provider = NwsWeatherProvider(settings, FrozenClock(NOW), http=http)
        with pytest.raises(ProviderClientError):
            await provider.resolve_location()
        await provider.aclose()


@pytest.mark.asyncio
async def test_conditional_request_uses_etag() -> None:
    settings = make_settings()
    http = RetryingHttpClient(
        base_url=BASE,
        user_agent=settings.nws_user_agent,
        timeout_seconds=1,
        max_retries=0,
        rng=__import__("random").Random(0),
    )
    route = None
    with respx.mock(base_url=BASE) as router:
        route = router.get(f"{BASE}/points/28.474,-81.466").mock(
            return_value=httpx.Response(
                200,
                json=_load("points.json"),
                headers={"ETag": '"abc"'},
            )
        )
        first = await http.get_json("/points/28.474,-81.466", operation="resolve_location")
        router.get(f"{BASE}/points/28.474,-81.466").mock(
            return_value=httpx.Response(304, headers={"ETag": '"abc"'})
        )
        second = await http.get_json("/points/28.474,-81.466", operation="resolve_location")
        assert first == second
        assert route.call_count == 2
        assert route.calls.last.request.headers["If-None-Match"] == '"abc"'
        await http.aclose()
