from __future__ import annotations

import itertools
import math
from datetime import datetime

from park_flow_intelligence.domain.models import (
    HORIZONS,
    MIN_THROUGHPUT_PER_MINUTE,
    NEWEST_WEIGHT,
    OLDER_WEIGHT,
    PREVIOUS_WEIGHT,
    Confidence,
    ForecastBundle,
    HorizonForecast,
    QueueSample,
    classify_confidence,
    forecast_id_for,
    freshness_for,
)


def ewma(newest: float, previous: float | None, older: float | None) -> float:
    if previous is None:
        return newest
    if older is None:
        total = NEWEST_WEIGHT + PREVIOUS_WEIGHT
        return (NEWEST_WEIGHT / total) * newest + (PREVIOUS_WEIGHT / total) * previous
    return NEWEST_WEIGHT * newest + PREVIOUS_WEIGHT * previous + OLDER_WEIGHT * older


def predicted_queue_length(
    *,
    current_queue: int,
    arrival_rate: float,
    throughput_rate: float,
    horizon_minutes: int,
    disruption_transfer: int,
) -> int:
    predicted = (
        current_queue
        + arrival_rate * horizon_minutes
        - throughput_rate * horizon_minutes
        + disruption_transfer
    )
    return int(max(0, round(predicted)))


def predicted_wait_minutes(predicted_queue: int, throughput_rate: float) -> int:
    rate = max(throughput_rate, MIN_THROUGHPUT_PER_MINUTE)
    return math.ceil(max(0, predicted_queue) / rate)


def intervals_are_stable(samples_newest_first: list[QueueSample], window_seconds: int) -> bool:
    if len(samples_newest_first) < 3:
        return False
    expected = float(window_seconds)
    for newer, older in itertools.pairwise(samples_newest_first):
        gap = abs((newer.observed_at - older.observed_at).total_seconds())
        if abs(gap - expected) > 15:
            return False
    return True


def forecast_attraction(
    *,
    newest: QueueSample,
    previous_newest_first: list[QueueSample],
    now: datetime,
    disruption_transfer_by_horizon: dict[int, int],
    extra_assumptions: list[str],
) -> ForecastBundle:
    previous = previous_newest_first[0] if previous_newest_first else None
    older = previous_newest_first[1] if len(previous_newest_first) > 1 else None
    arrival_rate = ewma(
        newest.arrivals_per_minute,
        None if previous is None else previous.arrivals_per_minute,
        None if older is None else older.arrivals_per_minute,
    )
    throughput_rate = ewma(
        newest.throughput_per_minute,
        None if previous is None else previous.throughput_per_minute,
        None if older is None else older.throughput_per_minute,
    )
    age_seconds = max(0.0, (now - newest.observed_at).total_seconds())
    freshness = freshness_for(age_seconds)
    samples = [newest, *previous_newest_first]
    confidence = classify_confidence(
        fresh_sample_count=len(samples),
        freshness=freshness,
        intervals_stable=intervals_are_stable(samples, newest.window_seconds),
    )
    assumptions = [
        f"Observation age {int(age_seconds)} seconds ({freshness.value}).",
        f"Smoothed arrivals {arrival_rate:.2f} guests/minute.",
        f"Smoothed throughput {throughput_rate:.2f} guests/minute.",
        f"Operating {newest.operating_units} of {newest.configured_units} units.",
        "Deterministic EWMA forecast; no machine learning.",
        "Simulated demonstration data.",
        *extra_assumptions,
    ]
    effective_confidence = confidence or Confidence.LOW
    horizons = tuple(
        _horizon(
            newest=newest,
            arrival_rate=arrival_rate,
            throughput_rate=throughput_rate,
            horizon_minutes=horizon,
            disruption_transfer=disruption_transfer_by_horizon.get(horizon, 0),
            confidence=effective_confidence,
            assumptions=assumptions,
        )
        for horizon in HORIZONS
    )
    return ForecastBundle(
        attraction_id=newest.attraction_id,
        generated_at=now,
        based_on_observation_id=newest.observation_id,
        simulated=newest.simulated,
        horizons=horizons,  # type: ignore[arg-type]
        arrival_rate=arrival_rate,
        throughput_rate=throughput_rate,
        freshness=freshness,
        confidence=confidence,
        assumptions=assumptions,
    )


def _horizon(
    *,
    newest: QueueSample,
    arrival_rate: float,
    throughput_rate: float,
    horizon_minutes: int,
    disruption_transfer: int,
    confidence: Confidence,
    assumptions: list[str],
) -> HorizonForecast:
    queue = predicted_queue_length(
        current_queue=newest.queue_length,
        arrival_rate=arrival_rate,
        throughput_rate=throughput_rate,
        horizon_minutes=horizon_minutes,
        disruption_transfer=disruption_transfer,
    )
    wait = predicted_wait_minutes(queue, throughput_rate)
    explanation = (
        f"{newest.attraction_id} wait in {horizon_minutes} minutes is about {wait} minutes "
        f"from queue {newest.queue_length}, arrivals {arrival_rate:.1f}/min, and throughput "
        f"{throughput_rate:.1f}/min."
    )
    if disruption_transfer:
        explanation += (
            f" Includes {disruption_transfer} transferred guests from disruption modeling."
        )
    return HorizonForecast(
        forecast_id=forecast_id_for(
            attraction_id=newest.attraction_id,
            observation_id=newest.observation_id,
            horizon_minutes=horizon_minutes,
        ),
        horizon_minutes=horizon_minutes,
        predicted_queue_length=queue,
        predicted_wait_minutes=wait,
        confidence=confidence,
        assumptions=assumptions,
        explanation=explanation,
    )
