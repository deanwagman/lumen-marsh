package com.deanwagman.lumenmarsh.venueops.flow.application;

import com.deanwagman.lumenmarsh.venueops.flow.domain.QueueObservation;

import java.util.UUID;

public record FlowObservationIngestResult(
        UUID observationId,
        boolean accepted,
        boolean replay,
        boolean appliedToProjection,
        long projectionVersion,
        QueueObservation observation
) {
    public static FlowObservationIngestResult replay(QueueObservation observation, long projectionVersion) {
        return new FlowObservationIngestResult(
                observation.observationId(),
                true,
                true,
                false,
                projectionVersion,
                observation
        );
    }

    public static FlowObservationIngestResult accepted(
            QueueObservation observation,
            boolean appliedToProjection,
            long projectionVersion
    ) {
        return new FlowObservationIngestResult(
                observation.observationId(),
                true,
                false,
                appliedToProjection,
                projectionVersion,
                observation
        );
    }
}
