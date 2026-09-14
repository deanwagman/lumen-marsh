package com.deanwagman.lumenmarsh.venueops.incident.application;

import com.deanwagman.lumenmarsh.venueops.incident.domain.Incident;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentSeverity;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentStatus;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentType;

import java.time.Instant;
import java.util.List;

public record IncidentOperationalSnapshot(
        String id,
        String title,
        IncidentType type,
        IncidentSeverity severity,
        IncidentStatus status,
        String assignedTo,
        boolean guestAdvisoryPublished,
        String guestTitle,
        String guestMessage,
        List<String> affectedAttractionIds,
        Instant updatedAt,
        long version
) {
    public static IncidentOperationalSnapshot from(Incident incident) {
        return new IncidentOperationalSnapshot(
                incident.id().value(),
                incident.title(),
                incident.type(),
                incident.severity(),
                incident.status(),
                incident.assignedTo(),
                incident.guestAdvisoryPublished(),
                incident.guestTitle(),
                incident.guestMessage(),
                incident.attractionIds().stream().map(id -> id.value()).toList(),
                incident.updatedAt(),
                incident.version()
        );
    }
}
