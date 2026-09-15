package com.deanwagman.lumenmarsh.venueops.maintenance.application;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceRecommendationId;

public class StaleMaintenanceRecommendationVersionException extends RuntimeException {

    private final MaintenanceRecommendationId recommendationId;
    private final long expectedVersion;
    private final long actualVersion;

    public StaleMaintenanceRecommendationVersionException(
            MaintenanceRecommendationId recommendationId,
            long expectedVersion,
            long actualVersion
    ) {
        super("Recommendation " + recommendationId
                + " is currently version " + actualVersion + ".");
        this.recommendationId = recommendationId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }

    public MaintenanceRecommendationId recommendationId() {
        return recommendationId;
    }

    public long expectedVersion() {
        return expectedVersion;
    }

    public long actualVersion() {
        return actualVersion;
    }
}
