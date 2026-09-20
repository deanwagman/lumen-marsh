package com.deanwagman.lumenmarsh.venueops.flow.domain;

public class InvalidFlowRecommendationTransitionException extends RuntimeException {

    private final FlowRecommendationStatus currentStatus;
    private final FlowRecommendationCommand command;

    public InvalidFlowRecommendationTransitionException(
            FlowRecommendationStatus currentStatus,
            FlowRecommendationCommand command
    ) {
        super("Cannot apply " + command + " when the flow recommendation is " + currentStatus + ".");
        this.currentStatus = currentStatus;
        this.command = command;
    }

    public FlowRecommendationStatus currentStatus() {
        return currentStatus;
    }

    public FlowRecommendationCommand command() {
        return command;
    }
}
