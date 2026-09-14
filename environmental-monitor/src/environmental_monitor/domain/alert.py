from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime

from environmental_monitor.domain.timeutils import ensure_utc


@dataclass(frozen=True, slots=True)
class WeatherAlert:
    """An official weather alert in provider-neutral form."""

    provider_alert_id: str
    event: str
    severity: str
    urgency: str
    certainty: str
    headline: str
    description: str
    instructions: str
    onset: datetime | None
    expires_at: datetime | None
    received_at: datetime

    def __post_init__(self) -> None:
        object.__setattr__(self, "received_at", ensure_utc(self.received_at, "received_at"))
        if self.onset is not None:
            object.__setattr__(self, "onset", ensure_utc(self.onset, "onset"))
        if self.expires_at is not None:
            object.__setattr__(self, "expires_at", ensure_utc(self.expires_at, "expires_at"))

    def is_expired(self, now: datetime) -> bool:
        if self.expires_at is None:
            return False
        return self.expires_at <= ensure_utc(now, "now")
