package com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder;

import java.util.Objects;
import java.util.UUID;

public record MaintenanceWorkOrderId(UUID value) {

    public MaintenanceWorkOrderId {
        Objects.requireNonNull(value, "work order id is required");
    }

    public static MaintenanceWorkOrderId of(String value) {
        Objects.requireNonNull(value, "work order id is required");
        return new MaintenanceWorkOrderId(UUID.fromString(value));
    }

    public static MaintenanceWorkOrderId random() {
        return new MaintenanceWorkOrderId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
