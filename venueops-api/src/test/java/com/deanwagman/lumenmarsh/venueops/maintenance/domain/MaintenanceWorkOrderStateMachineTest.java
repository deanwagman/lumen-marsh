package com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MaintenanceWorkOrderStateMachineTest {

    @ParameterizedTest(name = "{0} + {1} → {2}")
    @MethodSource("validTransitions")
    void allowsEveryDocumentedTransition(
            MaintenanceWorkOrderStatus current,
            MaintenanceWorkOrderCommand command,
            MaintenanceWorkOrderStatus expected
    ) {
        assertThat(MaintenanceWorkOrderStateMachine.isAllowed(current, command)).isTrue();
        assertThat(MaintenanceWorkOrderStateMachine.nextStatus(current, command)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} + {1} is rejected")
    @MethodSource("invalidTransitions")
    void rejectsUndocumentedTransitions(MaintenanceWorkOrderStatus current, MaintenanceWorkOrderCommand command) {
        assertThat(MaintenanceWorkOrderStateMachine.isAllowed(current, command)).isFalse();
        assertThatThrownBy(() -> MaintenanceWorkOrderStateMachine.nextStatus(current, command))
                .isInstanceOf(InvalidMaintenanceTransitionException.class)
                .satisfies(ex -> {
                    InvalidMaintenanceTransitionException invalid = (InvalidMaintenanceTransitionException) ex;
                    assertThat(invalid.currentStatus()).isEqualTo(current);
                    assertThat(invalid.command()).isEqualTo(command);
                });
    }

    static Stream<Arguments> validTransitions() {
        return Stream.of(
                Arguments.of(MaintenanceWorkOrderStatus.DRAFT, MaintenanceWorkOrderCommand.OPEN, MaintenanceWorkOrderStatus.OPEN),
                Arguments.of(MaintenanceWorkOrderStatus.OPEN, MaintenanceWorkOrderCommand.ASSIGN, MaintenanceWorkOrderStatus.ASSIGNED),
                Arguments.of(MaintenanceWorkOrderStatus.ASSIGNED, MaintenanceWorkOrderCommand.START_WORK, MaintenanceWorkOrderStatus.IN_PROGRESS),
                Arguments.of(MaintenanceWorkOrderStatus.IN_PROGRESS, MaintenanceWorkOrderCommand.REQUEST_INSPECTION, MaintenanceWorkOrderStatus.AWAITING_INSPECTION),
                Arguments.of(MaintenanceWorkOrderStatus.AWAITING_INSPECTION, MaintenanceWorkOrderCommand.REJECT_INSPECTION, MaintenanceWorkOrderStatus.IN_PROGRESS),
                Arguments.of(MaintenanceWorkOrderStatus.AWAITING_INSPECTION, MaintenanceWorkOrderCommand.APPROVE_INSPECTION, MaintenanceWorkOrderStatus.READY_FOR_TESTING),
                Arguments.of(MaintenanceWorkOrderStatus.READY_FOR_TESTING, MaintenanceWorkOrderCommand.COMPLETE, MaintenanceWorkOrderStatus.COMPLETED),
                Arguments.of(MaintenanceWorkOrderStatus.DRAFT, MaintenanceWorkOrderCommand.CANCEL, MaintenanceWorkOrderStatus.CANCELED),
                Arguments.of(MaintenanceWorkOrderStatus.OPEN, MaintenanceWorkOrderCommand.CANCEL, MaintenanceWorkOrderStatus.CANCELED),
                Arguments.of(MaintenanceWorkOrderStatus.ASSIGNED, MaintenanceWorkOrderCommand.CANCEL, MaintenanceWorkOrderStatus.CANCELED),
                Arguments.of(MaintenanceWorkOrderStatus.IN_PROGRESS, MaintenanceWorkOrderCommand.CANCEL, MaintenanceWorkOrderStatus.CANCELED),
                Arguments.of(MaintenanceWorkOrderStatus.AWAITING_INSPECTION, MaintenanceWorkOrderCommand.CANCEL, MaintenanceWorkOrderStatus.CANCELED),
                Arguments.of(MaintenanceWorkOrderStatus.READY_FOR_TESTING, MaintenanceWorkOrderCommand.CANCEL, MaintenanceWorkOrderStatus.CANCELED)
        );
    }

    static Stream<Arguments> invalidTransitions() {
        return Stream.of(
                Arguments.of(MaintenanceWorkOrderStatus.DRAFT, MaintenanceWorkOrderCommand.ASSIGN),
                Arguments.of(MaintenanceWorkOrderStatus.DRAFT, MaintenanceWorkOrderCommand.START_WORK),
                Arguments.of(MaintenanceWorkOrderStatus.OPEN, MaintenanceWorkOrderCommand.START_WORK),
                Arguments.of(MaintenanceWorkOrderStatus.OPEN, MaintenanceWorkOrderCommand.OPEN),
                Arguments.of(MaintenanceWorkOrderStatus.ASSIGNED, MaintenanceWorkOrderCommand.REQUEST_INSPECTION),
                Arguments.of(MaintenanceWorkOrderStatus.IN_PROGRESS, MaintenanceWorkOrderCommand.ASSIGN),
                Arguments.of(MaintenanceWorkOrderStatus.AWAITING_INSPECTION, MaintenanceWorkOrderCommand.START_WORK),
                Arguments.of(MaintenanceWorkOrderStatus.READY_FOR_TESTING, MaintenanceWorkOrderCommand.APPROVE_INSPECTION),
                Arguments.of(MaintenanceWorkOrderStatus.COMPLETED, MaintenanceWorkOrderCommand.OPEN),
                Arguments.of(MaintenanceWorkOrderStatus.COMPLETED, MaintenanceWorkOrderCommand.COMPLETE),
                Arguments.of(MaintenanceWorkOrderStatus.CANCELED, MaintenanceWorkOrderCommand.ASSIGN),
                Arguments.of(MaintenanceWorkOrderStatus.COMPLETED, MaintenanceWorkOrderCommand.CANCEL)
        );
    }
}
