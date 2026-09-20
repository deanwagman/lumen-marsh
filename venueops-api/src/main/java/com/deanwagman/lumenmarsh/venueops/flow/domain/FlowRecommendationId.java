package com.deanwagman.lumenmarsh.venueops.flow.domain;

import java.util.Objects;
import java.util.UUID;

public record FlowRecommendationId(String value) {

    public FlowRecommendationId {
        Objects.requireNonNull(value, "recommendation id is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException("recommendation id must not be blank");
        }
    }

    public static FlowRecommendationId of(UUID id) {
        return new FlowRecommendationId(Objects.requireNonNull(id, "recommendation id is required").toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
