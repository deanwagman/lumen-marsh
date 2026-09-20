package com.deanwagman.lumenmarsh.venueops.flow.api;

import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationCommand;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record FlowRecommendationCommandRequest(
        @NotNull UUID commandId,
        @NotNull FlowRecommendationCommand type,
        @NotNull Long expectedVersion,
        String reason,
        Map<String, Object> data
) {
}
