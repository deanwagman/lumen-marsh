from __future__ import annotations

from datetime import datetime

from environmental_monitor.domain.recommendation import (
    RecommendationSeverity,
    RuleEvaluation,
    WeatherRecommendation,
)
from environmental_monitor.domain.snapshot import WeatherSnapshot


class HeavyRainRule:
    """Fictional demonstration rule for Mangrove Run rainfall."""

    rule_id = "heavy-rain-mangrove-run"

    def __init__(self, *, attraction_id: str, hold_mm: float, clear_mm: float) -> None:
        self._attraction_id = attraction_id
        self._hold_mm = hold_mm
        self._clear_mm = clear_mm

    def evaluate(
        self,
        snapshot: WeatherSnapshot,
        previous: WeatherRecommendation | None,
    ) -> RuleEvaluation:
        now = snapshot.evaluated_at
        observation = snapshot.usable_observation()
        rainfall = observation.precipitation_mm if observation is not None else None
        previously_active = previous is not None
        station = observation.station_id if observation is not None else "unknown station"

        if rainfall is None:
            if previous is not None:
                return self._result(
                    now,
                    triggered=True,
                    summary=previous.summary,
                    evidence=(
                        "Rainfall measurements are missing while a heavy-rain recommendation "
                        "is active. Missing data is not treated as a safe clearance."
                    ),
                    action=previous.recommended_action,
                    severity=previous.severity,
                )
            return self._result(
                now,
                triggered=False,
                summary="Mangrove Run rainfall remains below the fictional demonstration threshold",
                evidence="No precipitation measurement is available.",
                action="No Mangrove Run water-level inspection required",
            )

        threshold = self._clear_mm if previously_active else self._hold_mm
        triggered = rainfall >= threshold
        if triggered:
            return self._result(
                now,
                triggered=True,
                summary="Inspect Mangrove Run water-level conditions",
                evidence=(
                    "Fictional demonstration rule: observed precipitation of "
                    f"{rainfall:.1f} mm at {station} meets or exceeds the "
                    f"{'clearance' if previously_active else 'hold'} threshold of "
                    f"{threshold:.1f} mm."
                ),
                action="Inspect Mangrove Run water-level conditions",
                severity=RecommendationSeverity.WATCH,
            )
        return self._result(
            now,
            triggered=False,
            summary="Mangrove Run rainfall remains below the fictional demonstration threshold",
            evidence=(
                f"Fictional demonstration rule: observed precipitation of {rainfall:.1f} mm "
                f"at {station} is below the "
                f"{'clearance' if previously_active else 'hold'} threshold of {threshold:.1f} mm."
            ),
            action="No Mangrove Run water-level inspection required",
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
    ) -> RuleEvaluation:
        return RuleEvaluation(
            rule_id=self.rule_id,
            triggered=triggered,
            severity=severity,
            summary=summary,
            evidence=evidence,
            recommended_action=action,
            affected_attraction_ids=(self._attraction_id,),
            evaluated_at=now,
        )
