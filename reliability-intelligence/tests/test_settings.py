from __future__ import annotations

import pytest
from pydantic import ValidationError
from tests.factory import make_settings


def test_oidc_disabled_is_required_without_client() -> None:
    with pytest.raises(ValidationError):
        make_settings(app_env="development", oidc_disabled=False, oidc_client_id="")


def test_production_cannot_disable_oidc() -> None:
    with pytest.raises(ValidationError):
        make_settings(app_env="production", oidc_disabled=True)
