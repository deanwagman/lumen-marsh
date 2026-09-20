from __future__ import annotations

from datetime import UTC, datetime, timedelta

import pytest

from park_flow_intelligence.domain.models import QueueSample
from park_flow_intelligence.forecasting.engine import (
    ewma,
    forecast_attraction,
    predicted_queue_length,
    predicted_wait_minutes,
)


def sample(
    *,
    arrivals: int,
    boarded: int,
    queue: int = 180,
    observed_at: datetime | None = None,
    observation_suffix: str = "1",
) -> QueueSample:
    from uuid import uuid4

    return QueueSample(
        attraction_id="mangrove-run",
        observation_id=uuid4(),
        observed_at=observed_at or datetime(2026, 9, 15, 18, 30, tzinfo=UTC),
        window_seconds=60,
        queue_length=queue,
        arrivals=arrivals,
        boarded=boarded,
        operating_units=8,
        configured_units=8,
        simulated=True,
    )


def test_ewma_uses_three_sample_weights() -> None:
    assert ewma(10.0, 8.0, 6.0) == pytest.approx(8.6)


def test_ewma_two_samples_matches_java() -> None:
    expected = (0.50 / 0.80) * 10.0 + (0.30 / 0.80) * 8.0
    assert ewma(10.0, 8.0, None) == expected


def test_predicted_queue_and_wait_are_deterministic() -> None:
    queue = predicted_queue_length(
        current_queue=180,
        arrival_rate=16.0,
        throughput_rate=12.0,
        horizon_minutes=15,
        disruption_transfer=0,
    )
    assert queue == 240
    assert predicted_wait_minutes(queue, 12.0) == 20


def test_forecast_horizons_are_repeatable() -> None:
    now = datetime(2026, 9, 15, 18, 30, tzinfo=UTC)
    newest = sample(arrivals=16, boarded=12, observed_at=now)
    previous = sample(arrivals=14, boarded=12, observed_at=now - timedelta(seconds=60))
    older = sample(arrivals=12, boarded=12, observed_at=now - timedelta(seconds=120))
    first = forecast_attraction(
        newest=newest,
        previous_newest_first=[previous, older],
        now=now,
        disruption_transfer_by_horizon={15: 0, 30: 0, 60: 0},
        extra_assumptions=[],
    )
    second = forecast_attraction(
        newest=newest,
        previous_newest_first=[previous, older],
        now=now,
        disruption_transfer_by_horizon={15: 0, 30: 0, 60: 0},
        extra_assumptions=[],
    )
    assert [h.horizon_minutes for h in first.horizons] == [15, 30, 60]
    assert [h.predicted_wait_minutes for h in first.horizons] == [
        h.predicted_wait_minutes for h in second.horizons
    ]
    assert first.horizons[0].explanation
    assert first.simulated is True
