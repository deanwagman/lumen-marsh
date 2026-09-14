from __future__ import annotations

import json
from datetime import UTC, datetime
from pathlib import Path

import pytest

from environmental_monitor.domain.errors import ProviderInvalidResponseError
from environmental_monitor.infrastructure.providers.nws import mapper

FIXTURES = Path(__file__).parent.parent / "fixtures" / "nws"
NOW = datetime(2026, 9, 1, 18, 55, tzinfo=UTC)


def _load(name: str) -> dict[str, object]:
    return json.loads((FIXTURES / name).read_text())


def test_complete_observation_mapping() -> None:
    observation = mapper.map_observation(_load("observation_complete.json"), received_at=NOW)
    assert observation.provider == "nws"
    assert observation.station_id == "KMCO"
    assert observation.temperature_c == pytest.approx(32.2)
    assert observation.wind_speed_mps == pytest.approx(16.56 / 3.6)
    assert observation.wind_gust_mps == pytest.approx(33.12 / 3.6)
    assert observation.precipitation_mm == 0.0
    assert observation.present_weather == "Partly Cloudy"


def test_missing_optional_measurements_remain_none() -> None:
    observation = mapper.map_observation(_load("observation_missing.json"), received_at=NOW)
    assert observation.temperature_c is None
    assert observation.wind_speed_mps is None
    assert observation.precipitation_mm is None
    assert observation.visibility_m is None


def test_forecast_unit_conversion() -> None:
    periods = mapper.map_forecast(_load("forecast_hourly.json"))
    assert periods[0].temperature_c == pytest.approx((91 - 32) * 5 / 9)
    assert periods[0].wind_gust_mps == pytest.approx(18 * 0.44704)
    assert periods[1].temperature_c == pytest.approx(32.0)
    assert periods[1].precipitation_probability_percent is None


def test_empty_and_expired_alerts() -> None:
    assert mapper.map_alerts(_load("alerts_empty.json"), received_at=NOW) == []
    alerts = mapper.map_alerts(_load("alerts_multiple.json"), received_at=NOW)
    assert len(alerts) == 2
    live = [alert for alert in alerts if not alert.is_expired(NOW)]
    expired = [alert for alert in alerts if alert.is_expired(NOW)]
    assert len(live) == 1
    assert len(expired) == 1


def test_malformed_observation() -> None:
    with pytest.raises(ProviderInvalidResponseError):
        mapper.map_observation({"not": "valid"}, received_at=NOW)
