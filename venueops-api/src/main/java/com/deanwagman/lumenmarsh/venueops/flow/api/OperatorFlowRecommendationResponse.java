package com.deanwagman.lumenmarsh.venueops.flow.api;

import com.deanwagman.lumenmarsh.venueops.flow.application.FlowSnapshots;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendation;

public record OperatorFlowRecommendationResponse(
        String recommendationId,
        FlowSnapshots.RecommendationSnapshot recommendation,
        boolean simulated
) {
    public static OperatorFlowRecommendationResponse from(FlowRecommendation recommendation) {
        return new OperatorFlowRecommendationResponse(
                recommendation.id().value(),
                FlowSnapshots.RecommendationSnapshot.from(recommendation),
                recommendation.simulated()
        );
    }
}
