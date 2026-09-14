from __future__ import annotations

from datetime import UTC, datetime

import pytest

from environmental_monitor.domain.observation import WeatherObservation
from environmental_monitor.domain.timeutils import ensure_utc, to_park_timezone


def test_naive_datetime_rejected() -> None:
    with pytest.raises(ValueError, match="timezone-aware"):
        ensure_utc(datetime(2026, 9, 1, 12, 0), "observed_at")


def test_park_timezone_conversion_is_display_only() -> None:
    instant = datetime(2026, 9, 1, 18, 30, tzinfo=UTC)
    local = to_park_timezone(instant, "America/New_York")
    assert local.tzinfo is not None
    assert local.hour == 14
    assert instant.tzinfo == UTC


def test_observation_normalizes_to_utc() -> None:
    observed = datetime.fromisoformat("2026-09-01T14:30:00-04:00")
    observation = WeatherObservation(
        observation_id="1",
        provider="nws",
        station_id="KMCO",
        observed_at=observed,
        received_at=datetime(2026, 9, 1, 18, 31, tzinfo=UTC),
        temperature_c=None,
        relative_humidity_percent=None,
        wind_speed_mps=None,
        wind_gust_mps=None,
        wind_direction_degrees=None,
        precipitation_mm=None,
        visibility_m=None,
        present_weather=None,
        raw_source_id="1",
    )
    assert observation.observed_at.tzinfo == UTC
    assert observation.observed_at.hour == 18
