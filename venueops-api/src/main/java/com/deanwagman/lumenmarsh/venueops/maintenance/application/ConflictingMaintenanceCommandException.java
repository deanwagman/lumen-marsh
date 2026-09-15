package com.deanwagman.lumenmarsh.venueops.maintenance.application;

import java.util.UUID;

public class ConflictingMaintenanceCommandException extends RuntimeException {

    private final UUID commandId;
    private final String existingAggregateId;
    private final String requestedAggregateId;

    public ConflictingMaintenanceCommandException(
            UUID commandId,
            String existingAggregateId,
            String requestedAggregateId
    ) {
        super("Command " + commandId + " already belongs to " + existingAggregateId + ".");
        this.commandId = commandId;
        this.existingAggregateId = existingAggregateId;
        this.requestedAggregateId = requestedAggregateId;
    }

    public UUID commandId() {
        return commandId;
    }

    public String existingAggregateId() {
        return existingAggregateId;
    }

    public String requestedAggregateId() {
        return requestedAggregateId;
    }
}
