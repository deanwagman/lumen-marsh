package com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation;

import java.util.Objects;
import java.util.UUID;

public record MaintenanceRecommendationId(UUID value) {

    public MaintenanceRecommendationId {
        Objects.requireNonNull(value, "recommendation id is required");
    }

    public static MaintenanceRecommendationId of(String value) {
        Objects.requireNonNull(value, "recommendation id is required");
        return new MaintenanceRecommendationId(UUID.fromString(value));
    }

    public static MaintenanceRecommendationId random() {
        return new MaintenanceRecommendationId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
