from __future__ import annotations

from typing import cast

from fastapi import APIRouter, Request

from environmental_monitor.api.schemas import (
    AlertResponse,
    DatasetHealthResponse,
    ForecastPeriodResponse,
    LightningResponse,
    ObservationResponse,
    RecommendationResponse,
    StationResponse,
    StatusResponse,
)
from environmental_monitor.container import AppContainer

router = APIRouter(prefix="/api/v1/weather", tags=["weather"])


def _container(request: Request) -> AppContainer:
    return cast(AppContainer, request.app.state.container)


@router.get("/current")
async def current_weather(request: Request) -> dict[str, object]:
    container = _container(request)
    snapshot = container.monitor.current_snapshot()
    observation = None
    if snapshot.observation is not None:
        observation = ObservationResponse.from_domain(
            snapshot.observation,
            snapshot.observation_health,
            container.settings.park_timezone,
        ).model_dump(by_alias=True)
    lightning = None
    if snapshot.lightning is not None:
        lightning = LightningResponse.from_domain(snapshot.lightning).model_dump(by_alias=True)
    return {
        "freshness": DatasetHealthResponse.from_health(snapshot.observation_health).model_dump(
            by_alias=True
        ),
        "observation": observation,
        "lightning": lightning,
        "station": (
            StationResponse.from_domain(snapshot.station).model_dump(by_alias=True)
            if snapshot.station
            else None
        ),
    }


@router.get("/forecast")
async def forecast(request: Request) -> dict[str, object]:
    snapshot = _container(request).monitor.current_snapshot()
    return {
        "freshness": DatasetHealthResponse.from_health(snapshot.forecast_health).model_dump(
            by_alias=True
        ),
        "periods": [
            ForecastPeriodResponse.from_domain(period).model_dump(by_alias=True)
            for period in snapshot.forecast
        ],
    }


@router.get("/alerts")
async def alerts(request: Request) -> dict[str, object]:
    snapshot = _container(request).monitor.current_snapshot()
    return {
        "freshness": DatasetHealthResponse.from_health(snapshot.alert_health).model_dump(
            by_alias=True
        ),
        "alerts": [
            AlertResponse.from_domain(alert).model_dump(by_alias=True) for alert in snapshot.alerts
        ],
    }


@router.get("/recommendations")
async def recommendations(request: Request) -> dict[str, object]:
    items = await _container(request).monitor.recommendations.list_all()
    return {
        "recommendations": [
            RecommendationResponse.from_domain(item).model_dump(by_alias=True) for item in items
        ]
    }


@router.get("/status")
async def status(request: Request) -> StatusResponse:
    container = _container(request)
    snapshot = container.monitor.current_snapshot()
    return StatusResponse(
        provider_state=snapshot.provider_state,
        current_station=(
            StationResponse.from_domain(snapshot.station) if snapshot.station else None
        ),
        observation=DatasetHealthResponse.from_health(snapshot.observation_health),
        forecast=DatasetHealthResponse.from_health(snapshot.forecast_health),
        alerts=DatasetHealthResponse.from_health(snapshot.alert_health),
        lightning=DatasetHealthResponse.from_health(snapshot.lightning_health),
        venueops_delivery=DatasetHealthResponse.from_health(
            container.store.venueops.health(
                snapshot.evaluated_at, container.settings.alert_stale_after_seconds
            )
        ),
        metrics=container.metrics.snapshot(),
    )
