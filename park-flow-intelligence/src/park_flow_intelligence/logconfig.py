from __future__ import annotations

import json
import logging
import sys
from contextvars import ContextVar
from datetime import UTC, datetime
from typing import Any

request_id_var: ContextVar[str | None] = ContextVar("request_id", default=None)

_LOG_KEYS = (
    "environment",
    "correlation_id",
    "observation_id",
    "recommendation_id",
    "attraction_id",
    "command_id",
    "actor_type",
    "simulated",
    "operation",
    "success",
    "error_category",
)


class JsonLogFormatter(logging.Formatter):
    def format(self, record: logging.LogRecord) -> str:
        payload: dict[str, Any] = {
            "timestamp": datetime.now(UTC).isoformat(),
            "level": record.levelname,
            "service": "park-flow-intelligence",
            "logger": record.name,
            "message": record.getMessage(),
        }
        request_id = request_id_var.get()
        if request_id:
            payload["request_id"] = request_id
        for key in _LOG_KEYS:
            value = getattr(record, key, None)
            if value is not None:
                payload[key] = value
        if record.exc_info:
            payload["error_category"] = payload.get("error_category", "unhandled")
            payload["exception"] = self.formatException(record.exc_info)
        return json.dumps(payload, default=str)


def configure_logging(*, level: str, structured: bool, environment: str) -> None:
    handler = logging.StreamHandler(sys.stdout)
    if structured:
        handler.setFormatter(JsonLogFormatter())
    else:
        handler.setFormatter(logging.Formatter("%(asctime)s %(levelname)s [%(name)s] %(message)s"))
    root = logging.getLogger()
    root.handlers.clear()
    root.addHandler(handler)
    root.setLevel(level)
    logging.getLogger("park-flow-intelligence").setLevel(level)
    logging.getLogger("park-flow-intelligence").debug(
        "logging configured", extra={"environment": environment}
    )
