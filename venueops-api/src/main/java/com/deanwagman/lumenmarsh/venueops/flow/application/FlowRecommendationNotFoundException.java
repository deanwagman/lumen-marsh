package com.deanwagman.lumenmarsh.venueops.flow.application;

import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationId;

public class FlowRecommendationNotFoundException extends RuntimeException {

    private final FlowRecommendationId recommendationId;

    public FlowRecommendationNotFoundException(FlowRecommendationId recommendationId) {
        super("Flow recommendation not found: " + recommendationId);
        this.recommendationId = recommendationId;
    }

    public FlowRecommendationId recommendationId() {
        return recommendationId;
    }
}
