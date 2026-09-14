"""Clock abstraction so tests can control time without process-global patching."""

from __future__ import annotations

from datetime import UTC, datetime, timedelta
from typing import Protocol


class Clock(Protocol):
    def now(self) -> datetime:
        """Return the current timezone-aware UTC instant."""


class SystemClock:
    def now(self) -> datetime:
        return datetime.now(UTC)


class FrozenClock:
    def __init__(self, instant: datetime) -> None:
        if instant.tzinfo is None:
            raise ValueError("FrozenClock requires a timezone-aware datetime")
        self._now = instant.astimezone(UTC)

    def now(self) -> datetime:
        return self._now

    def set(self, instant: datetime) -> None:
        if instant.tzinfo is None:
            raise ValueError("FrozenClock requires a timezone-aware datetime")
        self._now = instant.astimezone(UTC)

    def advance(self, delta: timedelta) -> datetime:
        self._now = self._now + delta
        return self._now
