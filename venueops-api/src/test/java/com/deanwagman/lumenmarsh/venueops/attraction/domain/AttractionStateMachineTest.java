package com.deanwagman.lumenmarsh.venueops.attraction.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttractionStateMachineTest {

    @ParameterizedTest(name = "{0} + {1} → {2}")
    @MethodSource("validTransitions")
    void allowsEveryDocumentedTransition(AttractionStatus current, AttractionCommand command, AttractionStatus expected) {
        assertThat(AttractionStateMachine.isAllowed(current, command)).isTrue();
        assertThat(AttractionStateMachine.nextStatus(current, command)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} + {1} is rejected")
    @MethodSource("invalidTransitions")
    void rejectsUndocumentedTransitions(AttractionStatus current, AttractionCommand command) {
        assertThat(AttractionStateMachine.isAllowed(current, command)).isFalse();
        assertThatThrownBy(() -> AttractionStateMachine.nextStatus(current, command))
                .isInstanceOf(InvalidAttractionTransitionException.class)
                .satisfies(ex -> {
                    InvalidAttractionTransitionException invalid = (InvalidAttractionTransitionException) ex;
                    assertThat(invalid.currentStatus()).isEqualTo(current);
                    assertThat(invalid.command()).isEqualTo(command);
                });
    }

    @Test
    void closeForDayIsRejectedFromClosed() {
        assertThat(AttractionStateMachine.isAllowed(AttractionStatus.CLOSED, AttractionCommand.CLOSE_FOR_DAY)).isFalse();
    }

    static Stream<Arguments> validTransitions() {
        return Stream.of(
                Arguments.of(AttractionStatus.CLOSED, AttractionCommand.START_TESTING, AttractionStatus.TESTING),
                Arguments.of(AttractionStatus.TESTING, AttractionCommand.COMPLETE_TESTING, AttractionStatus.RETURNING_TO_SERVICE),
                Arguments.of(AttractionStatus.RETURNING_TO_SERVICE, AttractionCommand.APPROVE_RETURN_TO_SERVICE, AttractionStatus.OPERATING),
                Arguments.of(AttractionStatus.OPERATING, AttractionCommand.PLACE_WEATHER_HOLD, AttractionStatus.WEATHER_HOLD),
                Arguments.of(AttractionStatus.WEATHER_HOLD, AttractionCommand.CLEAR_WEATHER_HOLD, AttractionStatus.TESTING),
                Arguments.of(AttractionStatus.OPERATING, AttractionCommand.REPORT_TECHNICAL_FAULT, AttractionStatus.TECHNICAL_DELAY),
                Arguments.of(AttractionStatus.TECHNICAL_DELAY, AttractionCommand.COMPLETE_REPAIR, AttractionStatus.TESTING),
                Arguments.of(AttractionStatus.TESTING, AttractionCommand.CLOSE_FOR_DAY, AttractionStatus.CLOSED),
                Arguments.of(AttractionStatus.RETURNING_TO_SERVICE, AttractionCommand.CLOSE_FOR_DAY, AttractionStatus.CLOSED),
                Arguments.of(AttractionStatus.OPERATING, AttractionCommand.CLOSE_FOR_DAY, AttractionStatus.CLOSED),
                Arguments.of(AttractionStatus.WEATHER_HOLD, AttractionCommand.CLOSE_FOR_DAY, AttractionStatus.CLOSED),
                Arguments.of(AttractionStatus.TECHNICAL_DELAY, AttractionCommand.CLOSE_FOR_DAY, AttractionStatus.CLOSED)
        );
    }

    static Stream<Arguments> invalidTransitions() {
        return Stream.of(
                Arguments.of(AttractionStatus.CLOSED, AttractionCommand.PLACE_WEATHER_HOLD),
                Arguments.of(AttractionStatus.CLOSED, AttractionCommand.APPROVE_RETURN_TO_SERVICE),
                Arguments.of(AttractionStatus.CLOSED, AttractionCommand.CLOSE_FOR_DAY),
                Arguments.of(AttractionStatus.WEATHER_HOLD, AttractionCommand.APPROVE_RETURN_TO_SERVICE),
                Arguments.of(AttractionStatus.WEATHER_HOLD, AttractionCommand.START_TESTING),
                Arguments.of(AttractionStatus.OPERATING, AttractionCommand.START_TESTING),
                Arguments.of(AttractionStatus.OPERATING, AttractionCommand.COMPLETE_TESTING),
                Arguments.of(AttractionStatus.TESTING, AttractionCommand.PLACE_WEATHER_HOLD),
                Arguments.of(AttractionStatus.RETURNING_TO_SERVICE, AttractionCommand.START_TESTING),
                Arguments.of(AttractionStatus.TECHNICAL_DELAY, AttractionCommand.PLACE_WEATHER_HOLD)
        );
    }
}
