package com.deanwagman.lumenmarsh.venueops.incident.domain;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;

import java.time.Instant;
import java.util.List;

public record IncidentReported(
        String id,
        IncidentId incidentId,
        IncidentEventType type,
        String actor,
        String reason,
        Instant occurredAt,
        long previousVersion,
        long resultingVersion,
        String title,
        IncidentType incidentType,
        IncidentSeverity severity,
        List<AttractionId> attractionIds
) implements IncidentActivity {
}
