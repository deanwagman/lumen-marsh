package com.deanwagman.lumenmarsh.venueops.flow.api;

import java.util.List;
import java.util.UUID;

public record IngestFlowForecastResponse(
        boolean accepted,
        boolean replay,
        List<UUID> forecastIds,
        boolean recommendationAccepted,
        String recommendationId,
        String recommendationSkipReason
) {
}
