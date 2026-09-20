from __future__ import annotations

from datetime import UTC, datetime

import httpx
import pytest
import respx
from tests.factory import make_settings

from reliability_intelligence.clients.venueops import HttpVenueOpsClient, VenueOpsClientError
from reliability_intelligence.domain.models import ReliabilitySample, Severity, SignalType


def _sample() -> ReliabilitySample:
    return ReliabilitySample(
        observation_id="vibration-cc-train-01-20260920T183000Z-0001-abcd1234",
        observed_at=datetime(2026, 9, 20, 18, 30, tzinfo=UTC),
        asset_code="CC-TRAIN-01-WHEEL-A",
        signal_type=SignalType.VIBRATION,
        severity=Severity.WARNING,
        value=8.4,
        unit="mm/s",
        evidence="Fictional simulated vibration exceeded the demonstration threshold.",
        recommended_action="Inspect the wheel assembly.",
        simulated=True,
    )


@pytest.mark.asyncio
async def test_recommendation_retries_then_succeeds() -> None:
    settings = make_settings(venueops_max_retries=2, venueops_base_url="http://venueops.test")
    with respx.mock(assert_all_called=True) as router:
        route = router.post(
            "http://venueops.test/api/v1/integrations/reliability/recommendations"
        ).mock(
            side_effect=[
                httpx.Response(503),
                httpx.Response(201, json={"duplicate": False, "status": "PENDING_REVIEW"}),
            ]
        )
        async with httpx.AsyncClient(base_url="http://venueops.test") as http:
            client = HttpVenueOpsClient(settings, client=http)
            result = await client.submit_recommendation(_sample())
    assert result["duplicate"] is False
    assert route.call_count == 2


@pytest.mark.asyncio
async def test_client_errors_are_not_retried() -> None:
    settings = make_settings(venueops_max_retries=2, venueops_base_url="http://venueops.test")
    with respx.mock(assert_all_called=True) as router:
        router.post("http://venueops.test/api/v1/integrations/reliability/recommendations").mock(
            return_value=httpx.Response(400)
        )
        async with httpx.AsyncClient(base_url="http://venueops.test") as http:
            client = HttpVenueOpsClient(settings, client=http)
            with pytest.raises(VenueOpsClientError):
                await client.submit_recommendation(_sample())
