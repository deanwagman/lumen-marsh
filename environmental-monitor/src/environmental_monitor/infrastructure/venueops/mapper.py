from __future__ import annotations

from environmental_monitor.domain.recommendation import WeatherRecommendation


def to_venueops_payload(recommendation: WeatherRecommendation) -> dict[str, object]:
    return {
        "recommendationId": recommendation.id,
        "ruleId": recommendation.rule_id,
        "status": recommendation.status.value,
        "severity": recommendation.severity.value,
        "summary": recommendation.summary,
        "evidence": recommendation.evidence,
        "recommendedAction": recommendation.recommended_action,
        "affectedAttractionIds": list(recommendation.affected_attraction_ids),
        "observedAt": recommendation.updated_at.isoformat(),
        "version": recommendation.version,
    }
