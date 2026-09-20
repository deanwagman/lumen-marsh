from __future__ import annotations

from park_flow_intelligence.domain.models import (
    Confidence,
    Freshness,
    classify_confidence,
    freshness_for,
)


def test_freshness_thresholds() -> None:
    assert freshness_for(120) is Freshness.FRESH
    assert freshness_for(121) is Freshness.DELAYED
    assert freshness_for(300) is Freshness.DELAYED
    assert freshness_for(301) is Freshness.STALE


def test_confidence_classification() -> None:
    assert (
        classify_confidence(fresh_sample_count=6, freshness=Freshness.FRESH, intervals_stable=True)
        is Confidence.HIGH
    )
    assert (
        classify_confidence(fresh_sample_count=4, freshness=Freshness.FRESH, intervals_stable=True)
        is Confidence.MEDIUM
    )
    assert (
        classify_confidence(fresh_sample_count=2, freshness=Freshness.FRESH, intervals_stable=False)
        is Confidence.LOW
    )
    assert (
        classify_confidence(
            fresh_sample_count=6, freshness=Freshness.DELAYED, intervals_stable=True
        )
        is Confidence.LOW
    )
    assert (
        classify_confidence(fresh_sample_count=6, freshness=Freshness.STALE, intervals_stable=True)
        is None
    )
