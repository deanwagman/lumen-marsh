package com.deanwagman.lumenmarsh.venueops.flow.api;

import com.deanwagman.lumenmarsh.venueops.flow.domain.ForecastConfidence;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationSeverity;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IngestFlowForecastRequest(
        @NotBlank String attractionId,
        @NotNull Instant generatedAt,
        @NotNull UUID basedOnObservationId,
        @NotNull Boolean simulated,
        @Valid @NotNull List<Horizon> forecasts,
        @Valid RecommendationProposal recommendation
) {
    public record Horizon(
            @NotNull UUID forecastId,
            @NotNull Integer horizonMinutes,
            @NotNull Integer predictedQueueLength,
            @NotNull Integer predictedWaitMinutes,
            @NotNull ForecastConfidence confidence,
            List<String> assumptions,
            @NotBlank String explanation
    ) {
    }

    public record RecommendationProposal(
            @NotNull UUID recommendationId,
            @NotNull FlowRecommendationType type,
            @NotNull FlowRecommendationSeverity severity,
            String sourceAttractionId,
            List<String> affectedAttractionIds,
            List<String> recommendedDestinationIds,
            @NotBlank String summary,
            @NotBlank String explanation,
            String guestMessage,
            Instant expiresAt,
            String relatedIncidentId,
            String relatedWorkOrderId
    ) {
    }
}
