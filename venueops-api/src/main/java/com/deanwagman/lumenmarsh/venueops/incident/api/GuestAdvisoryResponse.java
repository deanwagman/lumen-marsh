package com.deanwagman.lumenmarsh.venueops.incident.api;

import com.deanwagman.lumenmarsh.venueops.incident.domain.Incident;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentSeverity;

import java.time.Instant;
import java.util.List;

public record GuestAdvisoryResponse(
        String id,
        IncidentSeverity severity,
        String title,
        String message,
        List<String> affectedAttractionIds,
        Instant updatedAt,
        long version
) {
    public static GuestAdvisoryResponse from(Incident incident) {
        return new GuestAdvisoryResponse(
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
