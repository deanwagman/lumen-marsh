from __future__ import annotations

from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field

from environmental_monitor.domain.alert import WeatherAlert
from environmental_monitor.domain.forecast import WeatherForecastPeriod
from environmental_monitor.domain.lightning import LightningObservation
from environmental_monitor.domain.observation import WeatherObservation
from environmental_monitor.domain.recommendation import WeatherRecommendation
from environmental_monitor.domain.snapshot import DatasetHealth
from environmental_monitor.domain.station import WeatherStation
from environmental_monitor.domain.timeutils import to_park_timezone


def _to_camel(name: str) -> str:
    parts = name.split("_")
    return parts[0] + "".join(part.title() for part in parts[1:])


class ApiModel(BaseModel):
    model_config = ConfigDict(alias_generator=_to_camel, populate_by_name=True)


class DatasetHealthResponse(ApiModel):
    state: str
    last_success_at: datetime | None
    last_attempt_at: datetime | None
    last_error_category: str | None
    last_error_summary: str | None
    age_seconds: float | None

    @classmethod
    def from_health(cls, health: DatasetHealth) -> DatasetHealthResponse:
        return cls(
            state=health.state.value,
            last_success_at=health.last_success_at,
            last_attempt_at=health.last_attempt_at,
            last_error_category=health.last_error_category,
            last_error_summary=health.last_error_summary,
            age_seconds=health.age_seconds,
        )


class ObservationResponse(ApiModel):
    observation_id: str
    provider: str
    station_id: str
    observed_at: datetime
    park_local_time: datetime
    received_at: datetime
    temperature_c: float | None
    relative_humidity_percent: float | None
    wind_speed_mps: float | None
    wind_gust_mps: float | None
    wind_direction_degrees: float | None
    precipitation_mm: float | None
    visibility_m: float | None
    present_weather: str | None
    freshness: DatasetHealthResponse

    @classmethod
    def from_domain(
        cls,
        observation: WeatherObservation,
        health: DatasetHealth,
        timezone_name: str,
    ) -> ObservationResponse:
        return cls(
            observation_id=observation.observation_id,
            provider=observation.provider,
            station_id=observation.station_id,
            observed_at=observation.observed_at,
            park_local_time=to_park_timezone(observation.observed_at, timezone_name),
            received_at=observation.received_at,
            temperature_c=observation.temperature_c,
            relative_humidity_percent=observation.relative_humidity_percent,
            wind_speed_mps=observation.wind_speed_mps,
            wind_gust_mps=observation.wind_gust_mps,
            wind_direction_degrees=observation.wind_direction_degrees,
            precipitation_mm=observation.precipitation_mm,
            visibility_m=observation.visibility_m,
            present_weather=observation.present_weather,
            freshness=DatasetHealthResponse.from_health(health),
        )


class ForecastPeriodResponse(ApiModel):
    starts_at: datetime
    ends_at: datetime
    temperature_c: float | None
    wind_speed_mps: float | None
    wind_gust_mps: float | None
    precipitation_probability_percent: float | None
    summary: str

    @classmethod
    def from_domain(cls, period: WeatherForecastPeriod) -> ForecastPeriodResponse:
        return cls(
            starts_at=period.starts_at,
            ends_at=period.ends_at,
            temperature_c=period.temperature_c,
            wind_speed_mps=period.wind_speed_mps,
            wind_gust_mps=period.wind_gust_mps,
            precipitation_probability_percent=period.precipitation_probability_percent,
            summary=period.summary,
        )


class AlertResponse(ApiModel):
    provider_alert_id: str
    event: str
    severity: str
    urgency: str
    certainty: str
    headline: str
    description: str
    instructions: str
    onset: datetime | None
    expires_at: datetime | None

    @classmethod
    def from_domain(cls, alert: WeatherAlert) -> AlertResponse:
        return cls(
            provider_alert_id=alert.provider_alert_id,
            event=alert.event,
            severity=alert.severity,
            urgency=alert.urgency,
            certainty=alert.certainty,
            headline=alert.headline,
            description=alert.description,
            instructions=alert.instructions,
            onset=alert.onset,
            expires_at=alert.expires_at,
        )


class RecommendationResponse(ApiModel):
    id: str
    rule_id: str
    status: str
    severity: str
    summary: str
    evidence: str
    recommended_action: str
    affected_attraction_ids: list[str]
    created_at: datetime
    updated_at: datetime
    version: int

    @classmethod
    def from_domain(cls, recommendation: WeatherRecommendation) -> RecommendationResponse:
        return cls(
            id=recommendation.id,
            rule_id=recommendation.rule_id,
            status=recommendation.status.value,
            severity=recommendation.severity.value,
            summary=recommendation.summary,
            evidence=recommendation.evidence,
            recommended_action=recommendation.recommended_action,
            affected_attraction_ids=list(recommendation.affected_attraction_ids),
            created_at=recommendation.created_at,
            updated_at=recommendation.updated_at,
            version=recommendation.version,
        )


class StationResponse(ApiModel):
    station_id: str
    name: str
    latitude: float
    longitude: float
    provider: str

    @classmethod
    def from_domain(cls, station: WeatherStation) -> StationResponse:
        return cls(
            station_id=station.station_id,
            name=station.name,
            latitude=station.latitude,
            longitude=station.longitude,
            provider=station.provider,
        )


class LightningResponse(ApiModel):
    observation_id: str
    provider: str
    simulated: bool
    distance_miles: float
    bearing_degrees: float
    observed_at: datetime
    scenario: str

    @classmethod
    def from_domain(cls, observation: LightningObservation) -> LightningResponse:
        return cls(
            observation_id=observation.observation_id,
            provider=observation.provider,
            simulated=observation.simulated,
            distance_miles=observation.distance_miles,
            bearing_degrees=observation.bearing_degrees,
            observed_at=observation.observed_at,
            scenario=observation.scenario.value,
        )


class StatusResponse(ApiModel):
    provider_state: str
    current_station: StationResponse | None
    observation: DatasetHealthResponse
    forecast: DatasetHealthResponse
    alerts: DatasetHealthResponse
    lightning: DatasetHealthResponse
    venueops_delivery: DatasetHealthResponse
    metrics: dict[str, object]


class SimulationLightningRequest(ApiModel):
    distance_miles: float = Field(ge=0)
    bearing_degrees: float = Field(ge=0, le=360)
    observed_at: datetime | None = None
