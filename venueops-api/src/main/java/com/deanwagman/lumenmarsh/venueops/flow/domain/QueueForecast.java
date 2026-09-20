package com.deanwagman.lumenmarsh.venueops.flow.domain;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record QueueForecast(
        UUID forecastId,
        AttractionId attractionId,
        Instant generatedAt,
        UUID basedOnObservationId,
        int horizonMinutes,
        int predictedQueueLength,
        int predictedWaitMinutes,
        ForecastConfidence confidence,
        List<String> assumptions,
        String explanation,
        boolean simulated
) {
    public static final List<Integer> REQUIRED_HORIZONS = List.of(15, 30, 60);

    public QueueForecast {
        Objects.requireNonNull(forecastId, "forecastId is required");
        Objects.requireNonNull(attractionId, "attractionId is required");
        Objects.requireNonNull(generatedAt, "generatedAt is required");
        Objects.requireNonNull(basedOnObservationId, "basedOnObservationId is required");
        Objects.requireNonNull(confidence, "confidence is required");
        Objects.requireNonNull(assumptions, "assumptions are required");
        Objects.requireNonNull(explanation, "explanation is required");
        if (!REQUIRED_HORIZONS.contains(horizonMinutes)) {
            throw new IllegalArgumentException("horizonMinutes must be 15, 30, or 60");
        }
        if (predictedQueueLength < 0 || predictedWaitMinutes < 0) {
            throw new IllegalArgumentException("predicted values cannot be negative");
        }
        if (explanation.isBlank()) {
            throw new IllegalArgumentException("explanation is required");
        }
        assumptions = List.copyOf(assumptions);
    }
}
