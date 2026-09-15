package com.deanwagman.lumenmarsh.venueops.maintenance.api;

import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceRecommendationService;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceRecommendationStatus;

import java.time.Instant;
import java.util.UUID;

public record ReliabilityRecommendationIngestResponse(
        UUID recommendationId,
        MaintenanceRecommendationStatus status,
        boolean duplicate,
        Instant receivedAt
) {
    public static ReliabilityRecommendationIngestResponse from(
            MaintenanceRecommendationService.RecommendationIngestResult result
    ) {
        return new ReliabilityRecommendationIngestResponse(
                result.recommendation().id().value(),
                result.recommendation().status(),
                result.duplicate(),
                result.recommendation().receivedAt()
        );
    }
}
