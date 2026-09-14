package com.deanwagman.lumenmarsh.venueops.incident.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IncidentStateMachineTest {

    @ParameterizedTest(name = "{0} + {1} → {2}")
    @MethodSource("validTransitions")
    void allowsEveryDocumentedTransition(IncidentStatus current, IncidentCommand command, IncidentStatus expected) {
        assertThat(IncidentStateMachine.isAllowed(current, command)).isTrue();
        assertThat(IncidentStateMachine.nextStatus(current, command)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} + {1} is rejected")
    @MethodSource("invalidTransitions")
    void rejectsUndocumentedTransitions(IncidentStatus current, IncidentCommand command) {
        assertThat(IncidentStateMachine.isAllowed(current, command)).isFalse();
        assertThatThrownBy(() -> IncidentStateMachine.nextStatus(current, command))
                .isInstanceOf(InvalidIncidentTransitionException.class)
                .satisfies(ex -> {
                    InvalidIncidentTransitionException invalid = (InvalidIncidentTransitionException) ex;
                    assertThat(invalid.currentStatus()).isEqualTo(current);
                    assertThat(invalid.command()).isEqualTo(command);
                });
    }

    static Stream<Arguments> validTransitions() {
        return Stream.of(
                Arguments.of(IncidentStatus.REPORTED, IncidentCommand.ACKNOWLEDGE, IncidentStatus.ACKNOWLEDGED),
                Arguments.of(IncidentStatus.ACKNOWLEDGED, IncidentCommand.START_MITIGATION, IncidentStatus.MITIGATING),
                Arguments.of(IncidentStatus.MITIGATING, IncidentCommand.RESOLVE, IncidentStatus.RESOLVED)
        );
    }

    static Stream<Arguments> invalidTransitions() {
        return Stream.of(
                Arguments.of(IncidentStatus.REPORTED, IncidentCommand.START_MITIGATION),
                Arguments.of(IncidentStatus.REPORTED, IncidentCommand.RESOLVE),
                Arguments.of(IncidentStatus.ACKNOWLEDGED, IncidentCommand.ACKNOWLEDGE),
                Arguments.of(IncidentStatus.ACKNOWLEDGED, IncidentCommand.RESOLVE),
                Arguments.of(IncidentStatus.MITIGATING, IncidentCommand.ACKNOWLEDGE),
                Arguments.of(IncidentStatus.MITIGATING, IncidentCommand.START_MITIGATION),
                Arguments.of(IncidentStatus.RESOLVED, IncidentCommand.ACKNOWLEDGE),
                Arguments.of(IncidentStatus.RESOLVED, IncidentCommand.START_MITIGATION),
                Arguments.of(IncidentStatus.RESOLVED, IncidentCommand.RESOLVE)
        );
    }
}
