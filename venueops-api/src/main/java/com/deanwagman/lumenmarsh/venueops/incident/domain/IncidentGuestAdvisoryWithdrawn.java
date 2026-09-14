package com.deanwagman.lumenmarsh.venueops.incident.domain;

import java.time.Instant;

public record IncidentGuestAdvisoryWithdrawn(
        String id,
        IncidentId incidentId,
        IncidentEventType type,
        String actor,
        String reason,
        Instant occurredAt,
        long previousVersion,
        long resultingVersion
) implements IncidentActivity {
}
