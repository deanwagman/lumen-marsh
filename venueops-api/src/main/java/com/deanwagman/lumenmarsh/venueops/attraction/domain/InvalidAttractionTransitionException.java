package com.deanwagman.lumenmarsh.venueops.attraction.domain;

public class InvalidAttractionTransitionException extends RuntimeException {

    private final AttractionStatus currentStatus;
    private final AttractionCommand command;

    public InvalidAttractionTransitionException(AttractionStatus currentStatus, AttractionCommand command) {
        super("Cannot apply " + command + " when attraction is " + currentStatus);
        this.currentStatus = currentStatus;
        this.command = command;
    }

    public AttractionStatus currentStatus() {
        return currentStatus;
    }

    public AttractionCommand command() {
        return command;
    }
}
