from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
from enum import StrEnum

from environmental_monitor.domain.timeutils import ensure_utc


class DatasetType(StrEnum):
    LOCATION = "location"
    OBSERVATION = "observation"
    FORECAST = "forecast"
    ALERT = "alert"
    LIGHTNING = "lightning"
    VENUEOPS_DELIVERY = "venueops_delivery"


@dataclass(frozen=True, slots=True)
class PollAttempt:
    dataset: DatasetType
    attempted_at: datetime
    succeeded: bool
    duration_ms: float
    error_category: str | None
    error_summary: str | None

    def __post_init__(self) -> None:
        object.__setattr__(self, "attempted_at", ensure_utc(self.attempted_at, "attempted_at"))
