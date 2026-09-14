from __future__ import annotations

import asyncio
import logging
from collections.abc import Awaitable, Callable

from environmental_monitor.application.weather_monitor import WeatherMonitor
from environmental_monitor.settings import Settings

logger = logging.getLogger("environmental-monitor")


class PollingService:
    def __init__(self, monitor: WeatherMonitor, settings: Settings) -> None:
        self._monitor = monitor
        self._settings = settings
        self._stop = asyncio.Event()
        self._task: asyncio.Task[None] | None = None

    async def start(self) -> None:
        self._stop.clear()
        self._task = asyncio.create_task(self._run(), name="weather-polling")

    async def stop(self) -> None:
        self._stop.set()
        if self._task is not None:
            self._task.cancel()
            try:
                await self._task
            except asyncio.CancelledError:
                pass
            self._task = None

    async def _run(self) -> None:
        await asyncio.gather(
            self._loop(
                "location",
                self._settings.location_metadata_refresh_seconds,
                self._monitor.poll_location,
            ),
            self._loop(
                "observation",
                self._settings.observation_poll_seconds,
                self._monitor.poll_observation,
            ),
            self._loop(
                "forecast", self._settings.forecast_poll_seconds, self._monitor.poll_forecast
            ),
            self._loop("alerts", self._settings.alert_poll_seconds, self._monitor.poll_alerts),
            self._loop(
                "lightning", self._settings.alert_poll_seconds, self._monitor.refresh_lightning
            ),
            return_exceptions=True,
        )

    async def _loop(
        self,
        name: str,
        interval_seconds: int,
        operation: Callable[[], Awaitable[None]],
    ) -> None:
        while not self._stop.is_set():
            try:
                await operation()
            except Exception:
                logger.exception("independent poll loop crashed", extra={"operation": name})
            try:
                await asyncio.wait_for(self._stop.wait(), timeout=interval_seconds)
            except TimeoutError:
                continue
