from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime

from environmental_monitor.domain.poll import DatasetType, PollAttempt
from environmental_monitor.domain.snapshot import (
    DatasetHealth,
    DatasetState,
    WeatherSnapshot,
    describe_dataset,
)
from environmental_monitor.settings import Settings


@dataclass
class PollTracker:
    last_success_at: datetime | None = None
    last_attempt_at: datetime | None = None
    last_error_category: str | None = None
    last_error_summary: str | None = None
    last_failure_at: datetime | None = None

    def record(self, attempt: PollAttempt) -> None:
        self.last_attempt_at = attempt.attempted_at
        if attempt.succeeded:
            self.last_success_at = attempt.attempted_at
            self.last_error_category = None
            self.last_error_summary = None
        else:
            self.last_failure_at = attempt.attempted_at
            self.last_error_category = attempt.error_category
            self.last_error_summary = attempt.error_summary

    def health(self, now: datetime, stale_after_seconds: int) -> DatasetHealth:
        had_failure = self.last_failure_at is not None and (
            self.last_success_at is None or self.last_failure_at > self.last_success_at
        )
        return describe_dataset(
            now=now,
            last_success_at=self.last_success_at,
            last_attempt_at=self.last_attempt_at,
            last_error_category=self.last_error_category,
            last_error_summary=self.last_error_summary,
            stale_after_seconds=stale_after_seconds,
            had_failure=had_failure,
        )


@dataclass
class WeatherSnapshotStore:
    settings: Settings
    snapshot: WeatherSnapshot = field(init=False)
    location: PollTracker = field(default_factory=PollTracker)
    observation: PollTracker = field(default_factory=PollTracker)
    forecast: PollTracker = field(default_factory=PollTracker)
    alert: PollTracker = field(default_factory=PollTracker)
    lightning: PollTracker = field(default_factory=PollTracker)
    venueops: PollTracker = field(default_factory=PollTracker)

    def __post_init__(self) -> None:
        from datetime import UTC, datetime

        self.snapshot = WeatherSnapshot(evaluated_at=datetime.now(UTC))

    def record(self, attempt: PollAttempt) -> None:
        tracker = {
            DatasetType.LOCATION: self.location,
            DatasetType.OBSERVATION: self.observation,
            DatasetType.FORECAST: self.forecast,
            DatasetType.ALERT: self.alert,
            DatasetType.LIGHTNING: self.lightning,
            DatasetType.VENUEOPS_DELIVERY: self.venueops,
        }[attempt.dataset]
        tracker.record(attempt)

    def refresh_health(self, now: datetime) -> WeatherSnapshot:
        current = self.snapshot
        current.evaluated_at = now
        current.observation_health = self.observation.health(
            now, self.settings.observation_stale_after_seconds
        )
        current.forecast_health = self.forecast.health(
            now, self.settings.forecast_stale_after_seconds
        )
        current.alert_health = self.alert.health(now, self.settings.alert_stale_after_seconds)
        current.lightning_health = self.lightning.health(
            now, self.settings.lightning_clearance_minutes * 60
        )
        current.provider_state = self._provider_state()
        if current.location is not None:
            current.station = current.location.selected_station
        return current

    def _provider_state(self) -> str:
        healths = [
            self.observation.health(
                self.snapshot.evaluated_at, self.settings.observation_stale_after_seconds
            ),
            self.alert.health(self.snapshot.evaluated_at, self.settings.alert_stale_after_seconds),
            self.forecast.health(
                self.snapshot.evaluated_at, self.settings.forecast_stale_after_seconds
            ),
        ]
        states = {item.state for item in healths}
        if states == {DatasetState.NOT_YET_LOADED}:
            return "not_yet_loaded"
        if DatasetState.UNAVAILABLE in states and DatasetState.CURRENT not in states:
            return "unavailable"
        if DatasetState.STALE in states or DatasetState.UNAVAILABLE in states:
            return "degraded"
        return "healthy"
