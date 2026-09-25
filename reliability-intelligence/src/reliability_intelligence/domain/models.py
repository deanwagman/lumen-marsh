from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
from enum import StrEnum
from uuid import UUID, uuid4


class Scenario(StrEnum):
    NORMAL = "normal"
    CYPRESS_COIL_VIBRATION = "cypress-coil-vibration"
    CLEAR = "clear"


class SignalType(StrEnum):
    VIBRATION = "VIBRATION"


class Severity(StrEnum):
    INFO = "INFO"
    WARNING = "WARNING"
    CRITICAL = "CRITICAL"


@dataclass(frozen=True)
class ReliabilitySample:
    observation_id: str
    observed_at: datetime
    asset_code: str
    signal_type: SignalType
    severity: Severity
    value: float
    unit: str
    evidence: str
    recommended_action: str
    simulated: bool = True


def observation_id_for(prefix: str, observed_at: datetime, cycle: int) -> str:
    stamp = observed_at.strftime("%Y%m%dT%H%M%SZ")
    return f"{prefix}-{stamp}-{cycle:04d}-{uuid4().hex[:8]}"


def new_correlation_id() -> UUID:
    return uuid4()
