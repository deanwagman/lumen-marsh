from __future__ import annotations

from datetime import timedelta

from tests.conftest import NOW, make_observation

from environmental_monitor.application.recommendation_engine import RecommendationEngine
from environmental_monitor.domain.alert import WeatherAlert
from environmental_monitor.domain.lightning import LightningObservation, LightningScenario
from environmental_monitor.domain.recommendation import RecommendationStatus
from environmental_monitor.domain.rules import (
    ActiveAlertRule,
    HeavyRainRule,
    HighWindRule,
    LightningClearanceRule,
    LightningHoldRule,
)
from environmental_monitor.domain.snapshot import DatasetHealth, DatasetState, WeatherSnapshot


def _health(state: DatasetState = DatasetState.CURRENT) -> DatasetHealth:
    return DatasetHealth(state, NOW, NOW, None, None, 0.0)


def _snapshot(**overrides: object) -> WeatherSnapshot:
    values: dict[str, object] = {
        "evaluated_at": NOW,
        "observation": make_observation(),
        "observation_health": _health(),
        "forecast": (),
        "forecast_health": _health(),
        "alerts": (),
        "alert_health": _health(),
    }
    values.update(overrides)
    return WeatherSnapshot(**values)  # type: ignore[arg-type]


def test_alert_rule_triggers_on_warning() -> None:
    alert = WeatherAlert(
        provider_alert_id="a1",
        event="Severe Thunderstorm Warning",
        severity="Severe",
        urgency="Immediate",
        certainty="Observed",
        headline="Warning",
        description="",
        instructions="",
        onset=NOW,
        expires_at=NOW + timedelta(hours=1),
        received_at=NOW,
    )
    rule = ActiveAlertRule(("mangrove-run", "cypress-coil"))
    result = rule.evaluate(_snapshot(alerts=(alert,)), None)
    assert result.triggered
    assert "weather hold" in result.recommended_action.lower()


def test_expired_alert_does_not_trigger() -> None:
    alert = WeatherAlert(
        provider_alert_id="expired",
        event="Severe Thunderstorm Warning",
        severity="Severe",
        urgency="Immediate",
        certainty="Observed",
        headline="Expired",
        description="",
        instructions="",
        onset=NOW - timedelta(hours=2),
        expires_at=NOW - timedelta(minutes=5),
        received_at=NOW,
    )
    rule = ActiveAlertRule(("mangrove-run", "cypress-coil"))
    assert rule.evaluate(_snapshot(alerts=(alert,)), None).triggered is False


def test_wind_hysteresis_and_missing_data() -> None:
    rule = HighWindRule(attraction_id="cypress-coil", hold_mps=15.0, clear_mps=12.0)
    idle = rule.evaluate(_snapshot(observation=make_observation(wind_gust_mps=10.0)), None)
    assert idle.triggered is False
    active = rule.evaluate(_snapshot(observation=make_observation(wind_gust_mps=16.0)), None)
    assert active.triggered is True
    engine = RecommendationEngine([rule])
    first = engine.evaluate(_snapshot(observation=make_observation(wind_gust_mps=16.0)), [])
    assert len(first) == 1
    held = engine.evaluate(
        _snapshot(observation=make_observation(wind_gust_mps=13.0)),
        first,
    )
    assert held[0].status is RecommendationStatus.ACTIVE
    missing = engine.evaluate(
        _snapshot(observation=make_observation(wind_gust_mps=None, wind_speed_mps=None)),
        first,
    )
    assert missing[0].status is RecommendationStatus.ACTIVE
    cleared = engine.evaluate(
        _snapshot(observation=make_observation(wind_gust_mps=8.0)),
        first,
    )
    assert cleared[0].status is RecommendationStatus.CLEARED


def test_rain_rule_does_not_treat_missing_as_zero() -> None:
    rule = HeavyRainRule(attraction_id="mangrove-run", hold_mm=10.0, clear_mm=5.0)
    missing = rule.evaluate(_snapshot(observation=make_observation(precipitation_mm=None)), None)
    assert missing.triggered is False
    zero = rule.evaluate(_snapshot(observation=make_observation(precipitation_mm=0.0)), None)
    assert zero.triggered is False
    heavy = rule.evaluate(_snapshot(observation=make_observation(precipitation_mm=12.0)), None)
    assert heavy.triggered is True
    assert "Mangrove Run" in heavy.recommended_action


def test_lightning_hold_and_clearance() -> None:
    hold = LightningHoldRule(
        attraction_ids=("mangrove-run", "cypress-coil"),
        hold_radius_miles=10.0,
        clearance_minutes=30,
    )
    clearance = LightningClearanceRule(
        attraction_ids=("mangrove-run", "cypress-coil"),
        hold_radius_miles=10.0,
        clearance_minutes=30,
    )
    strike = LightningObservation(
        observation_id="sim-1",
        provider="simulated",
        distance_miles=5.0,
        bearing_degrees=245.0,
        observed_at=NOW,
        received_at=NOW,
        scenario=LightningScenario.HOLD_CONDITIONS,
    )
    engine = RecommendationEngine([hold, clearance])
    held = engine.evaluate(_snapshot(lightning=strike), [])
    active = [item for item in held if item.status is RecommendationStatus.ACTIVE]
    assert any(item.rule_id == hold.rule_id for item in active)
    assert not any(
        item.rule_id == clearance.rule_id and item.status is RecommendationStatus.ACTIVE
        for item in held
    )

    later = NOW + timedelta(minutes=31)
    aged = LightningObservation(
        observation_id="sim-1",
        provider="simulated",
        distance_miles=5.0,
        bearing_degrees=245.0,
        observed_at=NOW,
        received_at=later,
        scenario=LightningScenario.CLEARANCE_PERIOD,
    )
    snapshot = _snapshot(evaluated_at=later, lightning=aged)
    updated = engine.evaluate(snapshot, held)
    statuses = {item.rule_id: item.status for item in updated}
    assert statuses[hold.rule_id] is RecommendationStatus.CLEARED
    assert statuses[clearance.rule_id] is RecommendationStatus.ACTIVE


def test_identical_input_is_deterministic() -> None:
    rule = HighWindRule(attraction_id="cypress-coil", hold_mps=15.0, clear_mps=12.0)
    engine = RecommendationEngine([rule])
    snapshot = _snapshot(observation=make_observation(wind_gust_mps=20.0))
    first = engine.evaluate(snapshot, [])
    second = engine.evaluate(snapshot, [])
    assert first[0].summary == second[0].summary
    assert first[0].evidence == second[0].evidence
