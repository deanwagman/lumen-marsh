package com.deanwagman.lumenmarsh.venueops.incident.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;

public final class IncidentAttractionId implements Serializable {

    private String incidentId;
    private String attractionId;

    public IncidentAttractionId() {
    }

    public IncidentAttractionId(String incidentId, String attractionId) {
        this.incidentId = incidentId;
        this.attractionId = attractionId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof IncidentAttractionId that)) {
            return false;
        }
        return Objects.equals(incidentId, that.incidentId) && Objects.equals(attractionId, that.attractionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(incidentId, attractionId);
    }
}
