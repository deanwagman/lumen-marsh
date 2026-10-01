from __future__ import annotations

from fastapi import APIRouter, HTTPException, Request

router = APIRouter(tags=["health"])


@router.get("/health/live")
async def live() -> dict[str, str]:
    return {"status": "ok"}


@router.get("/health/ready")
async def ready(request: Request) -> dict[str, str]:
    container = getattr(request.app.state, "container", None)
    if container is None or not getattr(container, "initialized", False):
        raise HTTPException(status_code=503, detail="not_ready")
    return {"status": "ok"}
