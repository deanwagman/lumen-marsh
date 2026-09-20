package com.deanwagman.lumenmarsh.venueops.flow.application;

import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationStatus;
import com.deanwagman.lumenmarsh.venueops.flow.domain.QueueFreshness;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public class FlowMetrics {

    private final QueueProjectionRepository projections;
    private final FlowRecommendationRepository recommendations;
    private final Clock clock;
    private final Counter received;
    private final Counter replayed;
    private final Counter rejected;
    private final Counter commands;

    public FlowMetrics(
            MeterRegistry meterRegistry,
            QueueProjectionRepository projections,
            FlowRecommendationRepository recommendations,
            Clock clock
    ) {
        this.projections = Objects.requireNonNull(projections);
        this.recommendations = Objects.requireNonNull(recommendations);
        this.clock = Objects.requireNonNull(clock);
        this.received = Counter.builder("venueops.flow.observations.received").register(meterRegistry);
        this.replayed = Counter.builder("venueops.flow.observations.replayed").register(meterRegistry);
        this.rejected = Counter.builder("venueops.flow.observations.rejected").register(meterRegistry);
        this.commands = Counter.builder("venueops.flow.commands").register(meterRegistry);
        Gauge.builder("venueops.flow.recommendations.active", this, FlowMetrics::activeRecommendations)
                .register(meterRegistry);
        Gauge.builder("venueops.flow.stale.attractions", this, FlowMetrics::staleAttractions)
                .register(meterRegistry);
    }

    public void received() {
        received.increment();
    }

    public void replayed() {
        replayed.increment();
    }

    public void rejected() {
        rejected.increment();
    }

    public void command() {
        commands.increment();
    }

    private double activeRecommendations() {
        return recommendations.findAll().stream()
                .filter(recommendation -> recommendation.status() == FlowRecommendationStatus.PENDING_REVIEW
                        || recommendation.status() == FlowRecommendationStatus.APPROVED
                        || recommendation.status() == FlowRecommendationStatus.PUBLISHED)
                .count();
    }

    private double staleAttractions() {
        Instant now = Instant.now(clock);
        return projections.findAll().stream()
                .filter(projection -> projection.freshness(now) == QueueFreshness.STALE)
                .count();
    }
}
