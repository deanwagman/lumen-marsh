from __future__ import annotations

from dataclasses import dataclass
from typing import TYPE_CHECKING

from park_flow_intelligence.clock import Clock
from park_flow_intelligence.metrics import MetricsRegistry
from park_flow_intelligence.settings import Settings
from park_flow_intelligence.simulation.engine import FlowSimulator
from park_flow_intelligence.simulation.loop import SimulationLoop

if TYPE_CHECKING:
    from park_flow_intelligence.api.demo_reset import DemoResetRegistry


@dataclass
class AppContainer:
    settings: Settings
    clock: Clock
    simulator: FlowSimulator
    loop: SimulationLoop
    metrics: MetricsRegistry
    demo_resets: DemoResetRegistry
    initialized: bool = False

    async def reset_demo_state(self) -> None:
        await self.loop.stop()
        self.simulator.reset()
