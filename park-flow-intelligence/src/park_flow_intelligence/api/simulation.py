from __future__ import annotations

from typing import cast

from fastapi import APIRouter, HTTPException, Request

from park_flow_intelligence.container import AppContainer
from park_flow_intelligence.domain.models import Scenario

router = APIRouter(prefix="/simulation", tags=["simulation"])

_SCENARIOS = {
    "normal": Scenario.NORMAL,
    "mangrove-disruption": Scenario.MANGROVE_DISRUPTION,
    "clear": Scenario.CLEAR,
}


def _container(request: Request) -> AppContainer:
    return cast(AppContainer, request.app.state.container)


def _require_simulation(container: AppContainer) -> None:
    if not container.settings.simulation_enabled:
        raise HTTPException(status_code=404, detail="Simulation is disabled")


@router.post("/start")
async def start(request: Request) -> dict[str, object]:
    container = _container(request)
    _require_simulation(container)
    await container.loop.start()
    return {"running": True, "scenario": container.simulator.scenario.value, "simulated": True}


@router.post("/stop")
async def stop(request: Request) -> dict[str, object]:
    container = _container(request)
    _require_simulation(container)
    await container.loop.stop()
    return {"running": False, "simulated": True}


@router.post("/scenarios/{scenario}")
async def apply_scenario(scenario: str, request: Request) -> dict[str, object]:
    container = _container(request)
    _require_simulation(container)
    mapped = _SCENARIOS.get(scenario)
    if mapped is None:
        raise HTTPException(status_code=404, detail="Unknown simulation scenario")
    container.simulator.apply_scenario(mapped)
    if container.simulator.running:
        result = await container.loop.tick()
    else:
        result = await container.loop.start()
    return {"scenario": container.simulator.scenario.value, **result}


@router.get("/status")
async def status(request: Request) -> dict[str, object]:
    container = _container(request)
    _require_simulation(container)
    return container.simulator.snapshot()
