"""Typed process configuration. All environment access stays in this module."""

from __future__ import annotations

from typing import Literal, Self
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError

from pydantic import Field, field_validator, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Runtime configuration for Environmental Monitor.

    Required values fail at startup. Coordinates and polling intervals are
    configuration, not code constants. Credentials never belong here.
    """

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="forbid",
        case_sensitive=False,
    )

    app_env: Literal["development", "test", "production"] = "development"
    log_level: Literal["DEBUG", "INFO", "WARNING", "ERROR"] = "INFO"

    park_latitude: float
    park_longitude: float
    park_timezone: str = "America/New_York"

    nws_base_url: str = "https://api.weather.gov"
    nws_user_agent: str

    observation_poll_seconds: int = Field(default=300, gt=0)
    alert_poll_seconds: int = Field(default=60, gt=0)
    forecast_poll_seconds: int = Field(default=900, gt=0)
    location_metadata_refresh_seconds: int = Field(default=86400, gt=0)

    provider_timeout_seconds: float = Field(default=10, gt=0)
    provider_max_retries: int = Field(default=3, ge=0)

    simulation_enabled: bool = True

    venueops_base_url: str = "http://localhost:8080"
    venueops_timeout_seconds: float = Field(default=10, gt=0)
    venueops_max_retries: int = Field(default=3, ge=0)

    oidc_token_url: str = ""
    oidc_client_id: str = ""
    oidc_client_secret: str = ""
    oidc_scope: str = "venueops/weather-recommendations.write"
    oidc_disabled: bool = False
    # LOCAL_JWT Compose bridge only. Prefer Cognito client credentials in shared environments.
    venueops_bearer_token: str = ""

    database_url: str = ""

    # Fictional demonstration thresholds. Not real operating criteria.
    wind_gust_hold_mps: float = Field(default=15.0, gt=0)
    wind_gust_clear_mps: float = Field(default=12.0, gt=0)
    rainfall_hold_mm: float = Field(default=10.0, gt=0)
    rainfall_clear_mm: float = Field(default=5.0, gt=0)
    lightning_hold_radius_miles: float = Field(default=10.0, gt=0)
    lightning_clearance_minutes: int = Field(default=30, gt=0)

    mangrove_run_id: str = "mangrove-run"
    cypress_coil_id: str = "cypress-coil"
    stormglass_station_id: str = "stormglass-station"

    observation_stale_after_seconds: int = Field(default=600, gt=0)
    forecast_stale_after_seconds: int = Field(default=1800, gt=0)
    alert_stale_after_seconds: int = Field(default=120, gt=0)

    @field_validator("park_latitude")
    @classmethod
    def validate_latitude(cls, value: float) -> float:
        if not -90.0 <= value <= 90.0:
            raise ValueError("PARK_LATITUDE must be between -90 and 90")
        return value

    @field_validator("park_longitude")
    @classmethod
    def validate_longitude(cls, value: float) -> float:
        if not -180.0 <= value <= 180.0:
            raise ValueError("PARK_LONGITUDE must be between -180 and 180")
        return value

    @field_validator("nws_user_agent")
    @classmethod
    def validate_user_agent(cls, value: str) -> str:
        stripped = value.strip()
        if len(stripped) < 10:
            raise ValueError(
                "NWS_USER_AGENT must identify this application with contact information"
            )
        lowered = stripped.lower()
        has_contact = (
            "@" in stripped or "http://" in lowered or "https://" in lowered or "(" in stripped
        )
        if not has_contact:
            raise ValueError(
                "NWS_USER_AGENT must include contact information, for example "
                "'LumenMarshEnvironmentalMonitor/0.1 (ops@example.com)'"
            )
        if "your-contact-information" in lowered:
            raise ValueError(
                "NWS_USER_AGENT must replace the placeholder with real contact information"
            )
        return stripped

    @field_validator("park_timezone")
    @classmethod
    def validate_timezone(cls, value: str) -> str:
        try:
            ZoneInfo(value)
        except ZoneInfoNotFoundError as exc:
            raise ValueError(f"PARK_TIMEZONE is not a valid IANA timezone: {value}") from exc
        return value

    @field_validator("nws_base_url", "venueops_base_url")
    @classmethod
    def validate_base_url(cls, value: str) -> str:
        stripped = value.rstrip("/")
        if not stripped.startswith(("http://", "https://")):
            raise ValueError("Base URLs must start with http:// or https://")
        return stripped

    @field_validator(
        "oidc_token_url",
        "oidc_client_id",
        "oidc_client_secret",
        "oidc_scope",
        mode="before",
    )
    @classmethod
    def strip_oidc_values(cls, value: str) -> str:
        return value.strip()

    @field_validator("database_url")
    @classmethod
    def validate_database_url(cls, value: str) -> str:
        stripped = value.strip()
        if not stripped:
            return ""
        if stripped.startswith("postgresql://"):
            return stripped.replace("postgresql://", "postgresql+asyncpg://", 1)
        return stripped

    @model_validator(mode="after")
    def validate_thresholds(self) -> Self:
        if self.wind_gust_clear_mps >= self.wind_gust_hold_mps:
            raise ValueError("WIND_GUST_CLEAR_MPS must be below WIND_GUST_HOLD_MPS")
        if self.rainfall_clear_mm >= self.rainfall_hold_mm:
            raise ValueError("RAINFALL_CLEAR_MM must be below RAINFALL_HOLD_MM")
        if self.oidc_client_id:
            if self.oidc_disabled:
                raise ValueError("OIDC_DISABLED cannot be true when OIDC_CLIENT_ID is configured")
            if not self.oidc_client_secret:
                raise ValueError("OIDC_CLIENT_SECRET is required when OIDC_CLIENT_ID is configured")
            if not self.oidc_token_url:
                raise ValueError("OIDC_TOKEN_URL is required when OIDC_CLIENT_ID is configured")
            if not self.oidc_token_url.startswith(("http://", "https://")):
                raise ValueError("OIDC_TOKEN_URL must start with http:// or https://")
            if not self.oidc_scope:
                raise ValueError("OIDC_SCOPE cannot be empty")
        elif self.app_env != "test" and not self.oidc_disabled:
            raise ValueError(
                "OIDC_CLIENT_ID is required outside test; set OIDC_DISABLED=true "
                "only for unauthenticated local development"
            )
        if self.app_env == "production" and self.oidc_disabled:
            raise ValueError("OIDC_DISABLED cannot be true in production")
        if self.app_env == "production" and "simulation_enabled" not in self.model_fields_set:
            object.__setattr__(self, "simulation_enabled", False)
        return self

    @property
    def persistence_enabled(self) -> bool:
        return bool(self.database_url)

    @property
    def structured_logging(self) -> bool:
        return self.app_env != "development"

    @property
    def oidc_enabled(self) -> bool:
        return bool(self.oidc_client_id)
