from __future__ import annotations

from datetime import UTC, datetime

from environmental_monitor.domain.recommendation import (
    RecommendationSeverity,
    RecommendationStatus,
    WeatherRecommendation,
)

NOW = datetime(2026, 9, 1, 18, 30, tzinfo=UTC)


def test_open_and_update_same_condition_does_not_duplicate() -> None:
    rec = WeatherRecommendation.open(
        rule_id="high-wind-cypress-coil",
        severity=RecommendationSeverity.WARNING,
        summary="Review Cypress Coil for weather hold",
        evidence="gust 16",
        recommended_action="Review Cypress Coil for weather hold",
        affected_attraction_ids=("cypress-coil",),
        now=NOW,
        recommendation_id="rec-1",
    )
    rec.update_active(
        severity=RecommendationSeverity.WARNING,
        summary="Review Cypress Coil for weather hold",
        evidence="gust 16",
        recommended_action="Review Cypress Coil for weather hold",
        affected_attraction_ids=("cypress-coil",),
        now=NOW,
    )
    assert rec.version == 1
    rec.update_active(
        severity=RecommendationSeverity.WARNING,
        summary="Review Cypress Coil for weather hold",
        evidence="gust 18",
        recommended_action="Review Cypress Coil for weather hold",
        affected_attraction_ids=("cypress-coil",),
        now=NOW,
    )
    assert rec.version == 2
    assert rec.status is RecommendationStatus.ACTIVE


def test_clear_transitions_status_and_increments_version() -> None:
    rec = WeatherRecommendation.open(
        rule_id="high-wind-cypress-coil",
        severity=RecommendationSeverity.WARNING,
        summary="hold",
        evidence="gust 16",
        recommended_action="hold",
        affected_attraction_ids=("cypress-coil",),
        now=NOW,
    )
    rec.clear(now=NOW, summary="cleared", evidence="gust 8")
    assert rec.status is RecommendationStatus.CLEARED
    assert rec.version == 2
