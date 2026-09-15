package com.deanwagman.lumenmarsh.venueops.maintenance.application;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrderId;

public class MaintenanceWorkOrderNotFoundException extends RuntimeException {

    private final MaintenanceWorkOrderId workOrderId;

    public MaintenanceWorkOrderNotFoundException(MaintenanceWorkOrderId workOrderId) {
        super("Work order not found: " + workOrderId);
        this.workOrderId = workOrderId;
    }

    public MaintenanceWorkOrderId workOrderId() {
        return workOrderId;
    }
}
