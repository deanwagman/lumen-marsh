package com.deanwagman.lumenmarsh.venueops.maintenance.domain.checklist;

import java.util.Objects;
import java.util.UUID;

public record MaintenanceChecklistItemId(UUID value) {

    public MaintenanceChecklistItemId {
        Objects.requireNonNull(value, "checklist item id is required");
    }

    public static MaintenanceChecklistItemId of(String value) {
        Objects.requireNonNull(value, "checklist item id is required");
        return new MaintenanceChecklistItemId(UUID.fromString(value));
    }

    public static MaintenanceChecklistItemId random() {
        return new MaintenanceChecklistItemId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
