from __future__ import annotations

from collections.abc import Awaitable, Callable
from dataclasses import dataclass
from typing import TYPE_CHECKING

from environmental_monitor.application.polling_service import PollingService
from environmental_monitor.application.weather_monitor import LightningSimulator, WeatherMonitor
from environmental_monitor.application.weather_snapshot import WeatherSnapshotStore
from environmental_monitor.clock import Clock
from environmental_monitor.metrics import MetricsRegistry
from environmental_monitor.settings import Settings

if TYPE_CHECKING:
    from environmental_monitor.api.demo_reset import DemoResetRegistry


@dataclass
class AppContainer:
    settings: Settings
    clock: Clock
    monitor: WeatherMonitor
    store: WeatherSnapshotStore
    simulator: LightningSimulator
    metrics: MetricsRegistry
    polling: PollingService | None
    demo_resets: DemoResetRegistry
    persistence_reset: Callable[[], Awaitable[None]] | None = None
    initialized: bool = False

    async def reset_demo_state(self) -> None:
        polling = self.polling
        restart_polling = False
        if polling is not None and polling.running:
            restart_polling = True
            await polling.stop()
        try:
            await self.monitor.reset_demo_state(self.persistence_reset)
        finally:
            if restart_polling and polling is not None:
                await polling.start()
