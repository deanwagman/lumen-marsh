from __future__ import annotations


class StaticTokenProvider:
    """Fixed bearer token for LOCAL_JWT Compose bridges (never for production Cognito)."""

    def __init__(self, access_token: str) -> None:
        self._access_token = access_token

    async def get_access_token(self) -> str:
        return self._access_token

    async def aclose(self) -> None:
        return None
