package com.deanwagman.lumenmarsh.venueops.attraction.domain;

public enum AttractionCommand {
    START_TESTING(AttractionEventType.ATTRACTION_TESTING_STARTED, false),
    COMPLETE_TESTING(AttractionEventType.ATTRACTION_TESTING_COMPLETED, false),
    APPROVE_RETURN_TO_SERVICE(AttractionEventType.ATTRACTION_OPENED, false),
    PLACE_WEATHER_HOLD(AttractionEventType.WEATHER_HOLD_PLACED, true),
    CLEAR_WEATHER_HOLD(AttractionEventType.WEATHER_HOLD_CLEARED, false),
    REPORT_TECHNICAL_FAULT(AttractionEventType.TECHNICAL_FAULT_REPORTED, true),
    COMPLETE_REPAIR(AttractionEventType.REPAIR_COMPLETED, false),
    CLOSE_FOR_DAY(AttractionEventType.ATTRACTION_CLOSED, true),
    REDUCE_CAPACITY(AttractionEventType.CAPACITY_REDUCED, true),
    RESTORE_CAPACITY(AttractionEventType.CAPACITY_RESTORED, false),
    UPDATE_WAIT_TIME(AttractionEventType.WAIT_TIME_UPDATED, false);

    private final AttractionEventType eventType;
    private final boolean requiresReason;

    AttractionCommand(AttractionEventType eventType, boolean requiresReason) {
        this.eventType = eventType;
        this.requiresReason = requiresReason;
    }

    public AttractionEventType eventType() {
        return eventType;
    }

    public boolean requiresReason() {
        return requiresReason;
    }

    public boolean changesStatus() {
        return this != REDUCE_CAPACITY && this != RESTORE_CAPACITY && this != UPDATE_WAIT_TIME;
    }
}
