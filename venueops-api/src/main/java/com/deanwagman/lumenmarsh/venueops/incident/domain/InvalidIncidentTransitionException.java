package com.deanwagman.lumenmarsh.venueops.incident.domain;

public class InvalidIncidentTransitionException extends RuntimeException {

    private final IncidentStatus currentStatus;
    private final IncidentCommand command;

    public InvalidIncidentTransitionException(IncidentStatus currentStatus, IncidentCommand command) {
        super("Cannot apply " + command + " when incident is " + currentStatus);
        this.currentStatus = currentStatus;
        this.command = command;
    }

    public IncidentStatus currentStatus() {
        return currentStatus;
    }

    public IncidentCommand command() {
        return command;
    }
}
