from __future__ import annotations

import re
from datetime import datetime
from typing import Any

from pydantic import ValidationError

from environmental_monitor.application.weather_monitor import haversine_km
from environmental_monitor.domain.alert import WeatherAlert
from environmental_monitor.domain.errors import ProviderInvalidResponseError
from environmental_monitor.domain.forecast import WeatherForecastPeriod
from environmental_monitor.domain.observation import WeatherObservation
from environmental_monitor.domain.station import WeatherLocation, WeatherStation
from environmental_monitor.domain.timeutils import parse_datetime
from environmental_monitor.domain.units import (
    convert_length_to_m,
    convert_precipitation_to_mm,
    convert_speed_to_mps,
    convert_temperature,
    fahrenheit_to_celsius,
    mph_to_mps,
)
from environmental_monitor.infrastructure.providers.nws.models import (
    NwsAlertCollection,
    NwsForecastResponse,
    NwsObservationResponse,
    NwsPointResponse,
    NwsStationCollection,
    QuantitativeValue,
)

_SPEED_RE = re.compile(r"(\d+(?:\.\d+)?)")


def map_location(
    payload: dict[str, Any],
    *,
    latitude: float,
    longitude: float,
    received_at: datetime,
    selected_station: WeatherStation | None = None,
) -> WeatherLocation:
    try:
        parsed = NwsPointResponse.model_validate(payload)
    except ValidationError as exc:
        raise ProviderInvalidResponseError("NWS point response was malformed") from exc
    props = parsed.properties
    return WeatherLocation(
        latitude=latitude,
        longitude=longitude,
        forecast_office=props.forecast_office,
        grid_id=props.grid_id,
        grid_x=props.grid_x,
        grid_y=props.grid_y,
        hourly_forecast_url=props.forecast_hourly,
        observation_stations_url=props.observation_stations,
        forecast_zone=props.forecast_zone,
        county_zone=props.county,
        resolved_at=received_at,
        selected_station=selected_station,
    )


def map_stations(
    payload: dict[str, Any],
    *,
    latitude: float,
    longitude: float,
) -> list[WeatherStation]:
    try:
        parsed = NwsStationCollection.model_validate(payload)
    except ValidationError as exc:
        raise ProviderInvalidResponseError("NWS station list was malformed") from exc
    stations: list[WeatherStation] = []
    for feature in parsed.features:
        coords = _coordinates(feature.geometry)
        if coords is None:
            continue
        station_lon, station_lat = coords
        stations.append(
            WeatherStation(
                station_id=feature.properties.station_identifier,
                name=feature.properties.name,
                latitude=station_lat,
                longitude=station_lon,
                distance_km=haversine_km(latitude, longitude, station_lat, station_lon),
                provider="nws",
            )
        )
    stations.sort(key=lambda station: station.distance_km or 0.0)
    return stations


def map_observation(payload: dict[str, Any], *, received_at: datetime) -> WeatherObservation:
    try:
        parsed = NwsObservationResponse.model_validate(payload)
    except ValidationError as exc:
        raise ProviderInvalidResponseError("NWS observation was malformed") from exc
    props = parsed.properties
    station_id = _station_id(props.station)
    source_id = parsed.id or props.timestamp
    present = props.text_description
    if not present and props.present_weather:
        present = ", ".join(str(item) for item in props.present_weather)
    return WeatherObservation(
        observation_id=source_id,
        provider="nws",
        station_id=station_id,
        observed_at=parse_datetime(props.timestamp, "timestamp"),
        received_at=received_at,
        temperature_c=_quantity(props.temperature, convert_temperature),
        relative_humidity_percent=_raw_value(props.relative_humidity),
        wind_speed_mps=_quantity(props.wind_speed, convert_speed_to_mps),
        wind_gust_mps=_quantity(props.wind_gust, convert_speed_to_mps),
        wind_direction_degrees=_raw_value(props.wind_direction),
        precipitation_mm=_quantity(props.precipitation_last_hour, convert_precipitation_to_mm),
        visibility_m=_quantity(props.visibility, convert_length_to_m),
        present_weather=present,
        raw_source_id=source_id,
    )


def map_forecast(payload: dict[str, Any]) -> list[WeatherForecastPeriod]:
    try:
        parsed = NwsForecastResponse.model_validate(payload)
    except ValidationError as exc:
        raise ProviderInvalidResponseError("NWS forecast was malformed") from exc
    periods: list[WeatherForecastPeriod] = []
    for item in parsed.properties.periods:
        temperature = item.temperature
        if temperature is not None and (item.temperature_unit or "F").upper().startswith("F"):
            temperature = fahrenheit_to_celsius(temperature)
        elif temperature is not None and (item.temperature_unit or "").upper().startswith("C"):
            temperature = float(temperature)
        periods.append(
            WeatherForecastPeriod(
                starts_at=parse_datetime(item.start_time, "startTime"),
                ends_at=parse_datetime(item.end_time, "endTime"),
                temperature_c=temperature,
                wind_speed_mps=_parse_wind(item.wind_speed),
                wind_gust_mps=_parse_wind(item.wind_gust),
                precipitation_probability_percent=_raw_value(item.probability_of_precipitation),
                summary=item.short_forecast,
            )
        )
    return periods


def map_alerts(payload: dict[str, Any], *, received_at: datetime) -> list[WeatherAlert]:
    try:
        parsed = NwsAlertCollection.model_validate(payload)
    except ValidationError as exc:
        raise ProviderInvalidResponseError("NWS alert collection was malformed") from exc
    alerts: list[WeatherAlert] = []
    for feature in parsed.features:
        props = feature.properties
        alert_id = props.id or feature.id
        if not alert_id:
            raise ProviderInvalidResponseError("NWS alert was missing an id")
        alerts.append(
            WeatherAlert(
                provider_alert_id=alert_id,
                event=props.event,
                severity=props.severity,
                urgency=props.urgency,
                certainty=props.certainty,
                headline=props.headline or props.event,
                description=props.description or "",
                instructions=props.instruction or "",
                onset=parse_datetime(props.onset, "onset") if props.onset else None,
                expires_at=parse_datetime(props.expires, "expires") if props.expires else None,
                received_at=received_at,
            )
        )
    return alerts


def _quantity(
    value: QuantitativeValue | None,
    converter: Any,
) -> float | None:
    if value is None or value.value is None:
        return None
    unit = value.unit_code or ""
    if not unit:
        return float(value.value)
    return float(converter(value.value, unit))


def _raw_value(value: QuantitativeValue | None) -> float | None:
    if value is None or value.value is None:
        return None
    return float(value.value)


def _parse_wind(value: str | QuantitativeValue | None) -> float | None:
    if value is None:
        return None
    if isinstance(value, QuantitativeValue):
        return _quantity(value, convert_speed_to_mps)
    numbers = [float(match) for match in _SPEED_RE.findall(value)]
    if not numbers:
        return None
    peak = max(numbers)
    lowered = value.lower()
    if "kt" in lowered or "knot" in lowered:
        from environmental_monitor.domain.units import kt_to_mps

        return kt_to_mps(peak)
    if "km" in lowered:
        from environmental_monitor.domain.units import kmh_to_mps

        return kmh_to_mps(peak)
    return mph_to_mps(peak)


def _station_id(station: str | None) -> str:
    if not station:
        return "unknown"
    return station.rstrip("/").split("/")[-1]


def _coordinates(geometry: dict[str, Any] | None) -> tuple[float, float] | None:
    if not geometry:
        return None
    coords = geometry.get("coordinates")
    if not isinstance(coords, list) or len(coords) < 2:
        return None
    return float(coords[0]), float(coords[1])
