package com.deanwagman.lumenmarsh.venueops.incident.domain;

public enum IncidentCommand {
    ACKNOWLEDGE(IncidentEventType.INCIDENT_ACKNOWLEDGED, false),
    ASSIGN(IncidentEventType.INCIDENT_ASSIGNED, false),
    START_MITIGATION(IncidentEventType.MITIGATION_STARTED, false),
    CHANGE_SEVERITY(IncidentEventType.SEVERITY_CHANGED, true),
    LINK_ATTRACTION(IncidentEventType.ATTRACTION_LINKED, false),
    UNLINK_ATTRACTION(IncidentEventType.ATTRACTION_UNLINKED, true),
    PUBLISH_GUEST_ADVISORY(IncidentEventType.GUEST_ADVISORY_PUBLISHED, false),
    WITHDRAW_GUEST_ADVISORY(IncidentEventType.GUEST_ADVISORY_WITHDRAWN, true),
    RESOLVE(IncidentEventType.INCIDENT_RESOLVED, true);

    private final IncidentEventType eventType;
    private final boolean requiresReason;

    IncidentCommand(IncidentEventType eventType, boolean requiresReason) {
        this.eventType = eventType;
        this.requiresReason = requiresReason;
    }

    public IncidentEventType eventType() {
        return eventType;
    }

    public boolean requiresReason() {
        return requiresReason;
    }

    public boolean changesStatus() {
        return this == ACKNOWLEDGE || this == START_MITIGATION || this == RESOLVE;
    }
}
