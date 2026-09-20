from __future__ import annotations

from park_flow_intelligence.domain.models import HORIZONS
from park_flow_intelligence.settings import Settings


def displaced_arrivals_per_minute(
    *, expected_arrivals_per_minute: float, settings: Settings
) -> float:
    return expected_arrivals_per_minute * settings.displaced_demand_fraction


def transfer_by_horizon(
    *, destination_id: str, displaced_per_minute: float, settings: Settings
) -> dict[int, int]:
    weight = 0.0
    if destination_id == settings.cypress_coil_id:
        weight = settings.cypress_coil_weight
    elif destination_id == settings.stormglass_station_id:
        weight = settings.stormglass_station_weight
    return {horizon: round(displaced_per_minute * weight * horizon) for horizon in HORIZONS}


def redistribution_assumptions(
    *, source_id: str, displaced_per_minute: float, settings: Settings
) -> list[str]:
    other_percent = round(settings.other_experience_weight * 100)
    cypress_percent = round(settings.cypress_coil_weight * 100)
    stormglass_percent = round(settings.stormglass_station_weight * 100)
    return [
        f"{source_id} unavailable.",
        f"Expected displaced demand: {displaced_per_minute:.0f} guests/minute.",
        f"{settings.cypress_coil_id}: {cypress_percent}%.",
        f"{settings.stormglass_station_id}: {stormglass_percent}%.",
        f"Other experiences: {other_percent}%.",
    ]
