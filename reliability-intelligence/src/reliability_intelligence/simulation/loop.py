from __future__ import annotations

import asyncio
import logging
import time
from typing import Any

from reliability_intelligence.clients.venueops import VenueOpsClient, VenueOpsClientError
from reliability_intelligence.metrics import MetricsRegistry
from reliability_intelligence.simulation.engine import ReliabilitySimulator

logger = logging.getLogger("reliability-intelligence")


class SimulationLoop:
    def __init__(
        self,
        simulator: ReliabilitySimulator,
        client: VenueOpsClient,
        metrics: MetricsRegistry,
        cycle_seconds: float,
    ) -> None:
        self.simulator = simulator
        self.client = client
        self.metrics = metrics
        self.cycle_seconds = cycle_seconds
        self._task: asyncio.Task[None] | None = None

    async def start(self) -> dict[str, Any]:
        already_running = self.simulator.running
        self.simulator.start()
        result: dict[str, Any] = {
            "cycle": self.simulator.cycle,
            "scenario": self.simulator.scenario.value,
            "simulated": True,
        }
        if not already_running:
            result = await self.tick()
        if self._task is None or self._task.done():
            self._task = asyncio.create_task(self._run())
        return result

    async def stop(self) -> None:
        self.simulator.stop()
        if self._task is not None:
            self._task.cancel()
            try:
                await self._task
            except asyncio.CancelledError:
                pass
            self._task = None

    async def tick(self) -> dict[str, Any]:
        sample = self.simulator.next_cycle()
        self.metrics.simulator_cycles.inc()
        submitted = False
        if sample is not None:
            started = time.perf_counter()
            try:
                await self.client.submit_recommendation(sample)
            except VenueOpsClientError:
                self.metrics.submission_failures.inc()
                logger.warning(
                    "reliability submission failed",
                    extra={
                        "observation_id": sample.observation_id,
                        "asset_code": sample.asset_code,
                        "simulated": True,
                        "success": False,
                    },
                )
                raise
            self.metrics.submission_latency.observe(time.perf_counter() - started)
            self.metrics.recommendations_submitted.inc()
            submitted = True
        return {
            "cycle": self.simulator.cycle,
            "scenario": self.simulator.scenario.value,
            "submitted": submitted,
            "observationId": None if sample is None else sample.observation_id,
            "simulated": True,
        }

    async def _run(self) -> None:
        while self.simulator.running:
            await asyncio.sleep(self.cycle_seconds)
            if not self.simulator.running:
                break
            try:
                await self.tick()
            except asyncio.CancelledError:
                raise
            except Exception:
                logger.exception(
                    "simulator cycle failed", extra={"simulated": True, "success": False}
                )
