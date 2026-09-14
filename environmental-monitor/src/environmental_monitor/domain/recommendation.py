from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from enum import StrEnum
from uuid import uuid4

from environmental_monitor.domain.timeutils import ensure_utc


class RecommendationStatus(StrEnum):
    ACTIVE = "ACTIVE"
    CLEARED = "CLEARED"


class RecommendationSeverity(StrEnum):
    INFO = "INFO"
    WATCH = "WATCH"
    WARNING = "WARNING"


@dataclass(slots=True)
class WeatherRecommendation:
    """An operational recommendation. This service never commands attractions."""

    id: str
    rule_id: str
    status: RecommendationStatus
    severity: RecommendationSeverity
    summary: str
    evidence: str
    recommended_action: str
    affected_attraction_ids: tuple[str, ...]
    created_at: datetime
    updated_at: datetime
    version: int
    last_delivered_version: int = 0

    def __post_init__(self) -> None:
        object.__setattr__(self, "created_at", ensure_utc(self.created_at, "created_at"))
        object.__setattr__(self, "updated_at", ensure_utc(self.updated_at, "updated_at"))
        if self.version < 1:
            raise ValueError("version must start at 1")

    @classmethod
    def open(
        cls,
        *,
        rule_id: str,
        severity: RecommendationSeverity,
        summary: str,
        evidence: str,
        recommended_action: str,
        affected_attraction_ids: tuple[str, ...],
        now: datetime,
        recommendation_id: str | None = None,
    ) -> WeatherRecommendation:
        instant = ensure_utc(now, "now")
        return cls(
            id=recommendation_id or str(uuid4()),
            rule_id=rule_id,
            status=RecommendationStatus.ACTIVE,
            severity=severity,
            summary=summary,
            evidence=evidence,
            recommended_action=recommended_action,
            affected_attraction_ids=affected_attraction_ids,
            created_at=instant,
            updated_at=instant,
            version=1,
        )

    def update_active(
        self,
        *,
        severity: RecommendationSeverity,
        summary: str,
        evidence: str,
        recommended_action: str,
        affected_attraction_ids: tuple[str, ...],
        now: datetime,
    ) -> None:
        if self.status is RecommendationStatus.CLEARED:
            raise ValueError("Cannot update a cleared recommendation; open a new one")
        if (
            self.severity == severity
            and self.summary == summary
            and self.evidence == evidence
            and self.recommended_action == recommended_action
            and self.affected_attraction_ids == affected_attraction_ids
        ):
            self.updated_at = ensure_utc(now, "now")
            return
        self.severity = severity
        self.summary = summary
        self.evidence = evidence
        self.recommended_action = recommended_action
        self.affected_attraction_ids = affected_attraction_ids
        self.updated_at = ensure_utc(now, "now")
        self.version += 1

    def clear(self, *, now: datetime, summary: str, evidence: str) -> None:
        if self.status is RecommendationStatus.CLEARED:
            self.updated_at = ensure_utc(now, "now")
            return
        self.status = RecommendationStatus.CLEARED
        self.summary = summary
        self.evidence = evidence
        self.updated_at = ensure_utc(now, "now")
        self.version += 1

    @property
    def needs_delivery(self) -> bool:
        return self.version > self.last_delivered_version

    def mark_delivered(self, version: int) -> None:
        self.last_delivered_version = max(self.last_delivered_version, version)


@dataclass(frozen=True, slots=True)
class RuleEvaluation:
    rule_id: str
    triggered: bool
    severity: RecommendationSeverity
    summary: str
    evidence: str
    recommended_action: str
    affected_attraction_ids: tuple[str, ...]
    evaluated_at: datetime
    extra: dict[str, str] = field(default_factory=dict)

    def __post_init__(self) -> None:
        object.__setattr__(self, "evaluated_at", ensure_utc(self.evaluated_at, "evaluated_at"))
