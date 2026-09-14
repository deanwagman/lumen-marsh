from __future__ import annotations

import logging
from collections import defaultdict
from dataclasses import dataclass, field
from datetime import datetime

logger = logging.getLogger("environmental-monitor")


@dataclass
class MetricsRegistry:
    """In-process operational counters. No credentials are recorded."""

    provider_requests: int = 0
    provider_failures: int = 0
    provider_rate_limits: int = 0
    provider_latency_ms_total: float = 0.0
    poll_successes: dict[str, int] = field(default_factory=lambda: defaultdict(int))
    poll_failures: dict[str, int] = field(default_factory=lambda: defaultdict(int))
    recommendation_delivery_failures: int = 0
    last_venueops_success_at: datetime | None = None
    active_recommendations: int = 0
    observation_age_seconds: float | None = None

    def record_provider_call(
        self, *, duration_ms: float, success: bool, rate_limited: bool
    ) -> None:
        self.provider_requests += 1
        self.provider_latency_ms_total += duration_ms
        if rate_limited:
            self.provider_rate_limits += 1
        if not success:
            self.provider_failures += 1

    def snapshot(self) -> dict[str, object]:
        avg_latency = (
            self.provider_latency_ms_total / self.provider_requests
            if self.provider_requests
            else None
        )
        return {
            "providerRequestCount": self.provider_requests,
            "providerFailureCount": self.provider_failures,
            "providerRateLimitCount": self.provider_rate_limits,
            "providerAverageLatencyMs": avg_latency,
            "pollSuccessCounts": dict(self.poll_successes),
            "pollFailureCounts": dict(self.poll_failures),
            "activeRecommendationCount": self.active_recommendations,
            "recommendationDeliveryFailures": self.recommendation_delivery_failures,
            "lastVenueopsSuccessAt": (
                self.last_venueops_success_at.isoformat() if self.last_venueops_success_at else None
            ),
            "observationAgeSeconds": self.observation_age_seconds,
        }
