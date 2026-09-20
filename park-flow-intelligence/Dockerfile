# syntax=docker/dockerfile:1

FROM python:3.13-slim-bookworm AS builder
COPY --from=ghcr.io/astral-sh/uv:0.12.8 /uv /uvx /bin/
WORKDIR /app
ENV UV_COMPILE_BYTECODE=1 UV_LINK_MODE=copy
COPY pyproject.toml uv.lock README.md ./
COPY src ./src
RUN uv sync --frozen --no-dev --no-editable

FROM python:3.13-slim-bookworm AS runtime
RUN useradd --create-home --uid 1000 appuser
WORKDIR /app
COPY --from=builder /app/.venv /app/.venv
COPY src ./src
ENV PATH="/app/.venv/bin:$PATH" \
    PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONPATH=/app/src \
    APP_ENV=production \
    SIMULATION_ENABLED=false
USER appuser
WORKDIR /home/appuser
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8080/health/live')"
CMD ["uvicorn", "park_flow_intelligence.main:app", "--host", "0.0.0.0", "--port", "8080"]
