from __future__ import annotations

from datetime import UTC, datetime
from uuid import uuid4

import httpx
import pytest
import respx

from park_flow_intelligence.clients.venueops import HttpVenueOpsClient, VenueOpsClientError
from park_flow_intelligence.domain.models import QueueSample
from tests.factory import make_settings


def _sample() -> QueueSample:
    return QueueSample(
        attraction_id="mangrove-run",
        observation_id=uuid4(),
        observed_at=datetime(2026, 9, 15, 18, 30, tzinfo=UTC),
        window_seconds=60,
        queue_length=180,
        arrivals=16,
        boarded=16,
        operating_units=8,
        configured_units=8,
        simulated=True,
    )


@pytest.mark.asyncio
async def test_observation_retries_then_succeeds() -> None:
    settings = make_settings(venueops_max_retries=2, venueops_base_url="http://venueops.test")
    with respx.mock(assert_all_called=True) as router:
        route = router.post("http://venueops.test/api/v1/integrations/flow/observations").mock(
            side_effect=[
                httpx.Response(503),
                httpx.Response(
                    201, json={"accepted": True, "replay": False, "projectionVersion": 1}
                ),
            ]
        )
        async with httpx.AsyncClient(base_url="http://venueops.test") as http:
            client = HttpVenueOpsClient(settings, client=http)
            result = await client.submit_observation(_sample())
    assert result["accepted"] is True
    assert route.call_count == 2


@pytest.mark.asyncio
async def test_client_errors_are_not_retried() -> None:
    settings = make_settings(venueops_max_retries=2, venueops_base_url="http://venueops.test")
    with respx.mock(assert_all_called=True) as router:
        router.post("http://venueops.test/api/v1/integrations/flow/observations").mock(
            return_value=httpx.Response(400)
        )
        async with httpx.AsyncClient(base_url="http://venueops.test") as http:
            client = HttpVenueOpsClient(settings, client=http)
            with pytest.raises(VenueOpsClientError):
                await client.submit_observation(_sample())
