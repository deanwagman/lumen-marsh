"""Canonical units for provider-neutral weather values.

Internal units:
- Temperature: degrees Celsius
- Wind speed and gust: meters per second
- Precipitation: millimeters
- Visibility: meters
- Relative humidity: percent (0-100)
- Wind direction: meteorological degrees (direction the wind is coming from)
- Lightning distance: statute miles (park operations convention)
- Timestamps: timezone-aware UTC

Missing provider measurements must remain None. Never coerce missing values to zero.
"""

from __future__ import annotations

from typing import Final

KMH_TO_MPS: Final = 1.0 / 3.6
MPH_TO_MPS: Final = 0.44704
KT_TO_MPS: Final = 0.514444
INCH_TO_MM: Final = 25.4
MILE_TO_M: Final = 1609.344
METER_TO_MILE: Final = 1.0 / MILE_TO_M


def kmh_to_mps(value: float) -> float:
    return value * KMH_TO_MPS


def mph_to_mps(value: float) -> float:
    return value * MPH_TO_MPS


def kt_to_mps(value: float) -> float:
    return value * KT_TO_MPS


def fahrenheit_to_celsius(value: float) -> float:
    return (value - 32.0) * 5.0 / 9.0


def inches_to_mm(value: float) -> float:
    return value * INCH_TO_MM


def miles_to_meters(value: float) -> float:
    return value * MILE_TO_M


def meters_to_miles(value: float) -> float:
    return value * METER_TO_MILE


def convert_temperature(value: float, unit_code: str) -> float:
    normalized = _normalize_unit(unit_code)
    if normalized in {"degc", "c", "celsius"}:
        return value
    if normalized in {"degf", "f", "fahrenheit"}:
        return fahrenheit_to_celsius(value)
    if normalized in {"k", "degk", "kelvin"}:
        return value - 273.15
    raise ValueError(f"Unsupported temperature unit: {unit_code}")


def convert_speed_to_mps(value: float, unit_code: str) -> float:
    normalized = _normalize_unit(unit_code)
    if normalized in {"m_s-1", "m/s", "mps"}:
        return value
    if normalized in {"km_h-1", "km/h", "kph"}:
        return kmh_to_mps(value)
    if normalized in {"mi_h-1", "mph"}:
        return mph_to_mps(value)
    if normalized in {"kt", "kn", "knot", "knots"}:
        return kt_to_mps(value)
    raise ValueError(f"Unsupported speed unit: {unit_code}")


def convert_length_to_m(value: float, unit_code: str) -> float:
    normalized = _normalize_unit(unit_code)
    if normalized in {"m", "meter", "meters"}:
        return value
    if normalized in {"km"}:
        return value * 1000.0
    if normalized in {"mi", "mile", "miles"}:
        return miles_to_meters(value)
    if normalized in {"ft", "foot", "feet"}:
        return value * 0.3048
    raise ValueError(f"Unsupported length unit: {unit_code}")


def convert_precipitation_to_mm(value: float, unit_code: str) -> float:
    normalized = _normalize_unit(unit_code)
    if normalized in {"mm"}:
        return value
    if normalized in {"cm"}:
        return value * 10.0
    if normalized in {"m"}:
        return value * 1000.0
    if normalized in {"in", "inch", "inches"}:
        return inches_to_mm(value)
    raise ValueError(f"Unsupported precipitation unit: {unit_code}")


def _normalize_unit(unit_code: str) -> str:
    value = unit_code.strip().lower()
    prefixes = ("wmounit:", "unit:", "wmo:", "ucum:")
    for prefix in prefixes:
        if value.startswith(prefix):
            value = value[len(prefix) :]
            break
    return value.replace(" ", "")
