from __future__ import annotations

from datetime import datetime, timedelta

from environmental_monitor.domain.lightning import LightningObservation
from environmental_monitor.domain.recommendation import (
    RecommendationSeverity,
    RuleEvaluation,
    WeatherRecommendation,
)
from environmental_monitor.domain.snapshot import WeatherSnapshot


class LightningHoldRule:
    """Fictional demonstration rule: nearby simulated lightning requires a weather hold."""

    rule_id = "simulated-lightning-hold"

    def __init__(
        self,
        *,
        attraction_ids: tuple[str, ...],
        hold_radius_miles: float,
        clearance_minutes: int,
    ) -> None:
        self._attraction_ids = attraction_ids
        self._hold_radius_miles = hold_radius_miles
        self._clearance = timedelta(minutes=clearance_minutes)

    def evaluate(
        self,
        snapshot: WeatherSnapshot,
        previous: WeatherRecommendation | None,
    ) -> RuleEvaluation:
        now = snapshot.evaluated_at
        strike = snapshot.lightning
        in_hold = _strike_requires_hold(strike, now, self._hold_radius_miles, self._clearance)
        if in_hold and strike is not None:
            return RuleEvaluation(
                rule_id=self.rule_id,
                triggered=True,
                severity=RecommendationSeverity.WARNING,
                summary="Place Mangrove Run and Cypress Coil on weather hold",
                evidence=_hold_evidence(strike, self._hold_radius_miles, self._clearance, now),
                recommended_action="Place Mangrove Run and Cypress Coil on weather hold",
                affected_attraction_ids=self._attraction_ids,
                evaluated_at=now,
            )
        return RuleEvaluation(
            rule_id=self.rule_id,
            triggered=False,
            severity=RecommendationSeverity.INFO,
            summary="Simulated lightning is outside the hold radius",
            evidence=_clear_evidence(strike, self._hold_radius_miles, previous is not None),
            recommended_action="No lightning weather hold is required",
            affected_attraction_ids=self._attraction_ids,
            evaluated_at=now,
        )


class LightningClearanceRule:
    """Fictional demonstration rule: recommend return-to-service review after clearance."""

    rule_id = "simulated-lightning-clearance"

    def __init__(
        self,
        *,
        attraction_ids: tuple[str, ...],
        hold_radius_miles: float,
        clearance_minutes: int,
    ) -> None:
        self._attraction_ids = attraction_ids
        self._hold_radius_miles = hold_radius_miles
        self._clearance = timedelta(minutes=clearance_minutes)

    def evaluate(
        self,
        snapshot: WeatherSnapshot,
        previous: WeatherRecommendation | None,
    ) -> RuleEvaluation:
        now = snapshot.evaluated_at
        strike = snapshot.lightning
        in_hold = _strike_requires_hold(strike, now, self._hold_radius_miles, self._clearance)
        hold_recommendation = next(
            (
                recommendation
                for recommendation in snapshot.prior_recommendations
                if recommendation.rule_id == LightningHoldRule.rule_id
            ),
            None,
        )
        hold_was_active = hold_recommendation is not None or previous is not None
        if in_hold:
            return RuleEvaluation(
                rule_id=self.rule_id,
                triggered=False,
                severity=RecommendationSeverity.INFO,
                summary="Return-to-service review is not yet appropriate",
                evidence=(
                    "Fictional demonstration rule: simulated lightning remains inside the "
                    "configured hold radius or clearance period."
                ),
                recommended_action="Keep held attractions on weather hold",
                affected_attraction_ids=self._attraction_ids,
                evaluated_at=now,
            )
        if hold_was_active and strike is not None:
            elapsed = now - strike.observed_at
            return RuleEvaluation(
                rule_id=self.rule_id,
                triggered=True,
                severity=RecommendationSeverity.INFO,
                summary="Review held attractions for return to service",
                evidence=(
                    "Fictional demonstration rule: simulated lightning area has remained "
                    f"clear of strikes inside {self._hold_radius_miles:.1f} miles for "
                    f"{elapsed.total_seconds() / 60:.0f} minutes "
                    f"(clearance period {int(self._clearance.total_seconds() // 60)} minutes). "
                    "Simulated evidence is not an NWS observation."
                ),
                recommended_action="Review held attractions for return to service",
                affected_attraction_ids=self._attraction_ids,
                evaluated_at=now,
            )
        if previous is not None and strike is None:
            return RuleEvaluation(
                rule_id=self.rule_id,
                triggered=False,
                severity=RecommendationSeverity.INFO,
                summary="Simulator restored to clear conditions",
                evidence=(
                    "Fictional demonstration rule: lightning simulator was restored to clear "
                    "conditions. Simulated evidence is not an NWS observation."
                ),
                recommended_action="No return-to-service review pending from lightning",
                affected_attraction_ids=self._attraction_ids,
                evaluated_at=now,
            )
        return RuleEvaluation(
            rule_id=self.rule_id,
            triggered=False,
            severity=RecommendationSeverity.INFO,
            summary="No lightning return-to-service review is pending",
            evidence="No simulated lightning hold is in effect.",
            recommended_action="No return-to-service review pending from lightning",
            affected_attraction_ids=self._attraction_ids,
            evaluated_at=now,
        )


def _strike_requires_hold(
    strike: LightningObservation | None,
    now: datetime,
    radius_miles: float,
    clearance: timedelta,
) -> bool:
    if strike is None:
        return False
    if strike.distance_miles <= radius_miles:
        age = now - strike.observed_at
        return age < clearance
    return False


def _hold_evidence(
    strike: LightningObservation,
    radius_miles: float,
    clearance: timedelta,
    now: datetime,
) -> str:
    age_minutes = (now - strike.observed_at).total_seconds() / 60.0
    return (
        "Fictional demonstration rule: simulated lightning observed "
        f"{strike.distance_miles:.1f} miles from Stormglass Station at bearing "
        f"{strike.bearing_degrees:.0f}° at {strike.observed_at.isoformat()}. "
        f"Distance is inside the {radius_miles:.1f}-mile hold radius. "
        f"Age {age_minutes:.0f} minutes; clearance period "
        f"{int(clearance.total_seconds() // 60)} minutes. "
        "This observation is simulated and is not National Weather Service data."
    )


def _clear_evidence(
    strike: LightningObservation | None,
    radius_miles: float,
    previously_active: bool,
) -> str:
    if strike is None:
        return (
            "Fictional demonstration rule: no simulated lightning observation is present. "
            "Simulated evidence is not an NWS observation."
        )
    qualifier = "after a previous hold " if previously_active else ""
    return (
        f"Fictional demonstration rule: {qualifier}simulated lightning at "
        f"{strike.distance_miles:.1f} miles is outside the {radius_miles:.1f}-mile hold "
        "radius or older than the clearance period. "
        "This observation is simulated and is not National Weather Service data."
    )
