package com.deanwagman.lumenmarsh.venueops.flow.domain;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record QueueProjection(
        AttractionId attractionId,
        UUID observationId,
        int queueLength,
        double arrivalsPerMinute,
        double throughputPerMinute,
        int calculatedWaitMinutes,
        int postedWaitMinutes,
        int operatingCapacityPercent,
        QueueTrend trend,
        Instant observedAt,
        Instant receivedAt,
        Instant updatedAt,
        long version,
        boolean simulated
) {
    public QueueProjection {
        Objects.requireNonNull(attractionId, "attractionId is required");
        Objects.requireNonNull(observationId, "observationId is required");
        Objects.requireNonNull(trend, "trend is required");
        Objects.requireNonNull(observedAt, "observedAt is required");
        Objects.requireNonNull(receivedAt, "receivedAt is required");
        Objects.requireNonNull(updatedAt, "updatedAt is required");
        if (version < 1) {
            throw new IllegalArgumentException("version must be at least 1");
        }
    }

    public QueueFreshness freshness(Instant now) {
        return QueueFreshness.of(observedAt, now);
    }
}
