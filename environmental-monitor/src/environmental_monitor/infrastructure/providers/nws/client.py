from __future__ import annotations

from datetime import timedelta

from environmental_monitor.clock import Clock
from environmental_monitor.domain.alert import WeatherAlert
from environmental_monitor.domain.errors import ProviderInvalidResponseError
from environmental_monitor.domain.forecast import WeatherForecastPeriod
from environmental_monitor.domain.observation import WeatherObservation
from environmental_monitor.domain.station import WeatherLocation, WeatherStation
from environmental_monitor.infrastructure.http import RetryingHttpClient
from environmental_monitor.infrastructure.providers.nws import mapper
from environmental_monitor.settings import Settings


class NwsWeatherProvider:
    def __init__(
        self,
        settings: Settings,
        clock: Clock,
        http: RetryingHttpClient | None = None,
    ) -> None:
        self._settings = settings
        self._clock = clock
        self._http = http or RetryingHttpClient(
            base_url=settings.nws_base_url,
            user_agent=settings.nws_user_agent,
            timeout_seconds=settings.provider_timeout_seconds,
            max_retries=settings.provider_max_retries,
        )
        self._location: WeatherLocation | None = None

    async def aclose(self) -> None:
        await self._http.aclose()

    async def resolve_location(self) -> WeatherLocation:
        if self._location is not None:
            age = self._clock.now() - self._location.resolved_at
            if age < timedelta(seconds=self._settings.location_metadata_refresh_seconds):
                return self._location
        point_path = f"/points/{self._settings.park_latitude},{self._settings.park_longitude}"
        payload = await self._http.get_json(point_path, operation="resolve_location")
        location = mapper.map_location(
            payload,
            latitude=self._settings.park_latitude,
            longitude=self._settings.park_longitude,
            received_at=self._clock.now(),
        )
        stations_payload = await self._http.get_json(
            location.observation_stations_url, operation="list_stations"
        )
        stations = mapper.map_stations(
            stations_payload,
            latitude=self._settings.park_latitude,
            longitude=self._settings.park_longitude,
        )
        selected = await self._select_station(stations)
        location = WeatherLocation(
            latitude=location.latitude,
            longitude=location.longitude,
            forecast_office=location.forecast_office,
            grid_id=location.grid_id,
            grid_x=location.grid_x,
            grid_y=location.grid_y,
            hourly_forecast_url=location.hourly_forecast_url,
            observation_stations_url=location.observation_stations_url,
            forecast_zone=location.forecast_zone,
            county_zone=location.county_zone,
            resolved_at=location.resolved_at,
            selected_station=selected,
        )
        self._location = location
        return location

    async def fetch_latest_observation(self) -> WeatherObservation:
        location = await self.resolve_location()
        if location.selected_station is None:
            raise ProviderInvalidResponseError("No usable observation station was selected")
        path = f"/stations/{location.selected_station.station_id}/observations/latest"
        payload = await self._http.get_json(path, operation="fetch_latest_observation")
        return mapper.map_observation(payload, received_at=self._clock.now())

    async def fetch_hourly_forecast(self) -> list[WeatherForecastPeriod]:
        location = await self.resolve_location()
        payload = await self._http.get_json(
            location.hourly_forecast_url, operation="fetch_hourly_forecast"
        )
        return mapper.map_forecast(payload)

    async def fetch_active_alerts(self) -> list[WeatherAlert]:
        path = (
            f"/alerts/active?point={self._settings.park_latitude},{self._settings.park_longitude}"
        )
        payload = await self._http.get_json(path, operation="fetch_active_alerts")
        now = self._clock.now()
        return [
            alert
            for alert in mapper.map_alerts(payload, received_at=now)
            if not alert.is_expired(now)
        ]

    async def _select_station(self, stations: list[WeatherStation]) -> WeatherStation | None:
        if not stations:
            return None
        now = self._clock.now()
        for station in stations[:8]:
            try:
                payload = await self._http.get_json(
                    f"/stations/{station.station_id}/observations/latest",
                    operation="probe_station",
                )
                observation = mapper.map_observation(payload, received_at=now)
            except Exception:
                continue
            age = now - observation.observed_at
            if age <= timedelta(hours=6):
                return station
        return stations[0]
