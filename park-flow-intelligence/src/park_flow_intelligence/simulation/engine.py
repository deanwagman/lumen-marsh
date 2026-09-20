from __future__ import annotations

import random
from collections import defaultdict
from collections.abc import Mapping
from dataclasses import dataclass
from datetime import timedelta

from park_flow_intelligence.clock import Clock
from park_flow_intelligence.domain.models import (
    ForecastBundle,
    Freshness,
    QueueSample,
    RecommendationProposal,
    RecommendationSeverity,
    RecommendationType,
    Scenario,
    observation_id_for,
    recommendation_id_for,
)
from park_flow_intelligence.forecasting.engine import forecast_attraction
from park_flow_intelligence.forecasting.redistribution import (
    displaced_arrivals_per_minute,
    redistribution_assumptions,
    transfer_by_horizon,
)
from park_flow_intelligence.settings import Settings


@dataclass(frozen=True)
class AttractionProfile:
    attraction_id: str
    configured_units: int
    operating_units: int
    queue_length: int
    arrivals: int
    boarded: int


def _profiles(settings: Settings) -> dict[str, AttractionProfile]:
    return {
        settings.mangrove_run_id: AttractionProfile(
            attraction_id=settings.mangrove_run_id,
            configured_units=8,
            operating_units=8,
            queue_length=180,
            arrivals=16,
            boarded=16,
        ),
        settings.cypress_coil_id: AttractionProfile(
            attraction_id=settings.cypress_coil_id,
            configured_units=6,
            operating_units=6,
            queue_length=90,
            arrivals=10,
            boarded=10,
        ),
        settings.stormglass_station_id: AttractionProfile(
            attraction_id=settings.stormglass_station_id,
            configured_units=4,
            operating_units=4,
            queue_length=40,
            arrivals=6,
            boarded=6,
        ),
    }


class FlowSimulator:
    def __init__(self, settings: Settings, clock: Clock) -> None:
        self.settings = settings
        self.clock = clock
        self.scenario = Scenario.NORMAL
        self.running = False
        self.cycle = 0
        self.history: dict[str, list[QueueSample]] = defaultdict(list)
        self.last_recommendation_id: str | None = None

    def start(self) -> None:
        self.running = True

    def stop(self) -> None:
        self.running = False

    def apply_scenario(self, scenario: Scenario) -> None:
        self.scenario = scenario
        if scenario is Scenario.CLEAR:
            self.scenario = Scenario.NORMAL

    def snapshot(self) -> dict[str, object]:
        return {
            "scenario": self.scenario.value,
            "running": self.running,
            "cycle": self.cycle,
            "simulated": True,
        }

    def next_cycle(
        self,
    ) -> tuple[list[QueueSample], dict[str, ForecastBundle], RecommendationProposal | None]:
        self.cycle += 1
        now = self.clock.now()
        cycle_key = f"{self.scenario.value}:{self.cycle}"
        rng = random.Random(self.settings.simulation_seed + self.cycle)
        profiles = _profiles(self.settings)
        current = profiles_for_scenario(self.scenario, profiles, rng, self.settings)
        samples: list[QueueSample] = []
        for profile in current.values():
            sample = QueueSample(
                attraction_id=profile.attraction_id,
                observation_id=observation_id_for(
                    attraction_id=profile.attraction_id, cycle_key=cycle_key
                ),
                observed_at=now,
                window_seconds=self.settings.window_seconds,
                queue_length=profile.queue_length,
                arrivals=profile.arrivals,
                boarded=profile.boarded,
                operating_units=profile.operating_units,
                configured_units=profile.configured_units,
                simulated=True,
            )
            samples.append(sample)
            history = self.history[profile.attraction_id]
            history.insert(0, sample)
            del history[20:]

        extra: dict[str, list[str]] = {sample.attraction_id: [] for sample in samples}
        transfers: dict[str, dict[int, int]] = {sample.attraction_id: {} for sample in samples}
        if self.scenario is Scenario.MANGROVE_DISRUPTION:
            expected = (
                profiles[self.settings.mangrove_run_id].arrivals
                * 60.0
                / self.settings.window_seconds
            )
            displaced = displaced_arrivals_per_minute(
                expected_arrivals_per_minute=expected,
                settings=self.settings,
            )
            notes = redistribution_assumptions(
                source_id=self.settings.mangrove_run_id,
                displaced_per_minute=displaced,
                settings=self.settings,
            )
            for sample in samples:
                extra[sample.attraction_id] = notes
                transfers[sample.attraction_id] = transfer_by_horizon(
                    destination_id=sample.attraction_id,
                    displaced_per_minute=displaced,
                    settings=self.settings,
                )

        bundles: dict[str, ForecastBundle] = {}
        for sample in samples:
            previous = self.history[sample.attraction_id][1:]
            bundles[sample.attraction_id] = forecast_attraction(
                newest=sample,
                previous_newest_first=previous,
                now=now,
                disruption_transfer_by_horizon=transfers[sample.attraction_id],
                extra_assumptions=extra[sample.attraction_id],
            )

        recommendation = None
        mangrove_bundle = bundles[self.settings.mangrove_run_id]
        if (
            self.scenario is Scenario.MANGROVE_DISRUPTION
            and mangrove_bundle.freshness is not Freshness.STALE
            and mangrove_bundle.confidence is not None
        ):
            recommendation = RecommendationProposal(
                recommendation_id=recommendation_id_for(scenario=self.scenario, cycle_key="active"),
                type=RecommendationType.GUEST_REDIRECTION,
                severity=RecommendationSeverity.WARNING,
                source_attraction_id=self.settings.mangrove_run_id,
                affected_attraction_ids=[
                    self.settings.mangrove_run_id,
                    self.settings.cypress_coil_id,
                    self.settings.stormglass_station_id,
                ],
                recommended_destination_ids=[
                    self.settings.cypress_coil_id,
                    self.settings.stormglass_station_id,
                ],
                summary=(
                    "Mangrove Run capacity is down; expect demand to shift to Cypress Coil "
                    "and Stormglass Station."
                ),
                explanation=(
                    "Simulated telemetry shows Mangrove Run boarding no guests. Forecasts rise at "
                    "Cypress Coil and Stormglass Station before posted waits catch up. "
                    "Weather holds, incidents, and maintenance work remain "
                    "human-owned in VenueOps. "
                    + " ".join(extra[self.settings.mangrove_run_id])
                ),
                guest_message=(
                    "Mangrove Run is temporarily unavailable. Cypress Coil and Stormglass Station "
                    "currently have shorter waits."
                ),
                expires_at=now + timedelta(hours=2),
            )
            self.last_recommendation_id = str(recommendation.recommendation_id)

        return samples, bundles, recommendation


def profiles_for_scenario(
    scenario: Scenario,
    base: Mapping[str, AttractionProfile],
    rng: random.Random,
    settings: Settings,
) -> dict[str, AttractionProfile]:
    jittered: dict[str, AttractionProfile] = {}
    for attraction_id, profile in base.items():
        jittered[attraction_id] = AttractionProfile(
            attraction_id=profile.attraction_id,
            configured_units=profile.configured_units,
            operating_units=profile.operating_units,
            queue_length=max(0, profile.queue_length + rng.randint(-8, 8)),
            arrivals=max(0, profile.arrivals + rng.randint(-1, 1)),
            boarded=max(0, profile.boarded + rng.randint(-1, 1)),
        )
    if scenario is Scenario.MANGROVE_DISRUPTION:
        mangrove = jittered[settings.mangrove_run_id]
        expected = displaced_arrivals_per_minute(
            expected_arrivals_per_minute=mangrove.arrivals * 60.0 / settings.window_seconds,
            settings=settings,
        )
        cypress = jittered[settings.cypress_coil_id]
        stormglass = jittered[settings.stormglass_station_id]
        cypress_extra = max(0, round(expected * settings.cypress_coil_weight))
        stormglass_extra = max(0, round(expected * settings.stormglass_station_weight))
        jittered[settings.mangrove_run_id] = AttractionProfile(
            attraction_id=mangrove.attraction_id,
            configured_units=mangrove.configured_units,
            operating_units=0,
            queue_length=max(mangrove.queue_length, 200),
            arrivals=2,
            boarded=0,
        )
        jittered[settings.cypress_coil_id] = AttractionProfile(
            attraction_id=cypress.attraction_id,
            configured_units=cypress.configured_units,
            operating_units=cypress.operating_units,
            queue_length=cypress.queue_length + cypress_extra * 4,
            arrivals=cypress.arrivals + cypress_extra,
            boarded=cypress.boarded,
        )
        jittered[settings.stormglass_station_id] = AttractionProfile(
            attraction_id=stormglass.attraction_id,
            configured_units=stormglass.configured_units,
            operating_units=stormglass.operating_units,
            queue_length=stormglass.queue_length + stormglass_extra * 4,
            arrivals=stormglass.arrivals + stormglass_extra,
            boarded=stormglass.boarded,
        )
    return jittered
