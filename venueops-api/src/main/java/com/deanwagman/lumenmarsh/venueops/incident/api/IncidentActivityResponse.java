package com.deanwagman.lumenmarsh.venueops.incident.api;

import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentActivity;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentAssigned;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentAttractionLinked;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentAttractionUnlinked;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentEventType;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentGuestAdvisoryPublished;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentGuestAdvisoryWithdrawn;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentReported;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentSeverity;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentSeverityChanged;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentStatus;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentStatusChanged;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentType;

import java.time.Instant;
import java.util.List;

public record IncidentActivityResponse(
        String id,
        String incidentId,
        IncidentEventType type,
        String actor,
        String reason,
        Instant occurredAt,
        long previousVersion,
        long resultingVersion,
        String title,
        IncidentType incidentType,
        IncidentSeverity severity,
        List<String> attractionIds,
        IncidentStatus previousStatus,
        IncidentStatus newStatus,
        String assignee,
        IncidentSeverity previousSeverity,
        IncidentSeverity newSeverity,
        String attractionId,
        String guestTitle,
        String guestMessage
) {
    public static IncidentActivityResponse from(IncidentActivity activity) {
        return switch (activity) {
            case IncidentReported reported -> base(reported).title(reported.title())
                    .incidentType(reported.incidentType())
                    .severity(reported.severity())
                    .attractionIds(reported.attractionIds().stream().map(id -> id.value()).toList())
                    .build();
            case IncidentStatusChanged changed -> base(changed)
                    .previousStatus(changed.previousStatus())
                    .newStatus(changed.newStatus())
                    .build();
            case IncidentAssigned assigned -> base(assigned).assignee(assigned.assignee()).build();
            case IncidentSeverityChanged changed -> base(changed)
                    .previousSeverity(changed.previousSeverity())
                    .newSeverity(changed.newSeverity())
                    .build();
            case IncidentAttractionLinked linked -> base(linked).attractionId(linked.attractionId().value()).build();
            case IncidentAttractionUnlinked unlinked -> base(unlinked).attractionId(unlinked.attractionId().value()).build();
            case IncidentGuestAdvisoryPublished published -> base(published)
                    .guestTitle(published.guestTitle())
                    .guestMessage(published.guestMessage())
                    .build();
            case IncidentGuestAdvisoryWithdrawn withdrawn -> base(withdrawn).build();
        };
    }

    private static Builder base(IncidentActivity activity) {
        return new Builder(
                activity.id(),
                activity.incidentId().value(),
                activity.type(),
                activity.actor(),
                activity.reason(),
                activity.occurredAt(),
                activity.previousVersion(),
                activity.resultingVersion()
        );
    }

    private static final class Builder {
        private final String id;
        private final String incidentId;
        private final IncidentEventType type;
        private final String actor;
        private final String reason;
        private final Instant occurredAt;
        private final long previousVersion;
        private final long resultingVersion;
        private String title;
        private IncidentType incidentType;
        private IncidentSeverity severity;
        private List<String> attractionIds;
        private IncidentStatus previousStatus;
        private IncidentStatus newStatus;
        private String assignee;
        private IncidentSeverity previousSeverity;
        private IncidentSeverity newSeverity;
        private String attractionId;
        private String guestTitle;
        private String guestMessage;

        private Builder(
                String id,
                String incidentId,
                IncidentEventType type,
                String actor,
                String reason,
                Instant occurredAt,
                long previousVersion,
                long resultingVersion
        ) {
            this.id = id;
            this.incidentId = incidentId;
            this.type = type;
            this.actor = actor;
            this.reason = reason;
            this.occurredAt = occurredAt;
            this.previousVersion = previousVersion;
            this.resultingVersion = resultingVersion;
        }

        private Builder title(String title) {
            this.title = title;
            return this;
        }

        private Builder incidentType(IncidentType incidentType) {
            this.incidentType = incidentType;
            return this;
        }

        private Builder severity(IncidentSeverity severity) {
            this.severity = severity;
            return this;
        }

        private Builder attractionIds(List<String> attractionIds) {
            this.attractionIds = attractionIds;
            return this;
        }

        private Builder previousStatus(IncidentStatus previousStatus) {
            this.previousStatus = previousStatus;
            return this;
        }

        private Builder newStatus(IncidentStatus newStatus) {
            this.newStatus = newStatus;
            return this;
        }

        private Builder assignee(String assignee) {
            this.assignee = assignee;
            return this;
        }

        private Builder previousSeverity(IncidentSeverity previousSeverity) {
            this.previousSeverity = previousSeverity;
            return this;
        }

        private Builder newSeverity(IncidentSeverity newSeverity) {
            this.newSeverity = newSeverity;
            return this;
        }

        private Builder attractionId(String attractionId) {
            this.attractionId = attractionId;
            return this;
        }

        private Builder guestTitle(String guestTitle) {
            this.guestTitle = guestTitle;
            return this;
        }

        private Builder guestMessage(String guestMessage) {
            this.guestMessage = guestMessage;
            return this;
        }

        private IncidentActivityResponse build() {
            return new IncidentActivityResponse(
                    id,
                    incidentId,
                    type,
                    actor,
                    reason,
                    occurredAt,
                    previousVersion,
                    resultingVersion,
                    title,
                    incidentType,
                    severity,
                    attractionIds,
                    previousStatus,
                    newStatus,
                    assignee,
                    previousSeverity,
                    newSeverity,
                    attractionId,
                    guestTitle,
                    guestMessage
            );
        }
    }
}
