from __future__ import annotations

import logging
import math
import time
from collections.abc import Awaitable, Callable
from datetime import timedelta
from uuid import uuid4

from environmental_monitor.application.ports import (
    AlertRepository,
    ForecastRepository,
    ObservationRepository,
    PollAttemptRepository,
    RecommendationRepository,
    RecommendationSink,
    WeatherProvider,
)
from environmental_monitor.application.recommendation_engine import RecommendationEngine
from environmental_monitor.application.weather_snapshot import WeatherSnapshotStore
from environmental_monitor.clock import Clock
from environmental_monitor.domain.errors import ProviderError, ProviderRateLimitedError
from environmental_monitor.domain.lightning import LightningObservation, LightningScenario
from environmental_monitor.domain.poll import DatasetType, PollAttempt
from environmental_monitor.domain.recommendation import RecommendationStatus
from environmental_monitor.domain.snapshot import WeatherSnapshot
from environmental_monitor.metrics import MetricsRegistry
from environmental_monitor.settings import Settings

logger = logging.getLogger("environmental-monitor")


class LightningSimulator:
    def __init__(self, settings: Settings, clock: Clock) -> None:
        self._settings = settings
        self._clock = clock
        self._observation: LightningObservation | None = None
        self._scenario = LightningScenario.CLEAR

    @property
    def scenario(self) -> LightningScenario:
        return self._scenario

    @property
    def observation(self) -> LightningObservation | None:
        return self._observation

    def reset_clear(self) -> LightningObservation | None:
        self._scenario = LightningScenario.CLEAR
        self._observation = None
        return None

    def apply_scenario(self, scenario: LightningScenario) -> LightningObservation | None:
        now = self._clock.now()
        self._scenario = scenario
        if scenario is LightningScenario.CLEAR:
            return self.reset_clear()
        if scenario is LightningScenario.STORM_APPROACHING:
            distance = self._settings.lightning_hold_radius_miles + 5.0
            observed_at = now
        elif scenario is LightningScenario.HOLD_CONDITIONS:
            distance = max(1.0, self._settings.lightning_hold_radius_miles - 5.0)
            observed_at = now
        else:
            distance = max(1.0, self._settings.lightning_hold_radius_miles - 5.0)
            observed_at = now - timedelta(minutes=self._settings.lightning_clearance_minutes)
        self._observation = LightningObservation(
            observation_id=str(uuid4()),
            provider="simulated",
            distance_miles=distance,
            bearing_degrees=245.0,
            observed_at=observed_at,
            received_at=now,
            scenario=scenario,
            simulated=True,
        )
        return self._observation

    def record_strike(
        self,
        *,
        distance_miles: float,
        bearing_degrees: float,
        observed_at: object | None = None,
    ) -> LightningObservation:
        from datetime import datetime

        now = self._clock.now()
        observed = observed_at if isinstance(observed_at, datetime) else now
        if distance_miles <= self._settings.lightning_hold_radius_miles:
            self._scenario = LightningScenario.HOLD_CONDITIONS
        else:
            self._scenario = LightningScenario.STORM_APPROACHING
        self._observation = LightningObservation(
            observation_id=str(uuid4()),
            provider="simulated",
            distance_miles=distance_miles,
            bearing_degrees=bearing_degrees,
            observed_at=observed,
            received_at=now,
            scenario=self._scenario,
            simulated=True,
        )
        return self._observation


class WeatherMonitor:
    def __init__(
        self,
        *,
        settings: Settings,
        clock: Clock,
        provider: WeatherProvider,
        engine: RecommendationEngine,
        store: WeatherSnapshotStore,
        observations: ObservationRepository,
        alerts: AlertRepository,
        forecasts: ForecastRepository,
        recommendations: RecommendationRepository,
        polls: PollAttemptRepository,
        sink: RecommendationSink,
        simulator: LightningSimulator,
        metrics: MetricsRegistry,
    ) -> None:
        self.settings = settings
        self.clock = clock
        self.provider = provider
        self.engine = engine
        self.store = store
        self.observations = observations
        self.alerts = alerts
        self.forecasts = forecasts
        self.recommendations = recommendations
        self.polls = polls
        self.sink = sink
        self.simulator = simulator
        self.metrics = metrics

    def current_snapshot(self) -> WeatherSnapshot:
        return self.store.refresh_health(self.clock.now())

    async def poll_location(self) -> None:
        await self._run_poll(DatasetType.LOCATION, self._poll_location)

    async def poll_observation(self) -> None:
        await self._run_poll(DatasetType.OBSERVATION, self._poll_observation)

    async def poll_forecast(self) -> None:
        await self._run_poll(DatasetType.FORECAST, self._poll_forecast)

    async def poll_alerts(self) -> None:
        await self._run_poll(DatasetType.ALERT, self._poll_alerts)

    async def refresh_lightning(self) -> None:
        await self._run_poll(DatasetType.LIGHTNING, self._poll_lightning)

    async def evaluate_and_publish(self) -> None:
        snapshot = self.current_snapshot()
        existing = await self.recommendations.list_all()
        changed = self.engine.evaluate(snapshot, existing)
        for recommendation in changed:
            await self.recommendations.save(recommendation)
        await self.deliver_pending()
        active = [
            item
            for item in await self.recommendations.list_all()
            if item.status is RecommendationStatus.ACTIVE
        ]
        self.metrics.active_recommendations = len(active)
        if snapshot.observation is not None:
            self.metrics.observation_age_seconds = (
                snapshot.evaluated_at - snapshot.observation.observed_at
            ).total_seconds()

    async def deliver_pending(self) -> None:
        pending = await self.recommendations.pending_delivery()
        for recommendation in pending:
            started = time.perf_counter()
            try:
                await self.sink.publish(recommendation)
            except Exception as exc:
                duration_ms = (time.perf_counter() - started) * 1000
                self.metrics.recommendation_delivery_failures += 1
                attempt = PollAttempt(
                    dataset=DatasetType.VENUEOPS_DELIVERY,
                    attempted_at=self.clock.now(),
                    succeeded=False,
                    duration_ms=duration_ms,
                    error_category=_error_category(exc),
                    error_summary=_safe_error(exc),
                )
                self.store.record(attempt)
                await self.polls.record(attempt)
                logger.warning(
                    "venueops delivery failed",
                    extra={
                        "provider": "venueops",
                        "operation": "publish_recommendation",
                        "recommendation_id": recommendation.id,
                        "error_category": attempt.error_category,
                        "duration_ms": duration_ms,
                        "success": False,
                    },
                )
                continue
            duration_ms = (time.perf_counter() - started) * 1000
            recommendation.mark_delivered(recommendation.version)
            await self.recommendations.save(recommendation)
            self.metrics.last_venueops_success_at = self.clock.now()
            attempt = PollAttempt(
                dataset=DatasetType.VENUEOPS_DELIVERY,
                attempted_at=self.clock.now(),
                succeeded=True,
                duration_ms=duration_ms,
                error_category=None,
                error_summary=None,
            )
            self.store.record(attempt)
            await self.polls.record(attempt)

    async def _poll_location(self) -> None:
        location = await self.provider.resolve_location()
        self.store.snapshot.location = location
        self.store.snapshot.station = location.selected_station

    async def _poll_observation(self) -> None:
        observation = await self.provider.fetch_latest_observation()
        stored = await self.observations.upsert(observation)
        self.store.snapshot.observation = stored

    async def _poll_forecast(self) -> None:
        periods = tuple(await self.provider.fetch_hourly_forecast())
        await self.forecasts.replace_current(periods)
        self.store.snapshot.forecast = periods

    async def _poll_alerts(self) -> None:
        now = self.clock.now()
        alerts = tuple(
            alert
            for alert in await self.provider.fetch_active_alerts()
            if not alert.is_expired(now)
        )
        await self.alerts.replace_active(alerts)
        self.store.snapshot.alerts = alerts

    async def _poll_lightning(self) -> None:
        self.store.snapshot.lightning = self.simulator.observation

    async def _run_poll(
        self,
        dataset: DatasetType,
        operation: Callable[[], Awaitable[None]],
    ) -> None:
        started = time.perf_counter()
        try:
            await operation()
        except Exception as exc:
            duration_ms = (time.perf_counter() - started) * 1000
            attempt = PollAttempt(
                dataset=dataset,
                attempted_at=self.clock.now(),
                succeeded=False,
                duration_ms=duration_ms,
                error_category=_error_category(exc),
                error_summary=_safe_error(exc),
            )
            self.store.record(attempt)
            await self.polls.record(attempt)
            self.metrics.poll_failures[dataset.value] += 1
            self.metrics.record_provider_call(
                duration_ms=duration_ms,
                success=False,
                rate_limited=isinstance(exc, ProviderRateLimitedError),
            )
            logger.warning(
                "poll failed",
                extra={
                    "provider": "nws",
                    "operation": dataset.value,
                    "duration_ms": duration_ms,
                    "success": False,
                    "error_category": attempt.error_category,
                },
            )
            return
        duration_ms = (time.perf_counter() - started) * 1000
        attempt = PollAttempt(
            dataset=dataset,
            attempted_at=self.clock.now(),
            succeeded=True,
            duration_ms=duration_ms,
            error_category=None,
            error_summary=None,
        )
        self.store.record(attempt)
        await self.polls.record(attempt)
        self.metrics.poll_successes[dataset.value] += 1
        self.metrics.record_provider_call(duration_ms=duration_ms, success=True, rate_limited=False)
        logger.info(
            "poll succeeded",
            extra={
                "provider": "nws",
                "operation": dataset.value,
                "duration_ms": duration_ms,
                "success": True,
                "station_id": (
                    self.store.snapshot.station.station_id if self.store.snapshot.station else None
                ),
            },
        )
        await self.evaluate_and_publish()


def _error_category(exc: BaseException) -> str:
    if isinstance(exc, ProviderError):
        return exc.category
    return exc.__class__.__name__


def _safe_error(exc: BaseException) -> str:
    text = str(exc)
    for secret in ("password", "token", "secret", "api_key"):
        if secret in text.lower():
            return exc.__class__.__name__
    return text[:300]


def haversine_km(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    radius = 6371.0
    phi1, phi2 = math.radians(lat1), math.radians(lat2)
    d_phi = math.radians(lat2 - lat1)
    d_lambda = math.radians(lon2 - lon1)
    a = math.sin(d_phi / 2) ** 2 + math.cos(phi1) * math.cos(phi2) * math.sin(d_lambda / 2) ** 2
    return 2 * radius * math.asin(math.sqrt(a))
