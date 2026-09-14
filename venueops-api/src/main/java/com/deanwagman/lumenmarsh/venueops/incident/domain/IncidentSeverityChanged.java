package com.deanwagman.lumenmarsh.venueops.incident.domain;

import java.time.Instant;

public record IncidentSeverityChanged(
        String id,
        IncidentId incidentId,
        IncidentEventType type,
        String actor,
        String reason,
        Instant occurredAt,
        long previousVersion,
        long resultingVersion,
        IncidentSeverity previousSeverity,
        IncidentSeverity newSeverity
) implements IncidentActivity {
}
