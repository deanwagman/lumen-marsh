package com.deanwagman.lumenmarsh.venueops.flow.domain;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record QueueObservation(
        UUID observationId,
        AttractionId attractionId,
        Instant observedAt,
        Instant receivedAt,
        int windowSeconds,
        int queueLength,
        int arrivals,
        int boarded,
        int operatingUnits,
        int configuredUnits,
        QueueObservationSourceType sourceType,
        boolean simulated
) {
    public static final Duration FUTURE_SKEW_LIMIT = Duration.ofMinutes(2);
    private static final int GUESTS_PER_UNIT_PER_MINUTE = 20;

    public QueueObservation {
        Objects.requireNonNull(observationId, "observationId is required");
        Objects.requireNonNull(attractionId, "attractionId is required");
        Objects.requireNonNull(observedAt, "observedAt is required");
        Objects.requireNonNull(receivedAt, "receivedAt is required");
        Objects.requireNonNull(sourceType, "sourceType is required");
        if (windowSeconds <= 0) {
            throw new InvalidFlowObservationException("windowSeconds must be positive.");
        }
        if (queueLength < 0 || arrivals < 0 || boarded < 0 || operatingUnits < 0 || configuredUnits < 0) {
            throw new InvalidFlowObservationException("Counts cannot be negative.");
        }
        if (configuredUnits < 1) {
            throw new InvalidFlowObservationException("configuredUnits must be at least 1.");
        }
        if (operatingUnits > configuredUnits) {
            throw new InvalidFlowObservationException("operatingUnits cannot exceed configuredUnits.");
        }
        if (sourceType == QueueObservationSourceType.SIMULATOR && !simulated) {
            throw new InvalidFlowObservationException("Simulator observations must be marked simulated.");
        }
        int windowMinutes = Math.max(1, (int) Math.ceil(windowSeconds / 60.0));
        int maxBoarded = configuredUnits * GUESTS_PER_UNIT_PER_MINUTE * windowMinutes;
        if (boarded > maxBoarded) {
            throw new InvalidFlowObservationException("boarded exceeds configured attraction capacity.");
        }
    }

    public static QueueObservation accept(
            UUID observationId,
            AttractionId attractionId,
            Instant observedAt,
            int windowSeconds,
            int queueLength,
            int arrivals,
            int boarded,
            int operatingUnits,
            int configuredUnits,
            QueueObservationSourceType sourceType,
            boolean simulated,
            Clock clock
    ) {
        Instant now = Instant.now(Objects.requireNonNull(clock, "clock is required"));
        if (observedAt.isAfter(now.plus(FUTURE_SKEW_LIMIT))) {
            throw new InvalidFlowObservationException("Observation timestamp is unreasonably far in the future.");
        }
        return new QueueObservation(
                observationId,
                attractionId,
                observedAt,
                now,
                windowSeconds,
                queueLength,
                arrivals,
                boarded,
                operatingUnits,
                configuredUnits,
                sourceType,
                simulated
        );
    }

    public double arrivalsPerMinute() {
        return arrivals * 60.0 / windowSeconds;
    }

    public double throughputPerMinute() {
        return boarded * 60.0 / windowSeconds;
    }
}
