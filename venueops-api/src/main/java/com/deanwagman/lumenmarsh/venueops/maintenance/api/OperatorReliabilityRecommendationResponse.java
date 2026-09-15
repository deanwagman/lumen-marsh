package com.deanwagman.lumenmarsh.venueops.maintenance.api;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceRecommendation;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceRecommendationSeverity;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceRecommendationStatus;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceSignalType;

import java.time.Instant;
import java.util.UUID;

public record OperatorReliabilityRecommendationResponse(
        UUID recommendationId,
        String observationId,
        MaintenanceRecommendationStatus status,
        String assetCode,
        UUID assetId,
        MaintenanceSignalType signalType,
        MaintenanceRecommendationSeverity severity,
        Double value,
        String unit,
        String evidence,
        String recommendedAction,
        UUID workOrderId,
        Instant observedAt,
        Instant receivedAt,
        Instant updatedAt,
        long version
) {
    public static OperatorReliabilityRecommendationResponse from(MaintenanceRecommendation recommendation) {
        return new OperatorReliabilityRecommendationResponse(
                recommendation.id().value(),
                recommendation.observationId(),
                recommendation.status(),
                recommendation.assetCode(),
                recommendation.assetId().value(),
                recommendation.signalType(),
                recommendation.severity(),
                recommendation.value(),
                recommendation.unit(),
                recommendation.evidence(),
                recommendation.recommendedAction(),
                recommendation.workOrderId() == null ? null : recommendation.workOrderId().value(),
                recommendation.observedAt(),
                recommendation.receivedAt(),
                recommendation.updatedAt(),
                recommendation.version()
        );
    }
}
