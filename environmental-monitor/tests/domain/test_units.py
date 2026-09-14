from __future__ import annotations

import pytest

from environmental_monitor.domain.units import (
    convert_precipitation_to_mm,
    convert_speed_to_mps,
    convert_temperature,
    fahrenheit_to_celsius,
    kmh_to_mps,
    meters_to_miles,
)


def test_temperature_conversion() -> None:
    assert convert_temperature(32.2, "wmoUnit:degC") == pytest.approx(32.2)
    assert convert_temperature(91.0, "wmoUnit:degF") == pytest.approx(fahrenheit_to_celsius(91.0))


def test_speed_conversion() -> None:
    assert convert_speed_to_mps(16.56, "wmoUnit:km_h-1") == pytest.approx(kmh_to_mps(16.56))
    assert convert_speed_to_mps(10.0, "unit:m_s-1") == pytest.approx(10.0)


def test_precipitation_zero_is_preserved() -> None:
    assert convert_precipitation_to_mm(0.0, "wmoUnit:mm") == 0.0


def test_miles_round_trip() -> None:
    assert meters_to_miles(1609.344) == pytest.approx(1.0)
