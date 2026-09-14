package com.deanwagman.lumenmarsh.venueops.attraction.domain;

import java.util.Map;
import java.util.Optional;

public final class AttractionStateMachine {

    private static final Map<Transition, AttractionStatus> TRANSITIONS = Map.ofEntries(
            Map.entry(new Transition(AttractionStatus.CLOSED, AttractionCommand.START_TESTING), AttractionStatus.TESTING),
            Map.entry(new Transition(AttractionStatus.TESTING, AttractionCommand.COMPLETE_TESTING), AttractionStatus.RETURNING_TO_SERVICE),
            Map.entry(new Transition(AttractionStatus.RETURNING_TO_SERVICE, AttractionCommand.APPROVE_RETURN_TO_SERVICE), AttractionStatus.OPERATING),
            Map.entry(new Transition(AttractionStatus.OPERATING, AttractionCommand.PLACE_WEATHER_HOLD), AttractionStatus.WEATHER_HOLD),
            Map.entry(new Transition(AttractionStatus.WEATHER_HOLD, AttractionCommand.CLEAR_WEATHER_HOLD), AttractionStatus.TESTING),
            Map.entry(new Transition(AttractionStatus.OPERATING, AttractionCommand.REPORT_TECHNICAL_FAULT), AttractionStatus.TECHNICAL_DELAY),
            Map.entry(new Transition(AttractionStatus.TECHNICAL_DELAY, AttractionCommand.COMPLETE_REPAIR), AttractionStatus.TESTING),
            Map.entry(new Transition(AttractionStatus.TESTING, AttractionCommand.CLOSE_FOR_DAY), AttractionStatus.CLOSED),
            Map.entry(new Transition(AttractionStatus.RETURNING_TO_SERVICE, AttractionCommand.CLOSE_FOR_DAY), AttractionStatus.CLOSED),
            Map.entry(new Transition(AttractionStatus.OPERATING, AttractionCommand.CLOSE_FOR_DAY), AttractionStatus.CLOSED),
            Map.entry(new Transition(AttractionStatus.WEATHER_HOLD, AttractionCommand.CLOSE_FOR_DAY), AttractionStatus.CLOSED),
            Map.entry(new Transition(AttractionStatus.TECHNICAL_DELAY, AttractionCommand.CLOSE_FOR_DAY), AttractionStatus.CLOSED)
    );

    private AttractionStateMachine() {
    }

    public static AttractionStatus nextStatus(AttractionStatus current, AttractionCommand command) {
        return lookup(current, command)
                .orElseThrow(() -> new InvalidAttractionTransitionException(current, command));
    }

    public static boolean isAllowed(AttractionStatus current, AttractionCommand command) {
        return lookup(current, command).isPresent();
    }

    private static Optional<AttractionStatus> lookup(AttractionStatus current, AttractionCommand command) {
        return Optional.ofNullable(TRANSITIONS.get(new Transition(current, command)));
    }

    private record Transition(AttractionStatus from, AttractionCommand command) {
    }
}
