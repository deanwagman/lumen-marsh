package com.deanwagman.lumenmarsh.venueops.incident.application;

import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentId;

public class StaleIncidentVersionException extends RuntimeException {

    private final IncidentId incidentId;
    private final long expectedVersion;
    private final long actualVersion;

    public StaleIncidentVersionException(IncidentId incidentId, long expectedVersion, long actualVersion) {
        super("Incident " + incidentId + " expected version " + expectedVersion + " but was " + actualVersion);
        this.incidentId = incidentId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }

    public IncidentId incidentId() {
        return incidentId;
    }

    public long expectedVersion() {
        return expectedVersion;
    }

    public long actualVersion() {
        return actualVersion;
    }
}
