package com.deanwagman.lumenmarsh.venueops.incident.application;

import com.deanwagman.lumenmarsh.venueops.incident.domain.Incident;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentActivity;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentEventType;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentReported;

import java.time.Instant;

public record IncidentOperationalUpdate(
        String eventId,
        IncidentUpdateEventType eventType,
        Instant occurredAt,
        IncidentOperationalSnapshot incident
) {
    public static final String SNAPSHOT_EVENT = "incidents.snapshot";

    public static IncidentOperationalUpdate from(IncidentActivity activity, Incident incident) {
        return new IncidentOperationalUpdate(
                activity.id(),
                sseEventType(activity),
                activity.occurredAt(),
                IncidentOperationalSnapshot.from(incident)
        );
    }

    public String sseEventName() {
        return switch (eventType) {
            case INCIDENT_REPORTED -> "incident.reported";
            case INCIDENT_UPDATED -> "incident.updated";
            case INCIDENT_RESOLVED -> "incident.resolved";
        };
    }

    private static IncidentUpdateEventType sseEventType(IncidentActivity activity) {
        if (activity instanceof IncidentReported || activity.type() == IncidentEventType.INCIDENT_REPORTED) {
            return IncidentUpdateEventType.INCIDENT_REPORTED;
        }
        if (activity.type() == IncidentEventType.INCIDENT_RESOLVED) {
            return IncidentUpdateEventType.INCIDENT_RESOLVED;
        }
        return IncidentUpdateEventType.INCIDENT_UPDATED;
    }
}
