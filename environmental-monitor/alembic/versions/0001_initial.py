"""create weather tables

Revision ID: 0001
Revises:
Create Date: 2026-09-01
"""

from __future__ import annotations

from collections.abc import Sequence

import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

from alembic import op

revision: str = "0001"
down_revision: str | None = None
branch_labels: Sequence[str] | None = None
depends_on: Sequence[str] | None = None


def upgrade() -> None:
    op.create_table(
        "weather_observation",
        sa.Column("observation_id", sa.String(length=256), primary_key=True),
        sa.Column("provider", sa.String(length=32), nullable=False),
        sa.Column("station_id", sa.String(length=64), nullable=False),
        sa.Column("observed_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("received_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("temperature_c", sa.Float(), nullable=True),
        sa.Column("relative_humidity_percent", sa.Float(), nullable=True),
        sa.Column("wind_speed_mps", sa.Float(), nullable=True),
        sa.Column("wind_gust_mps", sa.Float(), nullable=True),
        sa.Column("wind_direction_degrees", sa.Float(), nullable=True),
        sa.Column("precipitation_mm", sa.Float(), nullable=True),
        sa.Column("visibility_m", sa.Float(), nullable=True),
        sa.Column("present_weather", sa.Text(), nullable=True),
        sa.Column("raw_source_id", sa.String(length=256), nullable=False),
        sa.UniqueConstraint("raw_source_id"),
    )
    op.create_index("ix_weather_observation_observed_at", "weather_observation", ["observed_at"])

    op.create_table(
        "weather_alert",
        sa.Column("provider_alert_id", sa.String(length=512), primary_key=True),
        sa.Column("event", sa.String(length=128), nullable=False),
        sa.Column("severity", sa.String(length=64), nullable=False),
        sa.Column("urgency", sa.String(length=64), nullable=False),
        sa.Column("certainty", sa.String(length=64), nullable=False),
        sa.Column("headline", sa.Text(), nullable=False),
        sa.Column("description", sa.Text(), nullable=False),
        sa.Column("instructions", sa.Text(), nullable=False),
        sa.Column("onset", sa.DateTime(timezone=True), nullable=True),
        sa.Column("expires_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("received_at", sa.DateTime(timezone=True), nullable=False),
    )

    op.create_table(
        "weather_forecast_period",
        sa.Column("id", sa.Integer(), primary_key=True, autoincrement=True),
        sa.Column("starts_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("ends_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("temperature_c", sa.Float(), nullable=True),
        sa.Column("wind_speed_mps", sa.Float(), nullable=True),
        sa.Column("wind_gust_mps", sa.Float(), nullable=True),
        sa.Column("precipitation_probability_percent", sa.Float(), nullable=True),
        sa.Column("summary", sa.Text(), nullable=False),
    )
    op.create_index("ix_weather_forecast_period_ends_at", "weather_forecast_period", ["ends_at"])

    op.create_table(
        "weather_recommendation",
        sa.Column("id", sa.String(length=64), primary_key=True),
        sa.Column("rule_id", sa.String(length=128), nullable=False),
        sa.Column("status", sa.String(length=32), nullable=False),
        sa.Column("severity", sa.String(length=32), nullable=False),
        sa.Column("summary", sa.Text(), nullable=False),
        sa.Column("evidence", sa.Text(), nullable=False),
        sa.Column("recommended_action", sa.Text(), nullable=False),
        sa.Column("affected_attraction_ids", postgresql.JSONB(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("version", sa.Integer(), nullable=False),
        sa.Column("last_delivered_version", sa.Integer(), nullable=False, server_default="0"),
    )
    op.create_index("ix_weather_recommendation_rule_id", "weather_recommendation", ["rule_id"])

    op.create_table(
        "weather_poll_attempt",
        sa.Column("id", sa.Integer(), primary_key=True, autoincrement=True),
        sa.Column("dataset", sa.String(length=64), nullable=False),
        sa.Column("attempted_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("succeeded", sa.Boolean(), nullable=False),
        sa.Column("duration_ms", sa.Float(), nullable=False),
        sa.Column("error_category", sa.String(length=64), nullable=True),
        sa.Column("error_summary", sa.Text(), nullable=True),
    )
    op.create_index(
        "ix_weather_poll_attempt_attempted_at", "weather_poll_attempt", ["attempted_at"]
    )


def downgrade() -> None:
    op.drop_table("weather_poll_attempt")
    op.drop_table("weather_recommendation")
    op.drop_table("weather_forecast_period")
    op.drop_table("weather_alert")
    op.drop_table("weather_observation")
