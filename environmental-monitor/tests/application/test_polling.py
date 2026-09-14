from __future__ import annotations

from datetime import timedelta

from tests.conftest import RecordingSink, make_settings

from environmental_monitor.application.polling_service import PollingService
from environmental_monitor.application.recommendation_engine import RecommendationEngine
from environmental_monitor.application.weather_monitor import LightningSimulator, WeatherMonitor
from environmental_monitor.application.weather_snapshot import WeatherSnapshotStore
from environmental_monitor.domain.errors import ProviderUnavailableError
from environmental_monitor.domain.lightning import LightningScenario
from environmental_monitor.domain.recommendation import RecommendationStatus
from environmental_monitor.domain.rules import LightningHoldRule
from environmental_monitor.domain.snapshot import DatasetState
from environmental_monitor.infrastructure.persistence.memory import (
    InMemoryAlertRepository,
    InMemoryForecastRepository,
    InMemoryObservationRepository,
    InMemoryPollAttemptRepository,
    InMemoryRecommendationRepository,
)
from environmental_monitor.metrics import MetricsRegistry


def _monitor(clock, provider, settings=None, sink=None) -> WeatherMonitor:
    resolved = settings or make_settings()
    store = WeatherSnapshotStore(resolved)
    return WeatherMonitor(
        settings=resolved,
        clock=clock,
        provider=provider,
        engine=RecommendationEngine(
            [
                LightningHoldRule(
                    attraction_ids=(resolved.mangrove_run_id, resolved.cypress_coil_id),
                    hold_radius_miles=resolved.lightning_hold_radius_miles,
                    clearance_minutes=resolved.lightning_clearance_minutes,
                )
            ]
        ),
        store=store,
        observations=InMemoryObservationRepository(),
        alerts=InMemoryAlertRepository(),
        forecasts=InMemoryForecastRepository(),
        recommendations=InMemoryRecommendationRepository(),
        polls=InMemoryPollAttemptRepository(),
        sink=sink or RecordingSink(),
        simulator=LightningSimulator(resolved, clock),
        metrics=MetricsRegistry(),
    )


async def test_independent_poll_failure(clock, provider) -> None:
    monitor = _monitor(clock, provider)
    provider.fail("fetch_latest_observation", ProviderUnavailableError("down"))
    await monitor.poll_observation()
    await monitor.poll_alerts()
    snapshot = monitor.current_snapshot()
    assert snapshot.observation_health.state is DatasetState.UNAVAILABLE
    assert snapshot.alert_health.state is DatasetState.CURRENT
    assert "fetch_active_alerts" in provider.calls


async def test_stale_state_uses_clock(clock, provider) -> None:
    settings = make_settings(observation_stale_after_seconds=60)
    monitor = _monitor(clock, provider, settings=settings)
    await monitor.poll_observation()
    assert monitor.current_snapshot().observation_health.state is DatasetState.CURRENT
    clock.advance(timedelta(seconds=120))
    assert monitor.current_snapshot().observation_health.state is DatasetState.STALE


async def test_polling_starts_and_stops(clock, provider) -> None:
    settings = make_settings(
        observation_poll_seconds=1,
        alert_poll_seconds=1,
        forecast_poll_seconds=1,
        location_metadata_refresh_seconds=1,
    )
    monitor = _monitor(clock, provider, settings=settings)
    service = PollingService(monitor, settings)
    await service.start()
    await monitor.poll_observation()
    await service.stop()
    assert monitor.current_snapshot().observation is not None


async def test_lightning_scenario_creates_recommendation(clock, provider, sink) -> None:
    monitor = _monitor(clock, provider, sink=sink)
    monitor.simulator.apply_scenario(LightningScenario.HOLD_CONDITIONS)
    await monitor.refresh_lightning()
    await monitor.evaluate_and_publish()
    active = [
        item
        for item in await monitor.recommendations.list_all()
        if item.status is RecommendationStatus.ACTIVE
    ]
    assert active
    assert sink.published
    assert active[0].affected_attraction_ids == ("mangrove-run", "cypress-coil")
