from __future__ import annotations

from datetime import datetime

from environmental_monitor.domain.recommendation import (
    RecommendationSeverity,
    RuleEvaluation,
    WeatherRecommendation,
)
from environmental_monitor.domain.snapshot import DatasetState, WeatherSnapshot

RELEVANT_WARNING_EVENTS = frozenset(
    {
        "tornado warning",
        "severe thunderstorm warning",
        "flash flood warning",
        "hurricane warning",
        "tropical storm warning",
        "extreme wind warning",
        "storm surge warning",
        "special marine warning",
    }
)


class ActiveAlertRule:
    """Fictional demonstration rule: official NWS warnings require operator review."""

    rule_id = "nws-severe-weather-alert"

    def __init__(self, attraction_ids: tuple[str, ...]) -> None:
        self._attraction_ids = attraction_ids

    def evaluate(
        self,
        snapshot: WeatherSnapshot,
        previous: WeatherRecommendation | None,
    ) -> RuleEvaluation:
        now = snapshot.evaluated_at
        if snapshot.alert_health.state is DatasetState.NOT_YET_LOADED:
            return self._result(
                now,
                triggered=False,
                summary="No relevant official weather warning is active",
                evidence="Alert dataset has not loaded; missing data is not treated as safe.",
                action="No weather-hold review required for official alerts",
            )
        if snapshot.alert_health.state is DatasetState.UNAVAILABLE:
            if previous is not None:
                return self._result(
                    now,
                    triggered=True,
                    summary=previous.summary,
                    evidence=(
                        "Official alerts became unavailable while this recommendation was "
                        "active. The recommendation remains until alerts can be confirmed clear."
                    ),
                    action=previous.recommended_action,
                    severity=previous.severity,
                    attraction_ids=previous.affected_attraction_ids,
                )
            return self._result(
                now,
                triggered=False,
                summary="No relevant official weather warning is active",
                evidence=(
                    "Official alerts are unavailable; missing data is not treated as a "
                    "clear condition."
                ),
                action="No weather-hold review required for official alerts",
            )

        relevant = [
            alert
            for alert in snapshot.alerts
            if alert.event.lower() in RELEVANT_WARNING_EVENTS and not alert.is_expired(now)
        ]
        if relevant:
            names = ", ".join(sorted({alert.event for alert in relevant}))
            headlines = "; ".join(alert.headline or alert.event for alert in relevant)
            return self._result(
                now,
                triggered=True,
                summary="Review outdoor attractions for weather hold",
                evidence=(
                    "Fictional demonstration rule: relevant National Weather Service warning "
                    f"is active ({names}). {headlines}"
                ),
                action="Review outdoor attractions for weather hold",
                severity=RecommendationSeverity.WARNING,
            )
        return self._result(
            now,
            triggered=False,
            summary="No relevant official weather warning is active",
            evidence=(
                "Fictional demonstration rule: no relevant National Weather Service warning "
                "is currently active."
            ),
            action="No weather-hold review required for official alerts",
        )

    def _result(
        self,
        now: datetime,
        *,
        triggered: bool,
        summary: str,
        evidence: str,
        action: str,
        severity: RecommendationSeverity = RecommendationSeverity.INFO,
        attraction_ids: tuple[str, ...] | None = None,
    ) -> RuleEvaluation:
        return RuleEvaluation(
            rule_id=self.rule_id,
            triggered=triggered,
            severity=severity,
            summary=summary,
            evidence=evidence,
            recommended_action=action,
            affected_attraction_ids=attraction_ids or self._attraction_ids,
            evaluated_at=now,
        )
