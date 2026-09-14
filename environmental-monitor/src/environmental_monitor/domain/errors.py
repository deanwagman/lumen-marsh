from __future__ import annotations


class ProviderError(Exception):
    """Provider-neutral failure. Infrastructure maps vendor errors here."""

    category = "provider_error"

    def __init__(self, message: str, *, category: str | None = None) -> None:
        super().__init__(message)
        if category is not None:
            self.category = category


class ProviderUnavailableError(ProviderError):
    category = "unavailable"


class ProviderRateLimitedError(ProviderError):
    category = "rate_limited"

    def __init__(self, message: str, *, retry_after_seconds: float | None = None) -> None:
        super().__init__(message)
        self.retry_after_seconds = retry_after_seconds


class ProviderInvalidResponseError(ProviderError):
    category = "invalid_response"


class ProviderClientError(ProviderError):
    category = "client_error"
