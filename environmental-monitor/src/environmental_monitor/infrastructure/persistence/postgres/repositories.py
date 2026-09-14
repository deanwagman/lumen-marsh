from __future__ import annotations

from collections.abc import Sequence

from sqlalchemy import delete, select
from sqlalchemy.ext.asyncio import (
    AsyncEngine,
    AsyncSession,
    async_sessionmaker,
    create_async_engine,
)

from environmental_monitor.domain.alert import WeatherAlert
from environmental_monitor.domain.forecast import WeatherForecastPeriod
from environmental_monitor.domain.observation import WeatherObservation
from environmental_monitor.domain.poll import DatasetType, PollAttempt
from environmental_monitor.domain.recommendation import (
    RecommendationSeverity,
    RecommendationStatus,
    WeatherRecommendation,
)
from environmental_monitor.infrastructure.persistence.postgres.models import (
    AlertRow,
    ForecastPeriodRow,
    ObservationRow,
    PollAttemptRow,
    RecommendationRow,
)


def create_session_factory(
    database_url: str,
) -> tuple[AsyncEngine, async_sessionmaker[AsyncSession]]:
    engine = create_async_engine(database_url, pool_pre_ping=True)
    return engine, async_sessionmaker(engine, expire_on_commit=False)


class PostgresObservationRepository:
    def __init__(self, sessions: async_sessionmaker[AsyncSession]) -> None:
        self._sessions = sessions

    async def upsert(self, observation: WeatherObservation) -> WeatherObservation:
        async with self._sessions() as session:
            existing = await session.get(ObservationRow, observation.observation_id)
            if existing is None:
                by_source = await session.scalar(
                    select(ObservationRow).where(
                        ObservationRow.provider == observation.provider,
                        ObservationRow.raw_source_id == observation.raw_source_id,
                    )
                )
                if by_source is not None:
                    return _observation_from_row(by_source)
                session.add(_observation_to_row(observation))
                await session.commit()
                return observation
            return _observation_from_row(existing)

    async def latest(self) -> WeatherObservation | None:
        async with self._sessions() as session:
            row = await session.scalar(
                select(ObservationRow).order_by(ObservationRow.observed_at.desc()).limit(1)
            )
            return _observation_from_row(row) if row else None


class PostgresAlertRepository:
    def __init__(self, sessions: async_sessionmaker[AsyncSession]) -> None:
        self._sessions = sessions

    async def replace_active(self, alerts: Sequence[WeatherAlert]) -> None:
        async with self._sessions() as session:
            for alert in alerts:
                existing = await session.get(AlertRow, alert.provider_alert_id)
                if existing is None:
                    session.add(_alert_to_row(alert))
                else:
                    existing.event = alert.event
                    existing.severity = alert.severity
                    existing.urgency = alert.urgency
                    existing.certainty = alert.certainty
                    existing.headline = alert.headline
                    existing.description = alert.description
                    existing.instructions = alert.instructions
                    existing.onset = alert.onset
                    existing.expires_at = alert.expires_at
            await session.commit()

    async def list_active(self) -> list[WeatherAlert]:
        async with self._sessions() as session:
            rows = await session.scalars(select(AlertRow))
            return [_alert_from_row(row) for row in rows]


class PostgresForecastRepository:
    def __init__(self, sessions: async_sessionmaker[AsyncSession]) -> None:
        self._sessions = sessions

    async def replace_current(self, periods: Sequence[WeatherForecastPeriod]) -> None:
        async with self._sessions() as session:
            await session.execute(delete(ForecastPeriodRow))
            for period in periods:
                session.add(
                    ForecastPeriodRow(
                        starts_at=period.starts_at,
                        ends_at=period.ends_at,
                        temperature_c=period.temperature_c,
                        wind_speed_mps=period.wind_speed_mps,
                        wind_gust_mps=period.wind_gust_mps,
                        precipitation_probability_percent=period.precipitation_probability_percent,
                        summary=period.summary,
                    )
                )
            await session.commit()

    async def list_current(self) -> list[WeatherForecastPeriod]:
        async with self._sessions() as session:
            rows = await session.scalars(
                select(ForecastPeriodRow).order_by(ForecastPeriodRow.starts_at)
            )
            return [
                WeatherForecastPeriod(
                    starts_at=row.starts_at,
                    ends_at=row.ends_at,
                    temperature_c=row.temperature_c,
                    wind_speed_mps=row.wind_speed_mps,
                    wind_gust_mps=row.wind_gust_mps,
                    precipitation_probability_percent=row.precipitation_probability_percent,
                    summary=row.summary,
                )
                for row in rows
            ]


class PostgresRecommendationRepository:
    def __init__(self, sessions: async_sessionmaker[AsyncSession]) -> None:
        self._sessions = sessions

    async def save(self, recommendation: WeatherRecommendation) -> None:
        async with self._sessions() as session:
            existing = await session.get(RecommendationRow, recommendation.id)
            if existing is None:
                session.add(_recommendation_to_row(recommendation))
            else:
                existing.rule_id = recommendation.rule_id
                existing.status = recommendation.status.value
                existing.severity = recommendation.severity.value
                existing.summary = recommendation.summary
                existing.evidence = recommendation.evidence
                existing.recommended_action = recommendation.recommended_action
                existing.affected_attraction_ids = list(recommendation.affected_attraction_ids)
                existing.updated_at = recommendation.updated_at
                existing.version = recommendation.version
                existing.last_delivered_version = recommendation.last_delivered_version
            await session.commit()

    async def get(self, recommendation_id: str) -> WeatherRecommendation | None:
        async with self._sessions() as session:
            row = await session.get(RecommendationRow, recommendation_id)
            return _recommendation_from_row(row) if row else None

    async def list_all(self) -> list[WeatherRecommendation]:
        async with self._sessions() as session:
            rows = await session.scalars(select(RecommendationRow))
            return [_recommendation_from_row(row) for row in rows]

    async def active_by_rule(self, rule_id: str) -> WeatherRecommendation | None:
        async with self._sessions() as session:
            row = await session.scalar(
                select(RecommendationRow).where(
                    RecommendationRow.rule_id == rule_id,
                    RecommendationRow.status == RecommendationStatus.ACTIVE.value,
                )
            )
            return _recommendation_from_row(row) if row else None

    async def pending_delivery(self) -> list[WeatherRecommendation]:
        items = await self.list_all()
        return [item for item in items if item.needs_delivery]


class PostgresPollAttemptRepository:
    def __init__(self, sessions: async_sessionmaker[AsyncSession]) -> None:
        self._sessions = sessions

    async def record(self, attempt: PollAttempt) -> None:
        async with self._sessions() as session:
            session.add(
                PollAttemptRow(
                    dataset=attempt.dataset.value,
                    attempted_at=attempt.attempted_at,
                    succeeded=attempt.succeeded,
                    duration_ms=attempt.duration_ms,
                    error_category=attempt.error_category,
                    error_summary=attempt.error_summary,
                )
            )
            await session.commit()

    async def recent(self, limit: int = 50) -> list[PollAttempt]:
        async with self._sessions() as session:
            rows = await session.scalars(
                select(PollAttemptRow).order_by(PollAttemptRow.attempted_at.desc()).limit(limit)
            )
            return [
                PollAttempt(
                    dataset=DatasetType(row.dataset),
                    attempted_at=row.attempted_at,
                    succeeded=row.succeeded,
                    duration_ms=row.duration_ms,
                    error_category=row.error_category,
                    error_summary=row.error_summary,
                )
                for row in rows
            ]


def _observation_to_row(observation: WeatherObservation) -> ObservationRow:
    return ObservationRow(
        observation_id=observation.observation_id,
        provider=observation.provider,
        station_id=observation.station_id,
        observed_at=observation.observed_at,
        received_at=observation.received_at,
        temperature_c=observation.temperature_c,
        relative_humidity_percent=observation.relative_humidity_percent,
        wind_speed_mps=observation.wind_speed_mps,
        wind_gust_mps=observation.wind_gust_mps,
        wind_direction_degrees=observation.wind_direction_degrees,
        precipitation_mm=observation.precipitation_mm,
        visibility_m=observation.visibility_m,
        present_weather=observation.present_weather,
        raw_source_id=observation.raw_source_id,
    )


def _observation_from_row(row: ObservationRow) -> WeatherObservation:
    return WeatherObservation(
        observation_id=row.observation_id,
        provider=row.provider,
        station_id=row.station_id,
        observed_at=row.observed_at,
        received_at=row.received_at,
        temperature_c=row.temperature_c,
        relative_humidity_percent=row.relative_humidity_percent,
        wind_speed_mps=row.wind_speed_mps,
        wind_gust_mps=row.wind_gust_mps,
        wind_direction_degrees=row.wind_direction_degrees,
        precipitation_mm=row.precipitation_mm,
        visibility_m=row.visibility_m,
        present_weather=row.present_weather,
        raw_source_id=row.raw_source_id,
    )


def _alert_to_row(alert: WeatherAlert) -> AlertRow:
    return AlertRow(
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
        received_at=alert.received_at,
    )


def _alert_from_row(row: AlertRow) -> WeatherAlert:
    return WeatherAlert(
        provider_alert_id=row.provider_alert_id,
        event=row.event,
        severity=row.severity,
        urgency=row.urgency,
        certainty=row.certainty,
        headline=row.headline,
        description=row.description,
        instructions=row.instructions,
        onset=row.onset,
        expires_at=row.expires_at,
        received_at=row.received_at,
    )


def _recommendation_to_row(recommendation: WeatherRecommendation) -> RecommendationRow:
    return RecommendationRow(
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
        last_delivered_version=recommendation.last_delivered_version,
    )


def _recommendation_from_row(row: RecommendationRow) -> WeatherRecommendation:
    return WeatherRecommendation(
        id=row.id,
        rule_id=row.rule_id,
        status=RecommendationStatus(row.status),
        severity=RecommendationSeverity(row.severity),
        summary=row.summary,
        evidence=row.evidence,
        recommended_action=row.recommended_action,
        affected_attraction_ids=tuple(row.affected_attraction_ids),
        created_at=row.created_at,
        updated_at=row.updated_at,
        version=row.version,
        last_delivered_version=row.last_delivered_version,
    )
