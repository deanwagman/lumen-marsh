from __future__ import annotations

from prometheus_client import CollectorRegistry, Counter, Gauge, Histogram, generate_latest


class MetricsRegistry:
    """Prometheus counters for the local simulator. Tokens are never recorded."""

    def __init__(self, registry: CollectorRegistry | None = None) -> None:
        self.registry = registry or CollectorRegistry()
        self.simulator_cycles = Counter(
            "flow_simulator_cycles_total",
            "Simulator cycles completed",
            registry=self.registry,
        )
        self.forecasts_generated = Counter(
            "flow_forecasts_generated_total",
            "Forecast snapshots generated",
            registry=self.registry,
        )
        self.forecast_submission_failures = Counter(
            "flow_forecast_submission_failures_total",
            "Forecast submissions that failed after retries",
            registry=self.registry,
        )
        self.observation_latency = Histogram(
            "flow_observation_submission_latency_seconds",
            "VenueOps observation ingest latency",
            registry=self.registry,
        )
        self.source_data_age = Gauge(
            "flow_source_data_age_seconds",
            "Age of the newest accepted observation",
            ["attraction_id"],
            registry=self.registry,
        )

    def render(self) -> bytes:
        return generate_latest(self.registry)
