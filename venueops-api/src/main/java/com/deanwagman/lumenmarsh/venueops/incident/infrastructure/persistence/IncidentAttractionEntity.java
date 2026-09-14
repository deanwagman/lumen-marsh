package com.deanwagman.lumenmarsh.venueops.incident.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "incident_attraction")
@IdClass(IncidentAttractionId.class)
public class IncidentAttractionEntity {

    @Id
    @Column(name = "incident_id", length = 36)
    private String incidentId;

    @Id
    @Column(name = "attraction_id", length = 64)
    private String attractionId;

    protected IncidentAttractionEntity() {
    }

    public IncidentAttractionEntity(String incidentId, String attractionId) {
        this.incidentId = incidentId;
        this.attractionId = attractionId;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public String getAttractionId() {
        return attractionId;
    }
}
