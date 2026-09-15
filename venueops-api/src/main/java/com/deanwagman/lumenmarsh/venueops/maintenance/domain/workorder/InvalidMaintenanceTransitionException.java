package com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder;

public class InvalidMaintenanceTransitionException extends RuntimeException {

    private final MaintenanceWorkOrderStatus currentStatus;
    private final MaintenanceWorkOrderCommand command;

    public InvalidMaintenanceTransitionException(
            MaintenanceWorkOrderStatus currentStatus,
            MaintenanceWorkOrderCommand command
    ) {
        super("Cannot apply " + command + " when work order is " + currentStatus);
        this.currentStatus = currentStatus;
        this.command = command;
    }

    public MaintenanceWorkOrderStatus currentStatus() {
        return currentStatus;
    }

    public MaintenanceWorkOrderCommand command() {
        return command;
    }
}
