package com.deanwagman.lumenmarsh.venueops.incident.api;

import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentCommand;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentSeverity;
import jakarta.validation.constraints.NotNull;

public record IncidentCommandRequest(
        @NotNull IncidentCommand type,
        String reason,
        @NotNull Long expectedVersion,
        String assignee,
        IncidentSeverity severity,
        String attractionId,
        String guestTitle,
        String guestMessage,
        Boolean confirmActiveWorkOrders
) {
}
