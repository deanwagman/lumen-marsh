package com.deanwagman.lumenmarsh.venueops.incident.api;

import com.deanwagman.lumenmarsh.venueops.incident.domain.Incident;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentSeverity;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentStatus;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentType;

import java.time.Instant;
import java.util.List;

public record OperatorIncidentResponse(
        String id,
        String title,
        IncidentType type,
        IncidentSeverity severity,
        IncidentStatus status,
        String internalDescription,
        String assignedTo,
        boolean guestAdvisoryPublished,
        String guestTitle,
        String guestMessage,
        List<String> attractionIds,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    public static OperatorIncidentResponse from(Incident incident) {
        return new OperatorIncidentResponse(
                incident.id().value(),
                incident.title(),
                incident.type(),
                incident.severity(),
                incident.status(),
                incident.internalDescription(),
                incident.assignedTo(),
                incident.guestAdvisoryPublished(),
                incident.guestTitle(),
                incident.guestMessage(),
                incident.attractionIds().stream().map(id -> id.value()).toList(),
                incident.createdAt(),
                incident.updatedAt(),
                incident.version()
        );
    }
}
