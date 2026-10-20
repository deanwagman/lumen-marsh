from __future__ import annotations

from dataclasses import dataclass
from typing import TYPE_CHECKING

from reliability_intelligence.clock import Clock
from reliability_intelligence.metrics import MetricsRegistry
from reliability_intelligence.settings import Settings
from reliability_intelligence.simulation.engine import ReliabilitySimulator
from reliability_intelligence.simulation.loop import SimulationLoop

if TYPE_CHECKING:
    from reliability_intelligence.api.demo_reset import DemoResetRegistry


@dataclass
class AppContainer:
    settings: Settings
    clock: Clock
    simulator: ReliabilitySimulator
    loop: SimulationLoop
    metrics: MetricsRegistry
    demo_resets: DemoResetRegistry
    initialized: bool = False

    async def reset_demo_state(self) -> None:
        await self.loop.stop()
        self.simulator.reset()
