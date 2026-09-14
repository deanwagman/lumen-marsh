from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from enum import StrEnum

from environmental_monitor.domain.alert import WeatherAlert
from environmental_monitor.domain.forecast import WeatherForecastPeriod
from environmental_monitor.domain.lightning import LightningObservation
from environmental_monitor.domain.observation import WeatherObservation
from environmental_monitor.domain.recommendation import WeatherRecommendation
from environmental_monitor.domain.station import WeatherLocation, WeatherStation
from environmental_monitor.domain.timeutils import ensure_utc


class DatasetState(StrEnum):
    CURRENT = "CURRENT"
    STALE = "STALE"
    UNAVAILABLE = "UNAVAILABLE"
    NOT_YET_LOADED = "NOT_YET_LOADED"


@dataclass(frozen=True, slots=True)
class DatasetHealth:
    state: DatasetState
    last_success_at: datetime | None
    last_attempt_at: datetime | None
    last_error_category: str | None
    last_error_summary: str | None
    age_seconds: float | None = None

    def __post_init__(self) -> None:
        if self.last_success_at is not None:
            object.__setattr__(
                self, "last_success_at", ensure_utc(self.last_success_at, "last_success_at")
            )
        if self.last_attempt_at is not None:
            object.__setattr__(
                self, "last_attempt_at", ensure_utc(self.last_attempt_at, "last_attempt_at")
            )


def describe_dataset(
    *,
    now: datetime,
    last_success_at: datetime | None,
    last_attempt_at: datetime | None,
    last_error_category: str | None,
    last_error_summary: str | None,
    stale_after_seconds: int,
    had_failure: bool,
) -> DatasetHealth:
    instant = ensure_utc(now, "now")
    if last_success_at is None:
        if last_attempt_at is None:
            state = DatasetState.NOT_YET_LOADED
        else:
            state = DatasetState.UNAVAILABLE
        return DatasetHealth(
            state=state,
            last_success_at=last_success_at,
            last_attempt_at=last_attempt_at,
            last_error_category=last_error_category,
            last_error_summary=last_error_summary,
            age_seconds=None,
        )
    age = (instant - ensure_utc(last_success_at, "last_success_at")).total_seconds()
    if age > stale_after_seconds:
        state = DatasetState.STALE
    elif had_failure:
        state = DatasetState.STALE
    else:
        state = DatasetState.CURRENT
    return DatasetHealth(
        state=state,
        last_success_at=last_success_at,
        last_attempt_at=last_attempt_at,
        last_error_category=last_error_category,
        last_error_summary=last_error_summary,
        age_seconds=age,
    )


@dataclass(slots=True)
class WeatherSnapshot:
    """The latest successful provider data plus explicit freshness state.

    Missing measurements stay None. Stale or missing data is never treated as
    a safe operating condition.
    """

    evaluated_at: datetime
    observation: WeatherObservation | None = None
    observation_health: DatasetHealth = field(
        default_factory=lambda: DatasetHealth(DatasetState.NOT_YET_LOADED, None, None, None, None)
    )
    forecast: tuple[WeatherForecastPeriod, ...] = ()
    forecast_health: DatasetHealth = field(
        default_factory=lambda: DatasetHealth(DatasetState.NOT_YET_LOADED, None, None, None, None)
    )
    alerts: tuple[WeatherAlert, ...] = ()
    alert_health: DatasetHealth = field(
        default_factory=lambda: DatasetHealth(DatasetState.NOT_YET_LOADED, None, None, None, None)
    )
    lightning: LightningObservation | None = None
    lightning_health: DatasetHealth = field(
        default_factory=lambda: DatasetHealth(DatasetState.NOT_YET_LOADED, None, None, None, None)
    )
    location: WeatherLocation | None = None
    station: WeatherStation | None = None
    provider_state: str = "not_yet_loaded"
    prior_recommendations: tuple[WeatherRecommendation, ...] = ()

    def __post_init__(self) -> None:
        self.evaluated_at = ensure_utc(self.evaluated_at, "evaluated_at")

    def usable_observation(self) -> WeatherObservation | None:
        if self.observation_health.state is DatasetState.NOT_YET_LOADED:
            return None
        if self.observation_health.state is DatasetState.UNAVAILABLE:
            return None
        return self.observation
