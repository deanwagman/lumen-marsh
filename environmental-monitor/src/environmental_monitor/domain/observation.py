from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime

from environmental_monitor.domain.timeutils import ensure_utc


@dataclass(frozen=True, slots=True)
class WeatherObservation:
    """A single station observation in canonical domain units."""

    observation_id: str
    provider: str
    station_id: str
    observed_at: datetime
    received_at: datetime
    temperature_c: float | None
    relative_humidity_percent: float | None
    wind_speed_mps: float | None
    wind_gust_mps: float | None
    wind_direction_degrees: float | None
    precipitation_mm: float | None
    visibility_m: float | None
    present_weather: str | None
    raw_source_id: str

    def __post_init__(self) -> None:
        object.__setattr__(self, "observed_at", ensure_utc(self.observed_at, "observed_at"))
        object.__setattr__(self, "received_at", ensure_utc(self.received_at, "received_at"))
        if self.provider == "nws" and self.raw_source_id.startswith("simulated"):
            raise ValueError("NWS observations cannot carry a simulated source id")
