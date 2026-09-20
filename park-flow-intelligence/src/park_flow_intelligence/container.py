from __future__ import annotations

from dataclasses import dataclass

from park_flow_intelligence.clock import Clock
from park_flow_intelligence.metrics import MetricsRegistry
from park_flow_intelligence.settings import Settings
from park_flow_intelligence.simulation.engine import FlowSimulator
from park_flow_intelligence.simulation.loop import SimulationLoop


@dataclass
class AppContainer:
    settings: Settings
    clock: Clock
    simulator: FlowSimulator
    loop: SimulationLoop
    metrics: MetricsRegistry
    initialized: bool = False
