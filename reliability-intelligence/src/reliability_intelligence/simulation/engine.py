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
        self._episode_observation_id: str | None = None
        self._episode_emitted = False

    def start(self) -> None:
        self.running = True

    def stop(self) -> None:
        self.running = False

    def apply_scenario(self, scenario: Scenario) -> None:
        if scenario is not self.scenario:
            if scenario is Scenario.CYPRESS_COIL_VIBRATION:
                self._begin_episode()
            else:
                self._end_episode()
        self.scenario = scenario

    def next_cycle(self) -> ReliabilitySample | None:
        self.cycle += 1
        if self.scenario != Scenario.CYPRESS_COIL_VIBRATION:
            self.last_observation_id = None
            return None
        if self._episode_emitted:
            return None
        if self._episode_observation_id is None:
            self._begin_episode()
        observation_id = self._episode_observation_id
        if observation_id is None:
            return None
        observed_at = self.clock.now()
        sample = ReliabilitySample(
            observation_id=observation_id,
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
        self._episode_emitted = True
        return sample

    def _begin_episode(self) -> None:
        observed_at = self.clock.now()
        self._episode_observation_id = observation_id_for("vibration-cc-train-01", observed_at, 0)
        self._episode_emitted = False
        self.last_observation_id = None

    def _end_episode(self) -> None:
        self._episode_observation_id = None
        self._episode_emitted = False
        self.last_observation_id = None

    def snapshot(self) -> dict[str, object]:
        return {
            "running": self.running,
            "scenario": self.scenario.value,
            "cycle": self.cycle,
            "lastObservationId": self.last_observation_id,
            "simulated": True,
        }
