from __future__ import annotations

from reliability_intelligence.app_factory import create_app

app = create_app()


def run() -> None:
    import uvicorn

    uvicorn.run(
        "reliability_intelligence.main:app",
        host="0.0.0.0",
        port=8200,
        factory=False,
    )
