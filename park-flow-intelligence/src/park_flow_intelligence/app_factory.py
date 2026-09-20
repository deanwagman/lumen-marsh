from __future__ import annotations

import uuid
from collections.abc import AsyncIterator, Awaitable, Callable
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request, Response
from fastapi.middleware.cors import CORSMiddleware

from park_flow_intelligence.api import health, simulation
from park_flow_intelligence.api import metrics as metrics_api
from park_flow_intelligence.clients.venueops import HttpVenueOpsClient, VenueOpsClient
from park_flow_intelligence.clock import Clock, SystemClock
from park_flow_intelligence.container import AppContainer
from park_flow_intelligence.logconfig import configure_logging, request_id_var
from park_flow_intelligence.metrics import MetricsRegistry
from park_flow_intelligence.settings import Settings
from park_flow_intelligence.simulation.engine import FlowSimulator
from park_flow_intelligence.simulation.loop import SimulationLoop


def create_app(
    settings: Settings | None = None,
    *,
    clock: Clock | None = None,
    venueops_client: VenueOpsClient | None = None,
    enable_loop: bool = True,
    enable_logging: bool = True,
) -> FastAPI:
    resolved_settings = settings or Settings()
    resolved_clock = clock or SystemClock()
    if enable_logging:
        configure_logging(
            level=resolved_settings.log_level,
            structured=resolved_settings.structured_logging,
            environment=resolved_settings.app_env,
        )

    metrics = MetricsRegistry()
    simulator = FlowSimulator(resolved_settings, resolved_clock)
    client = venueops_client or HttpVenueOpsClient(resolved_settings)
    loop = SimulationLoop(
        simulator,
        client,
        metrics,
        resolved_settings.simulation_cycle_seconds,
    )
    container = AppContainer(
        settings=resolved_settings,
        clock=resolved_clock,
        simulator=simulator,
        loop=loop,
        metrics=metrics,
        initialized=True,
    )

    @asynccontextmanager
    async def lifespan(_app: FastAPI) -> AsyncIterator[None]:
        container.initialized = True
        if enable_loop and resolved_settings.simulation_enabled:
            await loop.start()
        try:
            yield
        finally:
            await loop.stop()
            close = getattr(client, "aclose", None)
            if close is not None:
                await close()

    app = FastAPI(
        title="Lumen Marsh Park Flow Intelligence",
        description=(
            "Generates simulated queue telemetry and deterministic wait forecasts. "
            "It never changes attraction state."
        ),
        version="0.1.0",
        lifespan=lifespan,
    )
    app.state.container = container
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_methods=["*"],
        allow_headers=["*"],
    )
    app.include_router(health.router)
    app.include_router(metrics_api.router)
    app.include_router(simulation.router)

    @app.middleware("http")
    async def request_id_middleware(
        request: Request,
        call_next: Callable[[Request], Awaitable[Response]],
    ) -> Response:
        request_id = (
            request.headers.get("X-Correlation-Id")
            or request.headers.get("X-Request-ID")
            or str(uuid.uuid4())
        )
        token = request_id_var.set(request_id)
        try:
            response = await call_next(request)
        finally:
            request_id_var.reset(token)
        response.headers["X-Correlation-Id"] = request_id
        return response

    return app
