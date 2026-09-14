from __future__ import annotations

from environmental_monitor.app_factory import create_app

app = create_app()


def run() -> None:
    import uvicorn

    uvicorn.run(
        "environmental_monitor.main:app",
        host="0.0.0.0",
        port=8080,
        factory=False,
    )
