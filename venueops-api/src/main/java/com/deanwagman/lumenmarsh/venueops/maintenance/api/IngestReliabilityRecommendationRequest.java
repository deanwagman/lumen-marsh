package com.deanwagman.lumenmarsh.venueops.maintenance.api;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceRecommendationSeverity;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceSignalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record IngestReliabilityRecommendationRequest(
        @NotBlank String observationId,
        @NotNull Instant observedAt,
        @NotBlank String assetCode,
        @NotNull MaintenanceSignalType signalType,
        @NotNull MaintenanceRecommendationSeverity severity,
        Double value,
        String unit,
        @NotBlank String evidence,
        @NotBlank String recommendedAction
) {
}
