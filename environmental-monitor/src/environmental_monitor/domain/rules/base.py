from __future__ import annotations

from typing import Protocol

from environmental_monitor.domain.recommendation import RuleEvaluation, WeatherRecommendation
from environmental_monitor.domain.snapshot import WeatherSnapshot


class WeatherRule(Protocol):
    """A fictional demonstration rule. Rules never call attraction commands."""

    rule_id: str

    def evaluate(
        self,
        snapshot: WeatherSnapshot,
        previous: WeatherRecommendation | None,
    ) -> RuleEvaluation: ...
