package com.deanwagman.lumenmarsh.venueops.incident.application;

import com.deanwagman.lumenmarsh.venueops.incident.domain.Incident;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentSeverity;

import java.time.Instant;
import java.util.List;

public record GuestAdvisoryOperationalSnapshot(
        String id,
        IncidentSeverity severity,
        String title,
        String message,
        List<String> affectedAttractionIds,
        Instant updatedAt,
        long version
) {
    public static GuestAdvisoryOperationalSnapshot from(Incident incident) {
        return new GuestAdvisoryOperationalSnapshot(
                incident.id().value(),
                incident.severity(),
                incident.guestTitle(),
                incident.guestMessage(),
                incident.attractionIds().stream().map(id -> id.value()).toList(),
                incident.updatedAt(),
                incident.version()
        );
    }
}
