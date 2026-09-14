from __future__ import annotations

from datetime import datetime

from environmental_monitor.domain.recommendation import (
    RecommendationSeverity,
    RuleEvaluation,
    WeatherRecommendation,
)
from environmental_monitor.domain.snapshot import DatasetState, WeatherSnapshot


class HighWindRule:
    """Fictional demonstration rule for Cypress Coil wind gusts."""

    rule_id = "high-wind-cypress-coil"

    def __init__(self, *, attraction_id: str, hold_mps: float, clear_mps: float) -> None:
        self._attraction_id = attraction_id
        self._hold_mps = hold_mps
        self._clear_mps = clear_mps

    def evaluate(
        self,
        snapshot: WeatherSnapshot,
        previous: WeatherRecommendation | None,
    ) -> RuleEvaluation:
        now = snapshot.evaluated_at
        gust, source = self._peak_gust(snapshot)
        previously_active = previous is not None

        if gust is None:
            if previous is not None:
                return self._result(
                    now,
                    triggered=True,
                    summary=previous.summary,
                    evidence=(
                        "Wind measurements are missing while a high-wind recommendation is "
                        "active. Missing data is not treated as a safe clearance."
                    ),
                    action=previous.recommended_action,
                    severity=previous.severity,
                )
            return self._result(
                now,
                triggered=False,
                summary="Cypress Coil wind remains below the fictional demonstration threshold",
                evidence="No wind gust measurement is available.",
                action="No Cypress Coil weather-hold review required for wind",
            )

        threshold = self._clear_mps if previously_active else self._hold_mps
        triggered = gust >= threshold
        if triggered:
            return self._result(
                now,
                triggered=True,
                summary="Review Cypress Coil for weather hold",
                evidence=(
                    "Fictional demonstration rule: "
                    f"{source} of {gust:.1f} m/s meets or exceeds the "
                    f"{'clearance' if previously_active else 'hold'} threshold of "
                    f"{threshold:.1f} m/s."
                ),
                action="Review Cypress Coil for weather hold",
                severity=RecommendationSeverity.WARNING,
            )
        return self._result(
            now,
            triggered=False,
            summary="Cypress Coil wind remains below the fictional demonstration threshold",
            evidence=(
                f"Fictional demonstration rule: {source} of {gust:.1f} m/s is below the "
                f"{'clearance' if previously_active else 'hold'} threshold of {threshold:.1f} m/s."
            ),
            action="No Cypress Coil weather-hold review required for wind",
        )

    def _peak_gust(self, snapshot: WeatherSnapshot) -> tuple[float | None, str]:
        candidates: list[tuple[float, str]] = []
        observation = snapshot.usable_observation()
        if observation is not None:
            if observation.wind_gust_mps is not None:
                candidates.append(
                    (observation.wind_gust_mps, f"observed gust at {observation.station_id}")
                )
            elif observation.wind_speed_mps is not None:
                candidates.append(
                    (
                        observation.wind_speed_mps,
                        f"observed wind speed at {observation.station_id}",
                    )
                )

        if snapshot.forecast_health.state not in {
            DatasetState.NOT_YET_LOADED,
            DatasetState.UNAVAILABLE,
        }:
            for period in snapshot.forecast:
                if period.wind_gust_mps is not None:
                    candidates.append((period.wind_gust_mps, "forecast gust"))
                elif period.wind_speed_mps is not None:
                    candidates.append((period.wind_speed_mps, "forecast wind speed"))

        if not candidates:
            return None, "wind"
        peak, source = max(candidates, key=lambda item: item[0])
        return peak, source

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
