package com.deanwagman.lumenmarsh.venueops.flow.application;

import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationId;

public class StaleFlowRecommendationVersionException extends RuntimeException {

    private final FlowRecommendationId recommendationId;
    private final long expectedVersion;
    private final long actualVersion;

    public StaleFlowRecommendationVersionException(
            FlowRecommendationId recommendationId,
            long expectedVersion,
            long actualVersion
    ) {
        super("Flow recommendation " + recommendationId + " is currently version " + actualVersion + ".");
        this.recommendationId = recommendationId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }

    public FlowRecommendationId recommendationId() {
        return recommendationId;
    }

    public long expectedVersion() {
        return expectedVersion;
    }

    public long actualVersion() {
        return actualVersion;
    }
}
