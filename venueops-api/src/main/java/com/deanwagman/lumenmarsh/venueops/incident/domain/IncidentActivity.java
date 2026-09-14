package com.deanwagman.lumenmarsh.venueops.incident.domain;

import java.time.Instant;

public sealed interface IncidentActivity
        permits IncidentReported,
        IncidentStatusChanged,
        IncidentAssigned,
        IncidentSeverityChanged,
        IncidentAttractionLinked,
        IncidentAttractionUnlinked,
        IncidentGuestAdvisoryPublished,
        IncidentGuestAdvisoryWithdrawn {

    String id();

    IncidentId incidentId();

    IncidentEventType type();

    String actor();

    String reason();

    Instant occurredAt();

    long previousVersion();

    long resultingVersion();
}
