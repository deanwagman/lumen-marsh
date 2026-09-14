package com.deanwagman.lumenmarsh.venueops.incident.api;

import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentSeverity;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReportIncidentRequest(
        @NotBlank String title,
        @NotNull IncidentType type,
        @NotNull IncidentSeverity severity,
        String internalDescription,
        List<String> attractionIds
) {
}
