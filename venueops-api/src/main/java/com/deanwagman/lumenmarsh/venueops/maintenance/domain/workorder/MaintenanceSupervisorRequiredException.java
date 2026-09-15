package com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder;

public class MaintenanceSupervisorRequiredException extends RuntimeException {

    public MaintenanceSupervisorRequiredException(String message) {
        super(message);
    }
}
