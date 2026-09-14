from __future__ import annotations

from httpx import ASGITransport, AsyncClient
from tests.conftest import (
    make_settings,
)

from environmental_monitor.app_factory import create_app


async def test_weather_endpoints(client, app) -> None:
    monitor = app.state.container.monitor
    await monitor.poll_location()
    await monitor.poll_observation()
    await monitor.poll_forecast()
    await monitor.poll_alerts()

    current = await client.get("/api/v1/weather/current")
    assert current.status_code == 200
    body = current.json()
    assert body["observation"]["stationId"] == "KMCO"
    assert body["freshness"]["state"] in {"CURRENT", "STALE"}

    forecast = await client.get("/api/v1/weather/forecast")
    assert forecast.status_code == 200
    assert forecast.json()["periods"]

    alerts = await client.get("/api/v1/weather/alerts")
    assert alerts.status_code == 200

    recs = await client.get("/api/v1/weather/recommendations")
    assert recs.status_code == 200

    status = await client.get("/api/v1/weather/status")
    assert status.status_code == 200
    assert status.json()["providerState"] in {"healthy", "degraded", "not_yet_loaded"}
    assert "currentStation" in status.json()


async def test_simulation_hold_and_clear(client) -> None:
    hold = await client.post("/api/v1/simulation/scenarios/hold-conditions")
    assert hold.status_code == 200
    assert hold.json()["lightning"]["simulated"] is True
    recs = (await client.get("/api/v1/weather/recommendations")).json()["recommendations"]
    assert any(item["status"] == "ACTIVE" for item in recs)

    strike = await client.post(
        "/api/v1/simulation/lightning",
        json={"distanceMiles": 8.2, "bearingDegrees": 245, "observedAt": "2026-09-01T18:30:00Z"},
    )
    assert strike.status_code == 200
    assert strike.json()["provider"] == "simulated"

    cleared = await client.post("/api/v1/simulation/scenarios/clear")
    assert cleared.status_code == 200
    assert cleared.json()["lightning"] is None


async def test_simulation_disabled_returns_404(settings, clock, provider, sink) -> None:
    app = create_app(
        make_settings(simulation_enabled=False),
        clock=clock,
        weather_provider=provider,
        recommendation_sink=sink,
        enable_polling=False,
        enable_logging=False,
    )
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        response = await client.post("/api/v1/simulation/scenarios/clear")
        assert response.status_code == 404


async def test_invalid_simulation_input(client) -> None:
    response = await client.post(
        "/api/v1/simulation/lightning",
        json={"distanceMiles": -1, "bearingDegrees": 400},
    )
    assert response.status_code == 422
