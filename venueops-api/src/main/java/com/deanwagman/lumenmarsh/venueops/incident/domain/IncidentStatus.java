package com.deanwagman.lumenmarsh.venueops.incident.domain;

public enum IncidentStatus {
    REPORTED,
    ACKNOWLEDGED,
    MITIGATING,
    RESOLVED;

    public boolean isResolved() {
        return this == RESOLVED;
    }
}
