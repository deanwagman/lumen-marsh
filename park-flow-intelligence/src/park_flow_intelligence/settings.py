"""Typed process configuration. All environment access stays in this module."""

from __future__ import annotations

from typing import Literal, Self

from pydantic import Field, field_validator, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Runtime configuration for Park Flow Intelligence."""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="forbid",
        case_sensitive=False,
    )

    app_env: Literal["development", "test", "production"] = "development"
    log_level: Literal["DEBUG", "INFO", "WARNING", "ERROR"] = "INFO"

    simulation_enabled: bool = True
    simulation_cycle_seconds: float = Field(default=15, gt=0)
    simulation_seed: int = 20260915
    window_seconds: int = Field(default=60, gt=0)

    venueops_base_url: str = "http://localhost:8080"
    venueops_timeout_seconds: float = Field(default=10, gt=0)
    venueops_max_retries: int = Field(default=3, ge=0)

    oidc_token_url: str = ""
    oidc_client_id: str = ""
    oidc_client_secret: str = ""
    oidc_scope: str = "venueops/flow-ingest.write"
    oidc_disabled: bool = False
    venueops_bearer_token: str = ""

    mangrove_run_id: str = "mangrove-run"
    cypress_coil_id: str = "cypress-coil"
    stormglass_station_id: str = "stormglass-station"

    displaced_demand_fraction: float = Field(default=0.80, ge=0, le=1)
    cypress_coil_weight: float = Field(default=0.50, ge=0, le=1)
    stormglass_station_weight: float = Field(default=0.30, ge=0, le=1)

    @field_validator("venueops_base_url")
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

    @model_validator(mode="after")
    def validate_auth_and_weights(self) -> Self:
        if self.cypress_coil_weight + self.stormglass_station_weight > 1.0:
            raise ValueError("Destination weights cannot exceed 1.0")
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
    def structured_logging(self) -> bool:
        return self.app_env != "development"

    @property
    def oidc_enabled(self) -> bool:
        return bool(self.oidc_client_id)

    @property
    def other_experience_weight(self) -> float:
        return max(0.0, 1.0 - self.cypress_coil_weight - self.stormglass_station_weight)
