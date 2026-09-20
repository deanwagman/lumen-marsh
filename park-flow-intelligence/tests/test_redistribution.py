from __future__ import annotations

import pytest

from park_flow_intelligence.forecasting.redistribution import (
    displaced_arrivals_per_minute,
    redistribution_assumptions,
    transfer_by_horizon,
)
from tests.factory import make_settings


def test_mangrove_disruption_splits_demand() -> None:
    settings = make_settings()
    displaced = displaced_arrivals_per_minute(expected_arrivals_per_minute=24.0, settings=settings)
    assert displaced == pytest.approx(19.2)
    cypress = transfer_by_horizon(
        destination_id=settings.cypress_coil_id,
        displaced_per_minute=displaced,
        settings=settings,
    )
    stormglass = transfer_by_horizon(
        destination_id=settings.stormglass_station_id,
        displaced_per_minute=displaced,
        settings=settings,
    )
    mangrove = transfer_by_horizon(
        destination_id=settings.mangrove_run_id,
        displaced_per_minute=displaced,
        settings=settings,
    )
    assert cypress[15] == round(19.2 * 0.50 * 15)
    assert stormglass[15] == round(19.2 * 0.30 * 15)
    assert mangrove[15] == 0
    notes = redistribution_assumptions(
        source_id="mangrove-run",
        displaced_per_minute=displaced,
        settings=settings,
    )
    assert any("50%" in note for note in notes)
    assert any("Other experiences: 20%" in note for note in notes)
