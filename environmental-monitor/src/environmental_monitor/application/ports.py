from __future__ import annotations

from collections.abc import Sequence
from typing import Protocol

from environmental_monitor.domain.alert import WeatherAlert
from environmental_monitor.domain.forecast import WeatherForecastPeriod
from environmental_monitor.domain.observation import WeatherObservation
from environmental_monitor.domain.poll import PollAttempt
from environmental_monitor.domain.recommendation import WeatherRecommendation
from environmental_monitor.domain.station import WeatherLocation


class WeatherProvider(Protocol):
    """Application-facing weather source. Implementations live in infrastructure."""

    async def resolve_location(self) -> WeatherLocation: ...

    async def fetch_latest_observation(self) -> WeatherObservation: ...

    async def fetch_hourly_forecast(self) -> list[WeatherForecastPeriod]: ...

    async def fetch_active_alerts(self) -> list[WeatherAlert]: ...


class RecommendationSink(Protocol):
    async def publish(self, recommendation: WeatherRecommendation) -> None: ...


class ObservationRepository(Protocol):
    async def upsert(self, observation: WeatherObservation) -> WeatherObservation: ...

    async def latest(self) -> WeatherObservation | None: ...


class AlertRepository(Protocol):
    async def replace_active(self, alerts: Sequence[WeatherAlert]) -> None: ...

    async def list_active(self) -> list[WeatherAlert]: ...


class ForecastRepository(Protocol):
    async def replace_current(self, periods: Sequence[WeatherForecastPeriod]) -> None: ...

    async def list_current(self) -> list[WeatherForecastPeriod]: ...


class RecommendationRepository(Protocol):
    async def save(self, recommendation: WeatherRecommendation) -> None: ...

    async def get(self, recommendation_id: str) -> WeatherRecommendation | None: ...

    async def list_all(self) -> list[WeatherRecommendation]: ...

    async def active_by_rule(self, rule_id: str) -> WeatherRecommendation | None: ...

    async def pending_delivery(self) -> list[WeatherRecommendation]: ...


class PollAttemptRepository(Protocol):
    async def record(self, attempt: PollAttempt) -> None: ...

    async def recent(self, limit: int = 50) -> list[PollAttempt]: ...
