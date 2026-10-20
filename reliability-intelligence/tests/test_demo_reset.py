from __future__ import annotations

from uuid import uuid4

import pytest
from httpx import ASGITransport, AsyncClient
from pydantic import ValidationError
from tests.conftest import NOW
from tests.factory import RecordingClient, make_settings

from reliability_intelligence.app_factory import create_app
from reliability_intelligence.clock import FrozenClock
from reliability_intelligence.domain.models import Scenario

SECRET = "test-only-demo-sandbox-secret"
PATH = "/internal/demo/reset"


def _app() -> object:
    return create_app(
        make_settings(demo_sandbox_enabled=True, demo_sandbox_secret=SECRET),
        clock=FrozenClock(NOW),
        venueops_client=RecordingClient(),
        enable_loop=False,
        enable_logging=False,
    )


def test_demo_sandbox_is_disabled_by_default_and_requires_secret() -> None:
    assert make_settings().demo_sandbox_enabled is False
    with pytest.raises(ValidationError, match="DEMO_SANDBOX_SECRET"):
        make_settings(demo_sandbox_enabled=True)


@pytest.mark.asyncio
async def test_reset_is_hidden_when_disabled() -> None:
    app = create_app(
        make_settings(),
        clock=FrozenClock(NOW),
        venueops_client=RecordingClient(),
        enable_loop=False,
        enable_logging=False,
    )
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        response = await client.post(PATH, json={"resetId": str(uuid4())})
    assert response.status_code == 404


@pytest.mark.asyncio
async def test_reset_rejects_missing_and_wrong_secret() -> None:
    app = _app()
    body = {"resetId": str(uuid4())}
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        missing = await client.post(PATH, json=body)
        wrong = await client.post(
            PATH, json=body, headers={"X-Demo-Reset-Secret": "wrong"}
        )
    assert missing.status_code == 401
    assert wrong.status_code == 401


@pytest.mark.asyncio
async def test_reset_clears_loop_and_episode_and_replays_without_resetting_again() -> None:
    app = _app()
    container = app.state.container
    container.simulator.start()
    container.simulator.apply_scenario(Scenario.CYPRESS_COIL_VIBRATION)
    assert container.simulator.next_cycle() is not None
    reset_id = str(uuid4())

    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        completed = await client.post(
            PATH,
            json={"resetId": reset_id},
            headers={"X-Demo-Reset-Secret": SECRET},
        )
        assert container.simulator.snapshot() == {
            "running": False,
            "scenario": "normal",
            "cycle": 0,
            "lastObservationId": None,
            "simulated": True,
        }

        container.simulator.apply_scenario(Scenario.CYPRESS_COIL_VIBRATION)
        assert container.simulator.next_cycle() is not None
        replay = await client.post(
            PATH,
            json={"resetId": reset_id},
            headers={"X-Demo-Reset-Secret": SECRET},
        )

    assert completed.json() == {"resetId": reset_id, "status": "COMPLETED"}
    assert replay.json() == {"resetId": reset_id, "status": "REPLAY"}
    assert container.simulator.scenario is Scenario.CYPRESS_COIL_VIBRATION
    assert container.simulator.cycle == 1
