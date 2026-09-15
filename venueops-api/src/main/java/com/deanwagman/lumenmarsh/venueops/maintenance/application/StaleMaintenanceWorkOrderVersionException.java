package com.deanwagman.lumenmarsh.venueops.maintenance.application;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrderId;

public class StaleMaintenanceWorkOrderVersionException extends RuntimeException {

    private final MaintenanceWorkOrderId workOrderId;
    private final String workOrderNumber;
    private final long expectedVersion;
    private final long actualVersion;

    public StaleMaintenanceWorkOrderVersionException(
            MaintenanceWorkOrderId workOrderId,
            String workOrderNumber,
            long expectedVersion,
            long actualVersion
    ) {
        super("Work order " + (workOrderNumber == null ? workOrderId : workOrderNumber)
                + " is currently version " + actualVersion + ".");
        this.workOrderId = workOrderId;
        this.workOrderNumber = workOrderNumber;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }

    public MaintenanceWorkOrderId workOrderId() {
        return workOrderId;
    }

    public String workOrderNumber() {
        return workOrderNumber;
    }

    public long expectedVersion() {
        return expectedVersion;
    }

    public long actualVersion() {
        return actualVersion;
    }
}
