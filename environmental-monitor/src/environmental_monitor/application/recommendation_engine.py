from __future__ import annotations

from environmental_monitor.domain.recommendation import WeatherRecommendation
from environmental_monitor.domain.rules.base import WeatherRule
from environmental_monitor.domain.rules.lightning_rule import LightningHoldRule
from environmental_monitor.domain.snapshot import WeatherSnapshot


class RecommendationEngine:
    """Applies fictional demonstration rules. Never sends attraction commands."""

    def __init__(self, rules: list[WeatherRule]) -> None:
        self._rules = rules

    def evaluate(
        self,
        snapshot: WeatherSnapshot,
        existing: list[WeatherRecommendation],
    ) -> list[WeatherRecommendation]:
        snapshot.prior_recommendations = tuple(existing)
        active = {
            recommendation.rule_id: recommendation
            for recommendation in existing
            if recommendation.status.value == "ACTIVE"
        }
        changed: list[WeatherRecommendation] = []

        hold_rule_id = LightningHoldRule.rule_id
        ordered_rules = sorted(
            self._rules,
            key=lambda rule: 0 if rule.rule_id == hold_rule_id else 1,
        )

        for rule in ordered_rules:
            previous = active.get(rule.rule_id)
            evaluation = rule.evaluate(snapshot, previous)
            if evaluation.triggered:
                if previous is None:
                    opened = WeatherRecommendation.open(
                        rule_id=evaluation.rule_id,
                        severity=evaluation.severity,
                        summary=evaluation.summary,
                        evidence=evaluation.evidence,
                        recommended_action=evaluation.recommended_action,
                        affected_attraction_ids=evaluation.affected_attraction_ids,
                        now=evaluation.evaluated_at,
                    )
                    active[rule.rule_id] = opened
                    changed.append(opened)
                else:
                    version_before = previous.version
                    previous.update_active(
                        severity=evaluation.severity,
                        summary=evaluation.summary,
                        evidence=evaluation.evidence,
                        recommended_action=evaluation.recommended_action,
                        affected_attraction_ids=evaluation.affected_attraction_ids,
                        now=evaluation.evaluated_at,
                    )
                    if previous.version != version_before or previous not in changed:
                        changed.append(previous)
            elif previous is not None:
                previous.clear(
                    now=evaluation.evaluated_at,
                    summary=evaluation.summary,
                    evidence=evaluation.evidence,
                )
                changed.append(previous)
                del active[rule.rule_id]
            snapshot.prior_recommendations = tuple(existing)
        return changed
