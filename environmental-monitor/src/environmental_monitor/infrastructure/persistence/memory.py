from __future__ import annotations

from collections.abc import Sequence

from environmental_monitor.domain.alert import WeatherAlert
from environmental_monitor.domain.forecast import WeatherForecastPeriod
from environmental_monitor.domain.observation import WeatherObservation
from environmental_monitor.domain.poll import PollAttempt
from environmental_monitor.domain.recommendation import RecommendationStatus, WeatherRecommendation


class InMemoryObservationRepository:
    def __init__(self) -> None:
        self._by_source: dict[tuple[str, str], WeatherObservation] = {}

    async def upsert(self, observation: WeatherObservation) -> WeatherObservation:
        key = (observation.provider, observation.raw_source_id)
        existing = self._by_source.get(key)
        if existing is not None:
            return existing
        self._by_source[key] = observation
        return observation

    async def latest(self) -> WeatherObservation | None:
        if not self._by_source:
            return None
        return max(self._by_source.values(), key=lambda item: item.observed_at)


class InMemoryAlertRepository:
    def __init__(self) -> None:
        self._items: dict[str, WeatherAlert] = {}

    async def replace_active(self, alerts: Sequence[WeatherAlert]) -> None:
        for alert in alerts:
            self._items[alert.provider_alert_id] = alert

    async def list_active(self) -> list[WeatherAlert]:
        return list(self._items.values())


class InMemoryForecastRepository:
    def __init__(self) -> None:
        self._items: list[WeatherForecastPeriod] = []

    async def replace_current(self, periods: Sequence[WeatherForecastPeriod]) -> None:
        self._items = list(periods)

    async def list_current(self) -> list[WeatherForecastPeriod]:
        return list(self._items)


class InMemoryRecommendationRepository:
    def __init__(self) -> None:
        self._items: dict[str, WeatherRecommendation] = {}

    async def save(self, recommendation: WeatherRecommendation) -> None:
        self._items[recommendation.id] = recommendation

    async def get(self, recommendation_id: str) -> WeatherRecommendation | None:
        return self._items.get(recommendation_id)

    async def list_all(self) -> list[WeatherRecommendation]:
        return list(self._items.values())

    async def active_by_rule(self, rule_id: str) -> WeatherRecommendation | None:
        for item in self._items.values():
            if item.rule_id == rule_id and item.status is RecommendationStatus.ACTIVE:
                return item
        return None

    async def pending_delivery(self) -> list[WeatherRecommendation]:
        return [item for item in self._items.values() if item.needs_delivery]


class InMemoryPollAttemptRepository:
    def __init__(self) -> None:
        self._items: list[PollAttempt] = []

    async def record(self, attempt: PollAttempt) -> None:
        self._items.append(attempt)

    async def recent(self, limit: int = 50) -> list[PollAttempt]:
        return list(self._items[-limit:])
