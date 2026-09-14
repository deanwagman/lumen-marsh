package com.deanwagman.lumenmarsh.venueops.incident.domain;

import java.time.Instant;

public record IncidentStatusChanged(
        String id,
        IncidentId incidentId,
        IncidentEventType type,
        String actor,
        String reason,
        Instant occurredAt,
        long previousVersion,
        long resultingVersion,
        IncidentStatus previousStatus,
        IncidentStatus newStatus
) implements IncidentActivity {
}
