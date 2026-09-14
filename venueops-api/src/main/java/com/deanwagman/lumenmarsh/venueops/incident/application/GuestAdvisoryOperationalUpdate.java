package com.deanwagman.lumenmarsh.venueops.incident.application;

import com.deanwagman.lumenmarsh.venueops.incident.domain.Incident;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentActivity;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentEventType;

import java.time.Instant;

public record GuestAdvisoryOperationalUpdate(
        String eventId,
        GuestAdvisoryUpdateEventType eventType,
        Instant occurredAt,
        GuestAdvisoryOperationalSnapshot advisory
) {
    public static GuestAdvisoryOperationalUpdate published(IncidentActivity activity, Incident incident) {
        return new GuestAdvisoryOperationalUpdate(
                activity.id(),
                GuestAdvisoryUpdateEventType.ADVISORY_PUBLISHED,
                activity.occurredAt(),
                GuestAdvisoryOperationalSnapshot.from(incident)
        );
    }

    public static GuestAdvisoryOperationalUpdate updated(IncidentActivity activity, Incident incident) {
        return new GuestAdvisoryOperationalUpdate(
                activity.id(),
                GuestAdvisoryUpdateEventType.ADVISORY_UPDATED,
                activity.occurredAt(),
                GuestAdvisoryOperationalSnapshot.from(incident)
        );
    }

    public static GuestAdvisoryOperationalUpdate withdrawn(IncidentActivity activity, Incident incident) {
        return new GuestAdvisoryOperationalUpdate(
                activity.id(),
                GuestAdvisoryUpdateEventType.ADVISORY_WITHDRAWN,
                activity.occurredAt(),
                GuestAdvisoryOperationalSnapshot.from(incident)
        );
    }

    public String sseEventName() {
        return switch (eventType) {
            case ADVISORY_PUBLISHED -> "advisory.published";
            case ADVISORY_UPDATED -> "advisory.updated";
            case ADVISORY_WITHDRAWN -> "advisory.withdrawn";
        };
    }

    public static GuestAdvisoryOperationalUpdate fromActivity(IncidentActivity activity, Incident incident) {
        return switch (activity.type()) {
            case GUEST_ADVISORY_PUBLISHED -> published(activity, incident);
            case GUEST_ADVISORY_WITHDRAWN, INCIDENT_RESOLVED -> withdrawn(activity, incident);
            default -> incident.hasActiveGuestAdvisory()
                    ? updated(activity, incident)
                    : null;
        };
    }
}
