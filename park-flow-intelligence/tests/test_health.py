from __future__ import annotations

from httpx import ASGITransport, AsyncClient

from park_flow_intelligence.app_factory import create_app
from tests.factory import RecordingClient, make_settings


async def test_live_and_ready(client: AsyncClient) -> None:
    live = await client.get("/health/live")
    ready = await client.get("/health/ready")
    assert live.status_code == 200
    assert ready.status_code == 200
    assert live.json() == {"status": "ok"}


async def test_simulation_controls_are_local_only(
    client: AsyncClient,
    client_sink: RecordingClient,
) -> None:
    disrupted = await client.post("/simulation/scenarios/mangrove-disruption")
    assert disrupted.status_code == 200
    body = disrupted.json()
    assert body["scenario"] == "mangrove-disruption"
    assert body["simulated"] is True
    assert len(client_sink.observations) == 3
    assert any(forecast[1] is not None for forecast in client_sink.forecasts)
    stopped = await client.post("/simulation/stop")
    assert stopped.status_code == 200


async def test_simulation_disabled_returns_404() -> None:
    app = create_app(
        make_settings(simulation_enabled=False),
        venueops_client=RecordingClient(),
        enable_loop=False,
        enable_logging=False,
    )
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as http:
        response = await http.post("/simulation/start")
        assert response.status_code == 404
