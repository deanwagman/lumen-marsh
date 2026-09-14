from __future__ import annotations

from dataclasses import dataclass

from environmental_monitor.application.polling_service import PollingService
from environmental_monitor.application.weather_monitor import LightningSimulator, WeatherMonitor
from environmental_monitor.application.weather_snapshot import WeatherSnapshotStore
from environmental_monitor.clock import Clock
from environmental_monitor.metrics import MetricsRegistry
from environmental_monitor.settings import Settings


@dataclass
class AppContainer:
    settings: Settings
    clock: Clock
    monitor: WeatherMonitor
    store: WeatherSnapshotStore
    simulator: LightningSimulator
    metrics: MetricsRegistry
    polling: PollingService | None
    initialized: bool = False
