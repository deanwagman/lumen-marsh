from __future__ import annotations

import asyncio
import secrets
from collections import OrderedDict
from collections.abc import Awaitable, Callable
from typing import cast
from uuid import UUID

from fastapi import APIRouter, Header, HTTPException, Request
from pydantic import BaseModel, ConfigDict, Field

from environmental_monitor.container import AppContainer

router = APIRouter(prefix="/internal/demo", tags=["internal"])

_MAX_COMPLETED_RESETS = 256


class DemoResetRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    reset_id: UUID = Field(alias="resetId")


class DemoResetRegistry:
    """Serializes resets and remembers a bounded set of completed request IDs."""

    def __init__(self, max_completed: int = _MAX_COMPLETED_RESETS) -> None:
        self._max_completed = max_completed
        self._completed: OrderedDict[UUID, None] = OrderedDict()
        self._lock = asyncio.Lock()

    async def execute(self, reset_id: UUID, operation: Callable[[], Awaitable[None]]) -> str:
        async with self._lock:
            if reset_id in self._completed:
                self._completed.move_to_end(reset_id)
                return "REPLAY"
            await operation()
            self._completed[reset_id] = None
            while len(self._completed) > self._max_completed:
                self._completed.popitem(last=False)
            return "COMPLETED"


def _container(request: Request) -> AppContainer:
    return cast(AppContainer, request.app.state.container)


def _authorize(container: AppContainer, supplied_secret: str | None) -> None:
    if not container.settings.demo_sandbox_enabled:
        raise HTTPException(status_code=404, detail="Not found")
    configured = container.settings.demo_sandbox_secret
    if (
        configured is None
        or supplied_secret is None
        or not secrets.compare_digest(supplied_secret, configured.get_secret_value())
    ):
        raise HTTPException(status_code=401, detail="Invalid demo sandbox secret")


@router.post("/reset")
async def reset_demo(
    body: DemoResetRequest,
    request: Request,
    demo_secret: str | None = Header(default=None, alias="X-Demo-Reset-Secret"),
) -> dict[str, str]:
    container = _container(request)
    _authorize(container, demo_secret)
    status = await container.demo_resets.execute(body.reset_id, container.reset_demo_state)
    return {"resetId": str(body.reset_id), "status": status}
