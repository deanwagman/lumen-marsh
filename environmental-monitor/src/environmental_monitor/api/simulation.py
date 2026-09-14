from __future__ import annotations

from typing import cast

from fastapi import APIRouter, HTTPException, Request

from environmental_monitor.api.schemas import LightningResponse, SimulationLightningRequest
from environmental_monitor.container import AppContainer
from environmental_monitor.domain.lightning import LightningScenario

router = APIRouter(prefix="/api/v1/simulation", tags=["simulation"])

_SCENARIOS = {
    "clear": LightningScenario.CLEAR,
    "storm-approaching": LightningScenario.STORM_APPROACHING,
    "hold-conditions": LightningScenario.HOLD_CONDITIONS,
    "clearance-period": LightningScenario.CLEARANCE_PERIOD,
}


def _container(request: Request) -> AppContainer:
    return cast(AppContainer, request.app.state.container)


def _require_simulation(container: AppContainer) -> None:
    if not container.settings.simulation_enabled:
        raise HTTPException(status_code=404, detail="Simulation is disabled")


@router.post("/scenarios/{scenario}")
async def apply_scenario(scenario: str, request: Request) -> dict[str, object]:
    container = _container(request)
    _require_simulation(container)
    mapped = _SCENARIOS.get(scenario)
    if mapped is None:
        raise HTTPException(status_code=404, detail="Unknown simulation scenario")
    observation = container.simulator.apply_scenario(mapped)
    await container.monitor.refresh_lightning()
    await container.monitor.evaluate_and_publish()
    payload = (
        LightningResponse.from_domain(observation).model_dump(by_alias=True)
        if observation
        else None
    )
    return {"scenario": mapped.value, "lightning": payload}


@router.post("/lightning")
async def record_lightning(body: SimulationLightningRequest, request: Request) -> dict[str, object]:
    container = _container(request)
    _require_simulation(container)
    observation = container.simulator.record_strike(
        distance_miles=body.distance_miles,
        bearing_degrees=body.bearing_degrees,
        observed_at=body.observed_at,
    )
    await container.monitor.refresh_lightning()
    await container.monitor.evaluate_and_publish()
    return LightningResponse.from_domain(observation).model_dump(by_alias=True)
