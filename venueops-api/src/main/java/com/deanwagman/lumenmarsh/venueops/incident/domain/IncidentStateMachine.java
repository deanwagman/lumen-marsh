package com.deanwagman.lumenmarsh.venueops.incident.domain;

import java.util.Map;
import java.util.Optional;

public final class IncidentStateMachine {

    private static final Map<Transition, IncidentStatus> TRANSITIONS = Map.of(
            new Transition(IncidentStatus.REPORTED, IncidentCommand.ACKNOWLEDGE), IncidentStatus.ACKNOWLEDGED,
            new Transition(IncidentStatus.ACKNOWLEDGED, IncidentCommand.START_MITIGATION), IncidentStatus.MITIGATING,
            new Transition(IncidentStatus.MITIGATING, IncidentCommand.RESOLVE), IncidentStatus.RESOLVED
    );

    private IncidentStateMachine() {
    }

    public static IncidentStatus nextStatus(IncidentStatus current, IncidentCommand command) {
        return lookup(current, command)
                .orElseThrow(() -> new InvalidIncidentTransitionException(current, command));
    }

    public static boolean isAllowed(IncidentStatus current, IncidentCommand command) {
        return lookup(current, command).isPresent();
    }

    private static Optional<IncidentStatus> lookup(IncidentStatus current, IncidentCommand command) {
        return Optional.ofNullable(TRANSITIONS.get(new Transition(current, command)));
    }

    private record Transition(IncidentStatus from, IncidentCommand command) {
    }
}
