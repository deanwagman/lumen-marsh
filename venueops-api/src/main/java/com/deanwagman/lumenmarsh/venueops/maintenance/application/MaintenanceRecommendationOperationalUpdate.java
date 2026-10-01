package com.deanwagman.lumenmarsh.venueops.maintenance.application;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceRecommendation;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceRecommendationSeverity;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceRecommendationStatus;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceSignalType;

import java.time.Instant;
import java.util.UUID;

public record MaintenanceRecommendationOperationalUpdate(
        String eventId,
        Instant occurredAt,
        RecommendationSnapshot recommendation
) {
    public static final String UPDATED_EVENT = "maintenance.recommendation.updated";
    public static final String SNAPSHOT_EVENT = "maintenance.recommendations.snapshot";

    public String sseEventName() {
        return UPDATED_EVENT;
    }

    public static MaintenanceRecommendationOperationalUpdate from(MaintenanceRecommendation recommendation) {
        return new MaintenanceRecommendationOperationalUpdate(
                recommendation.id() + ":" + recommendation.version(),
                recommendation.updatedAt(),
                RecommendationSnapshot.from(recommendation)
        );
    }

    public record RecommendationSnapshot(
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
        public static RecommendationSnapshot from(MaintenanceRecommendation recommendation) {
            return new RecommendationSnapshot(
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
}
