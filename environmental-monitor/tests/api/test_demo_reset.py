from __future__ import annotations

from uuid import uuid4

import pytest
from httpx import ASGITransport, AsyncClient
from pydantic import ValidationError
from tests.conftest import NOW, make_observation, make_settings

from environmental_monitor.app_factory import create_app
from environmental_monitor.clock import FrozenClock
from environmental_monitor.domain.lightning import LightningScenario
from environmental_monitor.settings import Settings

SECRET = "test-only-demo-sandbox-secret"
PATH = "/internal/demo/reset"


def _app(settings: Settings) -> object:
    return create_app(
        settings,
        clock=FrozenClock(NOW),
        enable_polling=False,
        enable_logging=False,
    )


def test_demo_sandbox_is_disabled_by_default_and_requires_secret() -> None:
    assert make_settings().demo_sandbox_enabled is False
    with pytest.raises(ValidationError, match="DEMO_SANDBOX_SECRET"):
        make_settings(demo_sandbox_enabled=True)


@pytest.mark.asyncio
async def test_reset_is_hidden_when_disabled() -> None:
    app = _app(make_settings())
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        response = await client.post(PATH, json={"resetId": str(uuid4())})
    assert response.status_code == 404


@pytest.mark.asyncio
async def test_reset_rejects_missing_and_wrong_secret() -> None:
    app = _app(make_settings(demo_sandbox_enabled=True, demo_sandbox_secret=SECRET))
    body = {"resetId": str(uuid4())}
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        missing = await client.post(PATH, json=body)
        wrong = await client.post(PATH, json=body, headers={"X-Demo-Reset-Secret": "wrong"})
    assert missing.status_code == 401
    assert wrong.status_code == 401


@pytest.mark.asyncio
async def test_reset_clears_simulator_and_repository_state_and_replays() -> None:
    app = _app(make_settings(demo_sandbox_enabled=True, demo_sandbox_secret=SECRET))
    container = app.state.container
    container.simulator.apply_scenario(LightningScenario.HOLD_CONDITIONS)
    observation = make_observation()
    await container.monitor.observations.upsert(observation)
    container.store.snapshot.observation = observation
    reset_id = str(uuid4())

    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        completed = await client.post(
            PATH,
            json={"resetId": reset_id},
            headers={"X-Demo-Reset-Secret": SECRET},
        )
        assert container.simulator.scenario is LightningScenario.CLEAR
        container.simulator.apply_scenario(LightningScenario.HOLD_CONDITIONS)
        replay = await client.post(
            PATH,
            json={"resetId": reset_id},
            headers={"X-Demo-Reset-Secret": SECRET},
        )

    assert completed.json() == {"resetId": reset_id, "status": "COMPLETED"}
    assert await container.monitor.observations.latest() is None
    assert container.store.snapshot.observation is None
    assert replay.json() == {"resetId": reset_id, "status": "REPLAY"}
    assert container.simulator.scenario is LightningScenario.HOLD_CONDITIONS
