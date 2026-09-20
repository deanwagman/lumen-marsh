from __future__ import annotations

import asyncio
import logging
import time
from typing import Any

from park_flow_intelligence.clients.venueops import VenueOpsClient, VenueOpsClientError
from park_flow_intelligence.metrics import MetricsRegistry
from park_flow_intelligence.simulation.engine import FlowSimulator

logger = logging.getLogger("park-flow-intelligence")


class SimulationLoop:
    def __init__(
        self,
        simulator: FlowSimulator,
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
        samples, bundles, recommendation = self.simulator.next_cycle()
        self.metrics.simulator_cycles.inc()
        submitted = 0
        for sample in samples:
            started = time.perf_counter()
            await self.client.submit_observation(sample)
            self.metrics.observation_latency.observe(time.perf_counter() - started)
            age = max(0.0, (self.simulator.clock.now() - sample.observed_at).total_seconds())
            self.metrics.source_data_age.labels(attraction_id=sample.attraction_id).set(age)
            submitted += 1
        for attraction_id, bundle in bundles.items():
            self.metrics.forecasts_generated.inc()
            proposal = (
                recommendation if attraction_id == self.simulator.settings.mangrove_run_id else None
            )
            try:
                await self.client.submit_forecast(bundle, proposal)
            except VenueOpsClientError:
                self.metrics.forecast_submission_failures.inc()
                logger.warning(
                    "forecast submission failed",
                    extra={
                        "attraction_id": attraction_id,
                        "observation_id": str(bundle.based_on_observation_id),
                        "simulated": True,
                        "success": False,
                    },
                )
                raise
        return {
            "cycle": self.simulator.cycle,
            "scenario": self.simulator.scenario.value,
            "observations": submitted,
            "recommendationId": None
            if recommendation is None
            else str(recommendation.recommendation_id),
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
