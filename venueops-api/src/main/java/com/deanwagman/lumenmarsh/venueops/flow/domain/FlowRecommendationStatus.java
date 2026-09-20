package com.deanwagman.lumenmarsh.venueops.flow.domain;

public enum FlowRecommendationStatus {
    PENDING_REVIEW,
    APPROVED,
    PUBLISHED,
    DISMISSED,
    WITHDRAWN,
    EXPIRED;

    public boolean isOpen() {
        return this == PENDING_REVIEW || this == APPROVED || this == PUBLISHED;
    }

    public boolean isPending() {
        return this == PENDING_REVIEW;
    }

    public boolean isPublished() {
        return this == PUBLISHED;
    }
}
