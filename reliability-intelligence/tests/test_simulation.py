from __future__ import annotations

from datetime import UTC, datetime

from tests.factory import make_settings

from reliability_intelligence.clock import FrozenClock
from reliability_intelligence.domain.models import Scenario
from reliability_intelligence.simulation.engine import ReliabilitySimulator

NOW = datetime(2026, 9, 20, 18, 30, tzinfo=UTC)


def test_vibration_cycle_posts_simulated_sample() -> None:
    simulator = ReliabilitySimulator(make_settings(), FrozenClock(NOW))
    simulator.apply_scenario(Scenario.CYPRESS_COIL_VIBRATION)
    sample = simulator.next_cycle()
    assert sample is not None
    assert sample.simulated is True
    assert sample.asset_code == "CC-TRAIN-01-WHEEL-A"
    assert sample.severity.value == "WARNING"
    assert "Fictional simulated vibration" in sample.evidence


def test_clear_and_normal_do_not_emit_samples() -> None:
    simulator = ReliabilitySimulator(make_settings(), FrozenClock(NOW))
    assert simulator.next_cycle() is None
    simulator.apply_scenario(Scenario.CLEAR)
    assert simulator.next_cycle() is None


def test_vibration_episode_emits_once_until_reactivated() -> None:
    simulator = ReliabilitySimulator(make_settings(), FrozenClock(NOW))
    simulator.apply_scenario(Scenario.CYPRESS_COIL_VIBRATION)
    first = simulator.next_cycle()
    second = simulator.next_cycle()
    assert first is not None
    assert second is None

    simulator.apply_scenario(Scenario.CYPRESS_COIL_VIBRATION)
    assert simulator.next_cycle() is None

    simulator.apply_scenario(Scenario.CLEAR)
    simulator.apply_scenario(Scenario.CYPRESS_COIL_VIBRATION)
    reopened = simulator.next_cycle()
    assert reopened is not None
    assert reopened.observation_id != first.observation_id
