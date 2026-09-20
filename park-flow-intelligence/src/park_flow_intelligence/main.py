from __future__ import annotations

from park_flow_intelligence.app_factory import create_app

app = create_app()


def run() -> None:
    import uvicorn

    uvicorn.run(
        "park_flow_intelligence.main:app",
        host="0.0.0.0",
        port=8100,
        factory=False,
    )
