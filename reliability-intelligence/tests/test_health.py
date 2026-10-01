from __future__ import annotations

from httpx import ASGITransport, AsyncClient
from tests.factory import RecordingClient, make_settings

from reliability_intelligence.app_factory import create_app


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
    disrupted = await client.post("/simulation/scenarios/cypress-coil-vibration")
    assert disrupted.status_code == 200
    body = disrupted.json()
    assert body["scenario"] == "cypress-coil-vibration"
    assert body["simulated"] is True
    assert body["submitted"] is True
    assert len(client_sink.recommendations) == 1
    sample = client_sink.recommendations[0]
    assert sample.asset_code == "CC-TRAIN-01-WHEEL-A"
    assert sample.signal_type.value == "VIBRATION"
    stopped = await client.post("/simulation/stop")
    assert stopped.status_code == 200


async def test_normal_scenario_does_not_ingest(
    client: AsyncClient, client_sink: RecordingClient
) -> None:
    started = await client.post("/simulation/start")
    assert started.status_code == 200
    assert client_sink.recommendations == []


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
