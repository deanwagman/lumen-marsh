from __future__ import annotations

import pytest
from pydantic import ValidationError

from tests.conftest import make_settings


def test_valid_settings() -> None:
    settings = make_settings()
    assert settings.park_latitude == 28.474
    assert settings.nws_user_agent.startswith("LumenMarsh")


def test_invalid_coordinates() -> None:
    with pytest.raises(ValidationError, match="PARK_LATITUDE"):
        make_settings(park_latitude=200)


def test_missing_nws_identification() -> None:
    with pytest.raises(ValidationError, match="NWS_USER_AGENT"):
        make_settings(nws_user_agent="short")


def test_placeholder_user_agent_rejected() -> None:
    with pytest.raises(ValidationError, match="placeholder"):
        make_settings(
            nws_user_agent="LumenMarshEnvironmentalMonitor/0.1 (your-contact-information)"
        )


def test_settings_are_constructed_without_process_globals() -> None:
    first = make_settings(park_latitude=28.1)
    second = make_settings(park_latitude=28.9)
    assert first.park_latitude == 28.1
    assert second.park_latitude == 28.9


def test_test_environment_allows_oidc_to_be_unconfigured() -> None:
    settings = make_settings()
    assert settings.oidc_enabled is False


def test_oidc_client_requires_secret_and_token_url() -> None:
    with pytest.raises(ValidationError, match="OIDC_CLIENT_SECRET"):
        make_settings(oidc_client_id="client-id")
    with pytest.raises(ValidationError, match="OIDC_TOKEN_URL"):
        make_settings(oidc_client_id="client-id", oidc_client_secret="client-secret")


def test_development_requires_oidc_or_explicit_disable() -> None:
    with pytest.raises(ValidationError, match="OIDC_CLIENT_ID"):
        make_settings(app_env="development")

    settings = make_settings(app_env="development", oidc_disabled=True)
    assert settings.oidc_enabled is False
