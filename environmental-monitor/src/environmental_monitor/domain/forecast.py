from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime

from environmental_monitor.domain.timeutils import ensure_utc


@dataclass(frozen=True, slots=True)
class WeatherForecastPeriod:
    """One hourly forecast period in canonical domain units."""

    starts_at: datetime
    ends_at: datetime
    temperature_c: float | None
    wind_speed_mps: float | None
    wind_gust_mps: float | None
    precipitation_probability_percent: float | None
    summary: str

    def __post_init__(self) -> None:
        object.__setattr__(self, "starts_at", ensure_utc(self.starts_at, "starts_at"))
        object.__setattr__(self, "ends_at", ensure_utc(self.ends_at, "ends_at"))
        if self.ends_at <= self.starts_at:
            raise ValueError("Forecast period end must be after start")
