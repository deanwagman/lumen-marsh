from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
from enum import StrEnum

from environmental_monitor.domain.timeutils import ensure_utc


class LightningScenario(StrEnum):
    CLEAR = "CLEAR"
    STORM_APPROACHING = "STORM_APPROACHING"
    HOLD_CONDITIONS = "HOLD_CONDITIONS"
    CLEARANCE_PERIOD = "CLEARANCE_PERIOD"


@dataclass(frozen=True, slots=True)
class LightningObservation:
    """A simulated lightning observation. Never interchangeable with NWS data."""

    observation_id: str
    provider: str
    distance_miles: float
    bearing_degrees: float
    observed_at: datetime
    received_at: datetime
    scenario: LightningScenario
    simulated: bool = True

    def __post_init__(self) -> None:
        if not self.simulated or self.provider != "simulated":
            raise ValueError("Lightning observations must be labeled as simulated")
        if not 0 <= self.bearing_degrees <= 360:
            raise ValueError("bearing_degrees must be between 0 and 360")
        if self.distance_miles < 0:
            raise ValueError("distance_miles cannot be negative")
        object.__setattr__(self, "observed_at", ensure_utc(self.observed_at, "observed_at"))
        object.__setattr__(self, "received_at", ensure_utc(self.received_at, "received_at"))
