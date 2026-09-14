package com.deanwagman.lumenmarsh.venueops.incident.application;

import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentId;

public class IncidentNotFoundException extends RuntimeException {

    private final IncidentId incidentId;

    public IncidentNotFoundException(IncidentId incidentId) {
        super("Incident not found: " + incidentId);
        this.incidentId = incidentId;
    }

    public IncidentId incidentId() {
        return incidentId;
    }
}
