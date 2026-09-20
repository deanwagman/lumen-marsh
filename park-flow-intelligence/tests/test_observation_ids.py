from __future__ import annotations

from park_flow_intelligence.domain.models import (
    Scenario,
    forecast_id_for,
    observation_id_for,
    recommendation_id_for,
)


def test_observation_identifiers_are_idempotent() -> None:
    first = observation_id_for(attraction_id="mangrove-run", cycle_key="normal:1")
    second = observation_id_for(attraction_id="mangrove-run", cycle_key="normal:1")
    other = observation_id_for(attraction_id="cypress-coil", cycle_key="normal:1")
    assert first == second
    assert first != other


def test_forecast_and_recommendation_ids_are_stable() -> None:
    observation = observation_id_for(attraction_id="mangrove-run", cycle_key="normal:1")
    assert forecast_id_for(
        attraction_id="mangrove-run",
        observation_id=observation,
        horizon_minutes=15,
    ) == forecast_id_for(
        attraction_id="mangrove-run",
        observation_id=observation,
        horizon_minutes=15,
    )
    assert recommendation_id_for(
        scenario=Scenario.MANGROVE_DISRUPTION, cycle_key="active"
    ) == recommendation_id_for(
        scenario=Scenario.MANGROVE_DISRUPTION,
        cycle_key="active",
    )
