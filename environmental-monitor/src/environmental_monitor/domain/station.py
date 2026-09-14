from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime

from environmental_monitor.domain.timeutils import ensure_utc


@dataclass(frozen=True, slots=True)
class WeatherStation:
    station_id: str
    name: str
    latitude: float
    longitude: float
    distance_km: float | None
    provider: str


@dataclass(frozen=True, slots=True)
class WeatherLocation:
    """Cached NWS point metadata for the configured park coordinates."""

    latitude: float
    longitude: float
    forecast_office: str
    grid_id: str
    grid_x: int
    grid_y: int
    hourly_forecast_url: str
    observation_stations_url: str
    forecast_zone: str
    county_zone: str
    resolved_at: datetime
    selected_station: WeatherStation | None = None

    def __post_init__(self) -> None:
        object.__setattr__(self, "resolved_at", ensure_utc(self.resolved_at, "resolved_at"))
