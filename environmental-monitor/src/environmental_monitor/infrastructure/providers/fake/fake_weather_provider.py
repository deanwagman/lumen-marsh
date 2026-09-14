from __future__ import annotations

from datetime import datetime
from typing import TypeVar

from environmental_monitor.domain.alert import WeatherAlert
from environmental_monitor.domain.errors import ProviderError
from environmental_monitor.domain.forecast import WeatherForecastPeriod
from environmental_monitor.domain.observation import WeatherObservation
from environmental_monitor.domain.station import WeatherLocation, WeatherStation

T = TypeVar("T")


class FakeWeatherProvider:
    """Deterministic weather source for tests. Never touches the network."""

    def __init__(
        self,
        *,
        location: WeatherLocation,
        observation: WeatherObservation | None = None,
        forecast: list[WeatherForecastPeriod] | None = None,
        alerts: list[WeatherAlert] | None = None,
    ) -> None:
        self.location = location
        self.observation = observation
        self.forecast = forecast or []
        self.alerts = alerts or []
        self.calls: list[str] = []
        self._failures: dict[str, Exception] = {}

    def fail(self, operation: str, error: Exception | None = None) -> None:
        if error is None:
            self._failures.pop(operation, None)
        else:
            self._failures[operation] = error

    def fail_once(self, operation: str, error: Exception) -> None:
        self._failures[operation] = error

    async def resolve_location(self) -> WeatherLocation:
        return await self._call("resolve_location", self.location)

    async def fetch_latest_observation(self) -> WeatherObservation:
        if self.observation is None:
            raise ProviderError("fake provider has no observation", category="unavailable")
        return await self._call("fetch_latest_observation", self.observation)

    async def fetch_hourly_forecast(self) -> list[WeatherForecastPeriod]:
        return await self._call("fetch_hourly_forecast", list(self.forecast))

    async def fetch_active_alerts(self) -> list[WeatherAlert]:
        return await self._call("fetch_active_alerts", list(self.alerts))

    async def _call(self, operation: str, value: T) -> T:
        self.calls.append(operation)
        error = self._failures.pop(operation, None)
        if error is not None:
            raise error
        return value


def stormglass_location(now: datetime) -> WeatherLocation:
    station = WeatherStation(
        station_id="KMCO",
        name="Orlando International Airport",
        latitude=28.4294,
        longitude=-81.3089,
        distance_km=16.0,
        provider="fake",
    )
    return WeatherLocation(
        latitude=28.474,
        longitude=-81.466,
        forecast_office="https://api.weather.gov/offices/MLB",
        grid_id="MLB",
        grid_x=26,
        grid_y=69,
        hourly_forecast_url="https://api.weather.gov/gridpoints/MLB/26,69/forecast/hourly",
        observation_stations_url="https://api.weather.gov/gridpoints/MLB/26,69/stations",
        forecast_zone="https://api.weather.gov/zones/forecast/FLZ045",
        county_zone="https://api.weather.gov/zones/county/FLC095",
        resolved_at=now,
        selected_station=station,
    )
