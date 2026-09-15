package com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record MaintenanceEvidence(
        UUID id,
        String label,
        String contentType,
        String uri,
        String addedBySubject,
        Instant addedAt
) {
    public MaintenanceEvidence {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(label, "label is required");
        if (label.isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        Objects.requireNonNull(contentType, "contentType is required");
        Objects.requireNonNull(uri, "uri is required");
        Objects.requireNonNull(addedBySubject, "addedBySubject is required");
        Objects.requireNonNull(addedAt, "addedAt is required");
    }
}
