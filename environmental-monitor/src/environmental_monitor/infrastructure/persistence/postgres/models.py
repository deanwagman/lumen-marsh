from __future__ import annotations

from datetime import datetime

from sqlalchemy import Boolean, DateTime, Float, Integer, String, Text
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column


class Base(DeclarativeBase):
    pass


class ObservationRow(Base):
    __tablename__ = "weather_observation"

    observation_id: Mapped[str] = mapped_column(String(256), primary_key=True)
    provider: Mapped[str] = mapped_column(String(32), nullable=False)
    station_id: Mapped[str] = mapped_column(String(64), nullable=False)
    observed_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, index=True
    )
    received_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    temperature_c: Mapped[float | None] = mapped_column(Float)
    relative_humidity_percent: Mapped[float | None] = mapped_column(Float)
    wind_speed_mps: Mapped[float | None] = mapped_column(Float)
    wind_gust_mps: Mapped[float | None] = mapped_column(Float)
    wind_direction_degrees: Mapped[float | None] = mapped_column(Float)
    precipitation_mm: Mapped[float | None] = mapped_column(Float)
    visibility_m: Mapped[float | None] = mapped_column(Float)
    present_weather: Mapped[str | None] = mapped_column(Text)
    raw_source_id: Mapped[str] = mapped_column(String(256), nullable=False, unique=True)


class AlertRow(Base):
    __tablename__ = "weather_alert"

    provider_alert_id: Mapped[str] = mapped_column(String(512), primary_key=True)
    event: Mapped[str] = mapped_column(String(128), nullable=False)
    severity: Mapped[str] = mapped_column(String(64), nullable=False)
    urgency: Mapped[str] = mapped_column(String(64), nullable=False)
    certainty: Mapped[str] = mapped_column(String(64), nullable=False)
    headline: Mapped[str] = mapped_column(Text, nullable=False)
    description: Mapped[str] = mapped_column(Text, nullable=False)
    instructions: Mapped[str] = mapped_column(Text, nullable=False)
    onset: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    expires_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    received_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)


class ForecastPeriodRow(Base):
    __tablename__ = "weather_forecast_period"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    starts_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    ends_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, index=True)
    temperature_c: Mapped[float | None] = mapped_column(Float)
    wind_speed_mps: Mapped[float | None] = mapped_column(Float)
    wind_gust_mps: Mapped[float | None] = mapped_column(Float)
    precipitation_probability_percent: Mapped[float | None] = mapped_column(Float)
    summary: Mapped[str] = mapped_column(Text, nullable=False)


class RecommendationRow(Base):
    __tablename__ = "weather_recommendation"

    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    rule_id: Mapped[str] = mapped_column(String(128), nullable=False, index=True)
    status: Mapped[str] = mapped_column(String(32), nullable=False)
    severity: Mapped[str] = mapped_column(String(32), nullable=False)
    summary: Mapped[str] = mapped_column(Text, nullable=False)
    evidence: Mapped[str] = mapped_column(Text, nullable=False)
    recommended_action: Mapped[str] = mapped_column(Text, nullable=False)
    affected_attraction_ids: Mapped[list[str]] = mapped_column(JSONB, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    version: Mapped[int] = mapped_column(Integer, nullable=False)
    last_delivered_version: Mapped[int] = mapped_column(Integer, nullable=False, default=0)


class PollAttemptRow(Base):
    __tablename__ = "weather_poll_attempt"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    dataset: Mapped[str] = mapped_column(String(64), nullable=False)
    attempted_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, index=True
    )
    succeeded: Mapped[bool] = mapped_column(Boolean, nullable=False)
    duration_ms: Mapped[float] = mapped_column(Float, nullable=False)
    error_category: Mapped[str | None] = mapped_column(String(64))
    error_summary: Mapped[str | None] = mapped_column(Text)
