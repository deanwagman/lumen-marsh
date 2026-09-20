from __future__ import annotations

from collections.abc import AsyncIterator
from datetime import UTC, datetime

import pytest
from httpx import ASGITransport, AsyncClient

from park_flow_intelligence.app_factory import create_app
from park_flow_intelligence.clock import FrozenClock
from park_flow_intelligence.settings import Settings
from tests.factory import RecordingClient, make_settings

NOW = datetime(2026, 9, 15, 18, 30, tzinfo=UTC)


@pytest.fixture
def settings() -> Settings:
    return make_settings()


@pytest.fixture
def clock() -> FrozenClock:
    return FrozenClock(NOW)


@pytest.fixture
def client_sink() -> RecordingClient:
    return RecordingClient()


@pytest.fixture
def app(settings: Settings, clock: FrozenClock, client_sink: RecordingClient) -> object:
    return create_app(
        settings,
        clock=clock,
        venueops_client=client_sink,
        enable_loop=False,
        enable_logging=False,
    )


@pytest.fixture
async def client(app: object) -> AsyncIterator[AsyncClient]:
    async with AsyncClient(
        transport=ASGITransport(app=app),  # type: ignore[arg-type]
        base_url="http://test",
    ) as async_client:
        yield async_client
