from __future__ import annotations

from tests.conftest import make_observation

from environmental_monitor.infrastructure.persistence.memory import InMemoryObservationRepository


async def test_observation_upsert_is_idempotent() -> None:
    repo = InMemoryObservationRepository()
    first = make_observation(source="same")
    duplicate = make_observation(source="same", wind_gust_mps=99.0)
    stored = await repo.upsert(first)
    again = await repo.upsert(duplicate)
    assert stored.wind_gust_mps == 5.0
    assert again.observation_id == stored.observation_id
    latest = await repo.latest()
    assert latest is not None
    assert latest.raw_source_id == "same"
