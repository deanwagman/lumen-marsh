from __future__ import annotations

from park_flow_intelligence.settings import Settings


def make_settings(**overrides: object) -> Settings:
    values: dict[str, object] = {
        "app_env": "test",
        "log_level": "INFO",
        "simulation_enabled": True,
        "simulation_cycle_seconds": 15,
        "simulation_seed": 20260915,
        "oidc_disabled": True,
        "venueops_bearer_token": "local-flow-token",
        "venueops_base_url": "http://venueops.test",
    }
    values.update(overrides)
    return Settings(_env_file=None, **values)  # type: ignore[arg-type]


class RecordingClient:
    def __init__(self) -> None:
        self.observations: list[object] = []
        self.forecasts: list[object] = []

    async def submit_observation(self, sample: object) -> dict[str, object]:
        self.observations.append(sample)
        return {"accepted": True, "replay": False}

    async def submit_forecast(self, bundle: object, recommendation: object) -> dict[str, object]:
        self.forecasts.append((bundle, recommendation))
        return {"accepted": True}

    async def aclose(self) -> None:
        return None
