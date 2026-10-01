from __future__ import annotations

from prometheus_client import CollectorRegistry, Counter, Histogram, generate_latest


class MetricsRegistry:
    """Prometheus counters for the local simulator. Tokens are never recorded."""

    def __init__(self, registry: CollectorRegistry | None = None) -> None:
        self.registry = registry or CollectorRegistry()
        self.simulator_cycles = Counter(
            "reliability_simulator_cycles_total",
            "Simulator cycles completed",
            registry=self.registry,
        )
        self.recommendations_submitted = Counter(
            "reliability_recommendations_submitted_total",
            "Reliability recommendations posted to VenueOps",
            registry=self.registry,
        )
        self.submission_failures = Counter(
            "reliability_submission_failures_total",
            "Recommendation submissions that failed after retries",
            registry=self.registry,
        )
        self.submission_latency = Histogram(
            "reliability_recommendation_submission_latency_seconds",
            "VenueOps reliability ingest latency",
            registry=self.registry,
        )

    def render(self) -> bytes:
        return generate_latest(self.registry)
