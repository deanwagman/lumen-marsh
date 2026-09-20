package com.deanwagman.lumenmarsh.venueops.flow.api;

import com.deanwagman.lumenmarsh.venueops.flow.domain.QueueObservationSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record IngestFlowObservationRequest(
        @NotNull UUID observationId,
        @NotBlank String attractionId,
        @NotNull Instant observedAt,
        @NotNull Integer windowSeconds,
        @NotNull Integer queueLength,
        @NotNull Integer arrivals,
        @NotNull Integer boarded,
        @NotNull Integer operatingUnits,
        @NotNull Integer configuredUnits,
        @NotNull QueueObservationSourceType sourceType,
        @NotNull Boolean simulated
) {
}
