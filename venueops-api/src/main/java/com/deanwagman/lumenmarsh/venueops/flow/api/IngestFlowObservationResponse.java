package com.deanwagman.lumenmarsh.venueops.flow.api;

import java.util.UUID;

public record IngestFlowObservationResponse(
        UUID observationId,
        boolean accepted,
        boolean replay,
        long projectionVersion
) {
}
