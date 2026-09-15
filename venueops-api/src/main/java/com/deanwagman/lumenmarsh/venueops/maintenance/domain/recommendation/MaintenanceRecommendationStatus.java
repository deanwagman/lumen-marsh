package com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation;

public enum MaintenanceRecommendationStatus {
    PENDING_REVIEW,
    ACCEPTED,
    DISMISSED,
    WORK_ORDER_CREATED;

    public boolean isPending() {
        return this == PENDING_REVIEW;
    }
}
