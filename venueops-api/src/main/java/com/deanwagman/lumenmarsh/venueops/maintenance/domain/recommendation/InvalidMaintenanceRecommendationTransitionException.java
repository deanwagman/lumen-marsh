package com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation;

public class InvalidMaintenanceRecommendationTransitionException extends RuntimeException {

    private final MaintenanceRecommendationStatus currentStatus;
    private final MaintenanceRecommendationCommand command;

    public InvalidMaintenanceRecommendationTransitionException(
            MaintenanceRecommendationStatus currentStatus,
            MaintenanceRecommendationCommand command
    ) {
        super("Cannot apply " + command + " when recommendation is " + currentStatus);
        this.currentStatus = currentStatus;
        this.command = command;
    }

    public MaintenanceRecommendationStatus currentStatus() {
        return currentStatus;
    }

    public MaintenanceRecommendationCommand command() {
        return command;
    }
}
