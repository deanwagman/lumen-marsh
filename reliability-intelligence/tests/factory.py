from __future__ import annotations

from reliability_intelligence.domain.models import ReliabilitySample
from reliability_intelligence.settings import Settings


def make_settings(**overrides: object) -> Settings:
    values: dict[str, object] = {
        "app_env": "test",
        "log_level": "INFO",
        "simulation_enabled": True,
        "simulation_cycle_seconds": 20,
        "simulation_seed": 20260920,
        "oidc_disabled": True,
        "venueops_bearer_token": "local-reliability-token",
        "venueops_base_url": "http://venueops.test",
    }
    values.update(overrides)
    return Settings(_env_file=None, **values)  # type: ignore[arg-type]


class RecordingClient:
    def __init__(self) -> None:
        self.recommendations: list[ReliabilitySample] = []

    async def submit_recommendation(self, sample: ReliabilitySample) -> dict[str, object]:
        self.recommendations.append(sample)
        return {"duplicate": False, "status": "PENDING_REVIEW"}

    async def aclose(self) -> None:
        return None
