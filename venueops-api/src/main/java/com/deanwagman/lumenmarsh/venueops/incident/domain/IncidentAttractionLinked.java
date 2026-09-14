package com.deanwagman.lumenmarsh.venueops.incident.domain;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;

import java.time.Instant;

public record IncidentAttractionLinked(
        String id,
        IncidentId incidentId,
        IncidentEventType type,
        String actor,
        String reason,
        Instant occurredAt,
        long previousVersion,
        long resultingVersion,
        AttractionId attractionId
) implements IncidentActivity {
}
