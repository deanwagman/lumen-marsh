package com.deanwagman.lumenmarsh.venueops.flow.application;

import java.util.UUID;

public class FlowObservationNotFoundException extends RuntimeException {

    private final UUID observationId;

    public FlowObservationNotFoundException(UUID observationId) {
        super("Flow observation not found: " + observationId);
        this.observationId = observationId;
    }

    public UUID observationId() {
        return observationId;
    }
}
