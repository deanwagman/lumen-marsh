"""Timezone-aware timestamp helpers. Internal time is always UTC."""

from __future__ import annotations

from datetime import UTC, datetime
from zoneinfo import ZoneInfo


def ensure_utc(value: datetime, field_name: str) -> datetime:
    if value.tzinfo is None:
        raise ValueError(f"{field_name} must be timezone-aware")
    return value.astimezone(UTC)


def parse_datetime(value: str, field_name: str) -> datetime:
    normalized = value.replace("Z", "+00:00")
    parsed = datetime.fromisoformat(normalized)
    return ensure_utc(parsed, field_name)


def to_park_timezone(value: datetime, timezone_name: str) -> datetime:
    return ensure_utc(value, "value").astimezone(ZoneInfo(timezone_name))
