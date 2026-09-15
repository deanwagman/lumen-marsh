package com.deanwagman.lumenmarsh.venueops.maintenance.api;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceRecommendationCommand;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MaintenanceRecommendationCommandRequest(
        @NotNull UUID commandId,
        @NotNull MaintenanceRecommendationCommand type,
        @NotNull Long expectedVersion,
        String reason
) {
}
