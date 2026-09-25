from __future__ import annotations

from reliability_intelligence.clock import Clock
from reliability_intelligence.domain.models import (
    ReliabilitySample,
    Scenario,
    Severity,
    SignalType,
    observation_id_for,
)
from reliability_intelligence.settings import Settings

_EVIDENCE = (
    "Fictional simulated vibration exceeded the demonstration threshold "
    "for three consecutive samples."
)
_ACTION = "Inspect the wheel assembly and consider reduced-capacity operation."


class ReliabilitySimulator:
    def __init__(self, settings: Settings, clock: Clock) -> None:
        self.settings = settings
        self.clock = clock
        self.scenario = Scenario.NORMAL
        self.running = False
        self.cycle = 0
        self.last_observation_id: str | None = None

    def start(self) -> None:
        self.running = True

    def stop(self) -> None:
        self.running = False

    def apply_scenario(self, scenario: Scenario) -> None:
        self.scenario = scenario

    def next_cycle(self) -> ReliabilitySample | None:
        self.cycle += 1
        if self.scenario != Scenario.CYPRESS_COIL_VIBRATION:
            self.last_observation_id = None
            return None
        observed_at = self.clock.now()
        sample = ReliabilitySample(
            observation_id=observation_id_for("vibration-cc-train-01", observed_at, self.cycle),
            observed_at=observed_at,
            asset_code=self.settings.asset_code,
            signal_type=SignalType(self.settings.signal_type),
            severity=Severity.WARNING,
            value=self.settings.vibration_value,
            unit=self.settings.vibration_unit,
            evidence=_EVIDENCE,
            recommended_action=_ACTION,
            simulated=True,
        )
        self.last_observation_id = sample.observation_id
        return sample

    def snapshot(self) -> dict[str, object]:
        return {
            "running": self.running,
            "scenario": self.scenario.value,
            "cycle": self.cycle,
            "lastObservationId": self.last_observation_id,
            "simulated": True,
        }
