from __future__ import annotations

from collections.abc import AsyncIterator
from datetime import UTC, datetime, timedelta
from pathlib import Path

import pytest
from httpx import ASGITransport, AsyncClient

from environmental_monitor.app_factory import create_app
from environmental_monitor.clock import FrozenClock
from environmental_monitor.domain.forecast import WeatherForecastPeriod
from environmental_monitor.domain.observation import WeatherObservation
from environmental_monitor.infrastructure.providers.fake.fake_weather_provider import (
    FakeWeatherProvider,
    stormglass_location,
)
from environmental_monitor.settings import Settings

FIXTURES = Path(__file__).parent / "fixtures" / "nws"
NOW = datetime(2026, 9, 1, 18, 30, tzinfo=UTC)


def make_settings(**overrides: object) -> Settings:
    values: dict[str, object] = {
        "app_env": "test",
        "log_level": "INFO",
        "park_latitude": 28.474,
        "park_longitude": -81.466,
        "park_timezone": "America/New_York",
        "nws_user_agent": "LumenMarshEnvironmentalMonitor/0.1 (test@example.com)",
        "simulation_enabled": True,
        "database_url": "",
        "observation_poll_seconds": 300,
        "alert_poll_seconds": 60,
        "forecast_poll_seconds": 900,
    }
    values.update(overrides)
    return Settings(_env_file=None, **values)  # type: ignore[arg-type]


def make_observation(
    *,
    now: datetime = NOW,
    wind_gust_mps: float | None = 5.0,
    wind_speed_mps: float | None = 4.0,
    precipitation_mm: float | None = 0.0,
    temperature_c: float | None = 31.0,
    station_id: str = "KMCO",
    source: str = "nws-obs-1",
) -> WeatherObservation:
    return WeatherObservation(
        observation_id=source,
        provider="nws",
        station_id=station_id,
        observed_at=now,
        received_at=now,
        temperature_c=temperature_c,
        relative_humidity_percent=60.0,
        wind_speed_mps=wind_speed_mps,
        wind_gust_mps=wind_gust_mps,
        wind_direction_degrees=120.0,
        precipitation_mm=precipitation_mm,
        visibility_m=16000.0,
        present_weather="Partly Cloudy",
        raw_source_id=source,
    )


def make_forecast(now: datetime = NOW) -> list[WeatherForecastPeriod]:
    return [
        WeatherForecastPeriod(
            starts_at=now,
            ends_at=now + timedelta(hours=1),
            temperature_c=32.0,
            wind_speed_mps=4.5,
            wind_gust_mps=6.0,
            precipitation_probability_percent=20.0,
            summary="Partly Cloudy",
        )
    ]


class RecordingSink:
    def __init__(self) -> None:
        self.published: list[tuple[str, int]] = []

    async def publish(self, recommendation: object) -> None:
        self.published.append((recommendation.id, recommendation.version))  # type: ignore[attr-defined]


@pytest.fixture
def settings() -> Settings:
    return make_settings()


@pytest.fixture
def clock() -> FrozenClock:
    return FrozenClock(NOW)


@pytest.fixture
def provider(clock: FrozenClock) -> FakeWeatherProvider:
    location = stormglass_location(clock.now())
    return FakeWeatherProvider(
        location=location,
        observation=make_observation(now=clock.now()),
        forecast=make_forecast(clock.now()),
        alerts=[],
    )


@pytest.fixture
def sink() -> RecordingSink:
    return RecordingSink()


@pytest.fixture
def app(
    settings: Settings,
    clock: FrozenClock,
    provider: FakeWeatherProvider,
    sink: RecordingSink,
) -> object:
    return create_app(
        settings,
        clock=clock,
        weather_provider=provider,
        recommendation_sink=sink,
        enable_polling=False,
        enable_logging=False,
    )


@pytest.fixture
async def client(app: object) -> AsyncIterator[AsyncClient]:
    async with AsyncClient(
        transport=ASGITransport(app=app),  # type: ignore[arg-type]
        base_url="http://test",
    ) as async_client:
        yield async_client
