from __future__ import annotations

import logging
import os
import uuid
from collections.abc import AsyncIterator, Awaitable, Callable
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI, Request, Response
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.ext.asyncio import AsyncEngine

from environmental_monitor.api import health, simulation, weather
from environmental_monitor.application.polling_service import PollingService
from environmental_monitor.application.recommendation_engine import RecommendationEngine
from environmental_monitor.application.weather_monitor import LightningSimulator, WeatherMonitor
from environmental_monitor.application.weather_snapshot import WeatherSnapshotStore
from environmental_monitor.clock import Clock, SystemClock
from environmental_monitor.container import AppContainer
from environmental_monitor.domain.rules import (
    ActiveAlertRule,
    HeavyRainRule,
    HighWindRule,
    LightningClearanceRule,
    LightningHoldRule,
    WeatherRule,
)
from environmental_monitor.infrastructure.persistence.memory import (
    InMemoryAlertRepository,
    InMemoryForecastRepository,
    InMemoryObservationRepository,
    InMemoryPollAttemptRepository,
    InMemoryRecommendationRepository,
)
from environmental_monitor.infrastructure.providers.nws.client import NwsWeatherProvider
from environmental_monitor.infrastructure.venueops.recommendation_sink import (
    VenueOpsRecommendationSink,
)
from environmental_monitor.logconfig import configure_logging, request_id_var
from environmental_monitor.metrics import MetricsRegistry
from environmental_monitor.settings import Settings

logger = logging.getLogger("environmental-monitor")


def _alembic_config_path() -> str:
    return "/app/alembic.ini" if Path("/app/alembic.ini").is_file() else "alembic.ini"


def build_rules(settings: Settings) -> list[WeatherRule]:
    outdoor = (settings.mangrove_run_id, settings.cypress_coil_id)
    return [
        ActiveAlertRule(outdoor),
        HighWindRule(
            attraction_id=settings.cypress_coil_id,
            hold_mps=settings.wind_gust_hold_mps,
            clear_mps=settings.wind_gust_clear_mps,
        ),
        HeavyRainRule(
            attraction_id=settings.mangrove_run_id,
            hold_mm=settings.rainfall_hold_mm,
            clear_mm=settings.rainfall_clear_mm,
        ),
        LightningHoldRule(
            attraction_ids=outdoor,
            hold_radius_miles=settings.lightning_hold_radius_miles,
            clearance_minutes=settings.lightning_clearance_minutes,
        ),
        LightningClearanceRule(
            attraction_ids=outdoor,
            hold_radius_miles=settings.lightning_hold_radius_miles,
            clearance_minutes=settings.lightning_clearance_minutes,
        ),
    ]


def create_app(
    settings: Settings | None = None,
    *,
    clock: Clock | None = None,
    weather_provider: object | None = None,
    recommendation_sink: object | None = None,
    enable_polling: bool = True,
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

    store = WeatherSnapshotStore(resolved_settings)
    metrics = MetricsRegistry()
    simulator = LightningSimulator(resolved_settings, resolved_clock)
    engine = RecommendationEngine(build_rules(resolved_settings))

    observations = InMemoryObservationRepository()
    alerts = InMemoryAlertRepository()
    forecasts = InMemoryForecastRepository()
    recommendations = InMemoryRecommendationRepository()
    polls = InMemoryPollAttemptRepository()
    postgres_engine: AsyncEngine | None = None

    if resolved_settings.persistence_enabled:
        from environmental_monitor.infrastructure.persistence.postgres.repositories import (
            PostgresAlertRepository,
            PostgresForecastRepository,
            PostgresObservationRepository,
            PostgresPollAttemptRepository,
            PostgresRecommendationRepository,
            create_session_factory,
        )

        db_engine, sessions = create_session_factory(resolved_settings.database_url)
        observations = PostgresObservationRepository(sessions)  # type: ignore[assignment]
        alerts = PostgresAlertRepository(sessions)  # type: ignore[assignment]
        forecasts = PostgresForecastRepository(sessions)  # type: ignore[assignment]
        recommendations = PostgresRecommendationRepository(sessions)  # type: ignore[assignment]
        polls = PostgresPollAttemptRepository(sessions)  # type: ignore[assignment]
        postgres_engine = db_engine

    provider = weather_provider or NwsWeatherProvider(resolved_settings, resolved_clock)
    sink = recommendation_sink or VenueOpsRecommendationSink(resolved_settings)

    monitor = WeatherMonitor(
        settings=resolved_settings,
        clock=resolved_clock,
        provider=provider,  # type: ignore[arg-type]
        engine=engine,
        store=store,
        observations=observations,
        alerts=alerts,
        forecasts=forecasts,
        recommendations=recommendations,
        polls=polls,
        sink=sink,  # type: ignore[arg-type]
        simulator=simulator,
        metrics=metrics,
    )
    polling = PollingService(monitor, resolved_settings) if enable_polling else None
    container = AppContainer(
        settings=resolved_settings,
        clock=resolved_clock,
        monitor=monitor,
        store=store,
        simulator=simulator,
        metrics=metrics,
        polling=polling,
        initialized=True,
    )

    @asynccontextmanager
    async def lifespan(_app: FastAPI) -> AsyncIterator[None]:
        run_startup_migrations = resolved_settings.persistence_enabled and os.getenv(
            "RUN_MIGRATIONS_ON_STARTUP", "true"
        ).strip().lower() in {"1", "true", "yes"}
        if run_startup_migrations:
            from alembic import command
            from alembic.config import Config

            alembic_cfg = Config(_alembic_config_path())
            command.upgrade(alembic_cfg, "head")
        container.initialized = True
        if polling is not None:
            await polling.start()
        try:
            yield
        finally:
            if polling is not None:
                await polling.stop()
            close = getattr(provider, "aclose", None)
            if close is not None:
                await close()
            sink_close = getattr(sink, "aclose", None)
            if sink_close is not None:
                await sink_close()
            if postgres_engine is not None:
                await postgres_engine.dispose()

    app = FastAPI(
        title="Lumen Marsh Environmental Monitor",
        description=(
            "Retrieves public weather data, evaluates fictional operational thresholds, "
            "and sends recommendations to VenueOps. It never changes attraction state."
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
    app.include_router(weather.router)
    app.include_router(simulation.router)

    @app.middleware("http")
    async def request_id_middleware(
        request: Request,
        call_next: Callable[[Request], Awaitable[Response]],
    ) -> Response:
        request_id = request.headers.get("X-Request-ID") or str(uuid.uuid4())
        token = request_id_var.set(request_id)
        try:
            response = await call_next(request)
        finally:
            request_id_var.reset(token)
        response.headers["X-Request-ID"] = request_id
        return response

    return app
