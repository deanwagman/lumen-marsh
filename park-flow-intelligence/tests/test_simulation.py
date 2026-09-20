from __future__ import annotations

from datetime import UTC, datetime

from park_flow_intelligence.clock import FrozenClock
from park_flow_intelligence.domain.models import Scenario
from park_flow_intelligence.simulation.engine import FlowSimulator
from tests.factory import make_settings

NOW = datetime(2026, 9, 15, 18, 30, tzinfo=UTC)


def test_fixed_seed_repeats_observations() -> None:
    settings = make_settings()
    first = FlowSimulator(settings, FrozenClock(NOW))
    second = FlowSimulator(settings, FrozenClock(NOW))
    first_samples, first_forecasts, _ = first.next_cycle()
    second_samples, second_forecasts, _ = second.next_cycle()
    assert [sample.observation_id for sample in first_samples] == [
        sample.observation_id for sample in second_samples
    ]
    assert [bundle.horizons[1].predicted_wait_minutes for bundle in first_forecasts.values()] == [
        bundle.horizons[1].predicted_wait_minutes for bundle in second_forecasts.values()
    ]


def test_disruption_reduces_mangrove_capacity() -> None:
    simulator = FlowSimulator(make_settings(), FrozenClock(NOW))
    simulator.apply_scenario(Scenario.MANGROVE_DISRUPTION)
    samples, bundles, recommendation = simulator.next_cycle()
    mangrove = next(sample for sample in samples if sample.attraction_id == "mangrove-run")
    cypress = next(sample for sample in samples if sample.attraction_id == "cypress-coil")
    assert mangrove.operating_units == 0
    assert mangrove.boarded == 0
    assert cypress.arrivals > 10
    assert recommendation is not None
    assert recommendation.type.value == "GUEST_REDIRECTION"
    assert bundles["mangrove-run"].simulated is True
    assert any("unavailable" in note.lower() for note in bundles["cypress-coil"].assumptions)
