package com.deanwagman.lumenmarsh.venueops.maintenance.application;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceRecommendationId;

public class MaintenanceRecommendationNotFoundException extends RuntimeException {

    private final MaintenanceRecommendationId recommendationId;

    public MaintenanceRecommendationNotFoundException(MaintenanceRecommendationId recommendationId) {
        super("Maintenance recommendation not found: " + recommendationId);
        this.recommendationId = recommendationId;
    }

    public MaintenanceRecommendationId recommendationId() {
        return recommendationId;
    }
}
