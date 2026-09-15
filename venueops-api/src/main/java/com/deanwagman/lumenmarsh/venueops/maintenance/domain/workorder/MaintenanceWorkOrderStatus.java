package com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder;

public enum MaintenanceWorkOrderStatus {
    DRAFT,
    OPEN,
    ASSIGNED,
    IN_PROGRESS,
    AWAITING_INSPECTION,
    READY_FOR_TESTING,
    COMPLETED,
    CANCELED;

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELED;
    }

    public boolean isActive() {
        return !isTerminal();
    }
}
