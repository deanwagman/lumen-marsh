from __future__ import annotations

from dataclasses import dataclass

from reliability_intelligence.clock import Clock
from reliability_intelligence.metrics import MetricsRegistry
from reliability_intelligence.settings import Settings
from reliability_intelligence.simulation.engine import ReliabilitySimulator
from reliability_intelligence.simulation.loop import SimulationLoop


@dataclass
class AppContainer:
    settings: Settings
    clock: Clock
    simulator: ReliabilitySimulator
    loop: SimulationLoop
    metrics: MetricsRegistry
    initialized: bool = False
