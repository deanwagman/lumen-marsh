from __future__ import annotations

from fastapi import APIRouter, Request, Response

from reliability_intelligence.container import AppContainer

router = APIRouter(tags=["metrics"])


@router.get("/metrics")
async def metrics(request: Request) -> Response:
    container: AppContainer = request.app.state.container
    return Response(content=container.metrics.render(), media_type="text/plain; version=0.0.4")
