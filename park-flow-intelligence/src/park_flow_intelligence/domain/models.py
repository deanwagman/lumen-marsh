from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
from enum import StrEnum
from uuid import UUID, uuid5

OBSERVATION_NAMESPACE = UUID("7e2c1a90-4d3b-4f5e-9c8a-1b2d3e4f5a60")
FORECAST_NAMESPACE = UUID("8f3d2b01-5e4c-406f-ad9b-2c3e4f5a6172")
RECOMMENDATION_NAMESPACE = UUID("9a4e3c12-6f5d-4170-be0c-3d4f5a617283")

HORIZONS = (15, 30, 60)
MIN_THROUGHPUT_PER_MINUTE = 0.5
NEWEST_WEIGHT = 0.50
PREVIOUS_WEIGHT = 0.30
OLDER_WEIGHT = 0.20
FRESH_SECONDS = 120
DELAYED_SECONDS = 300


class Scenario(StrEnum):
    NORMAL = "normal"
    MANGROVE_DISRUPTION = "mangrove-disruption"
    CLEAR = "clear"


class Freshness(StrEnum):
    FRESH = "FRESH"
    DELAYED = "DELAYED"
    STALE = "STALE"


class Confidence(StrEnum):
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"


class RecommendationType(StrEnum):
    CONGESTION_EXPECTED = "CONGESTION_EXPECTED"
    DEMAND_SHIFT_EXPECTED = "DEMAND_SHIFT_EXPECTED"
    CAPACITY_REVIEW = "CAPACITY_REVIEW"
    POSTED_WAIT_REVIEW = "POSTED_WAIT_REVIEW"
    GUEST_REDIRECTION = "GUEST_REDIRECTION"
    DATA_QUALITY = "DATA_QUALITY"


class RecommendationSeverity(StrEnum):
    INFO = "INFO"
    WARNING = "WARNING"
    CRITICAL = "CRITICAL"


@dataclass(frozen=True)
class QueueSample:
    attraction_id: str
    observation_id: UUID
    observed_at: datetime
    window_seconds: int
    queue_length: int
    arrivals: int
    boarded: int
    operating_units: int
    configured_units: int
    simulated: bool = True

    @property
    def arrivals_per_minute(self) -> float:
        return self.arrivals * 60.0 / self.window_seconds

    @property
    def throughput_per_minute(self) -> float:
        return self.boarded * 60.0 / self.window_seconds


@dataclass(frozen=True)
class HorizonForecast:
    forecast_id: UUID
    horizon_minutes: int
    predicted_queue_length: int
    predicted_wait_minutes: int
    confidence: Confidence
    assumptions: list[str]
    explanation: str


@dataclass(frozen=True)
class ForecastBundle:
    attraction_id: str
    generated_at: datetime
    based_on_observation_id: UUID
    simulated: bool
    horizons: tuple[HorizonForecast, HorizonForecast, HorizonForecast]
    arrival_rate: float
    throughput_rate: float
    freshness: Freshness
    confidence: Confidence | None
    assumptions: list[str]


@dataclass(frozen=True)
class RecommendationProposal:
    recommendation_id: UUID
    type: RecommendationType
    severity: RecommendationSeverity
    source_attraction_id: str
    affected_attraction_ids: list[str]
    recommended_destination_ids: list[str]
    summary: str
    explanation: str
    guest_message: str
    expires_at: datetime


def observation_id_for(*, attraction_id: str, cycle_key: str) -> UUID:
    return uuid5(OBSERVATION_NAMESPACE, f"{attraction_id}:{cycle_key}")


def forecast_id_for(*, attraction_id: str, observation_id: UUID, horizon_minutes: int) -> UUID:
    return uuid5(FORECAST_NAMESPACE, f"{attraction_id}:{observation_id}:{horizon_minutes}")


def recommendation_id_for(*, scenario: Scenario, cycle_key: str) -> UUID:
    return uuid5(RECOMMENDATION_NAMESPACE, f"{scenario}:{cycle_key}")


def freshness_for(age_seconds: float) -> Freshness:
    if age_seconds <= FRESH_SECONDS:
        return Freshness.FRESH
    if age_seconds <= DELAYED_SECONDS:
        return Freshness.DELAYED
    return Freshness.STALE


def classify_confidence(
    *,
    fresh_sample_count: int,
    freshness: Freshness,
    intervals_stable: bool,
) -> Confidence | None:
    if freshness is Freshness.STALE:
        return None
    if freshness is Freshness.DELAYED or fresh_sample_count < 3:
        return Confidence.LOW
    if fresh_sample_count >= 6 and intervals_stable:
        return Confidence.HIGH
    return Confidence.MEDIUM
