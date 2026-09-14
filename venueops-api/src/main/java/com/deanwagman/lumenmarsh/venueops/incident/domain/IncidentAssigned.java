package com.deanwagman.lumenmarsh.venueops.incident.domain;

import java.time.Instant;

public record IncidentAssigned(
        String id,
        IncidentId incidentId,
        IncidentEventType type,
        String actor,
        String reason,
        Instant occurredAt,
        long previousVersion,
        long resultingVersion,
        String assignee
) implements IncidentActivity {
}
