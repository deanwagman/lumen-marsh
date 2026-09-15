package com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder;

import java.util.Map;
import java.util.Optional;

public final class MaintenanceWorkOrderStateMachine {

    private static final Map<Transition, MaintenanceWorkOrderStatus> TRANSITIONS = Map.ofEntries(
            Map.entry(new Transition(MaintenanceWorkOrderStatus.DRAFT, MaintenanceWorkOrderCommand.OPEN),
                    MaintenanceWorkOrderStatus.OPEN),
            Map.entry(new Transition(MaintenanceWorkOrderStatus.OPEN, MaintenanceWorkOrderCommand.ASSIGN),
                    MaintenanceWorkOrderStatus.ASSIGNED),
            Map.entry(new Transition(MaintenanceWorkOrderStatus.ASSIGNED, MaintenanceWorkOrderCommand.START_WORK),
                    MaintenanceWorkOrderStatus.IN_PROGRESS),
            Map.entry(new Transition(MaintenanceWorkOrderStatus.IN_PROGRESS, MaintenanceWorkOrderCommand.REQUEST_INSPECTION),
                    MaintenanceWorkOrderStatus.AWAITING_INSPECTION),
            Map.entry(new Transition(MaintenanceWorkOrderStatus.AWAITING_INSPECTION, MaintenanceWorkOrderCommand.REJECT_INSPECTION),
                    MaintenanceWorkOrderStatus.IN_PROGRESS),
            Map.entry(new Transition(MaintenanceWorkOrderStatus.AWAITING_INSPECTION, MaintenanceWorkOrderCommand.APPROVE_INSPECTION),
                    MaintenanceWorkOrderStatus.READY_FOR_TESTING),
            Map.entry(new Transition(MaintenanceWorkOrderStatus.READY_FOR_TESTING, MaintenanceWorkOrderCommand.COMPLETE),
                    MaintenanceWorkOrderStatus.COMPLETED),
            Map.entry(new Transition(MaintenanceWorkOrderStatus.DRAFT, MaintenanceWorkOrderCommand.CANCEL),
                    MaintenanceWorkOrderStatus.CANCELED),
            Map.entry(new Transition(MaintenanceWorkOrderStatus.OPEN, MaintenanceWorkOrderCommand.CANCEL),
                    MaintenanceWorkOrderStatus.CANCELED),
            Map.entry(new Transition(MaintenanceWorkOrderStatus.ASSIGNED, MaintenanceWorkOrderCommand.CANCEL),
                    MaintenanceWorkOrderStatus.CANCELED),
            Map.entry(new Transition(MaintenanceWorkOrderStatus.IN_PROGRESS, MaintenanceWorkOrderCommand.CANCEL),
                    MaintenanceWorkOrderStatus.CANCELED),
            Map.entry(new Transition(MaintenanceWorkOrderStatus.AWAITING_INSPECTION, MaintenanceWorkOrderCommand.CANCEL),
                    MaintenanceWorkOrderStatus.CANCELED),
            Map.entry(new Transition(MaintenanceWorkOrderStatus.READY_FOR_TESTING, MaintenanceWorkOrderCommand.CANCEL),
                    MaintenanceWorkOrderStatus.CANCELED)
    );

    private MaintenanceWorkOrderStateMachine() {
    }

    public static MaintenanceWorkOrderStatus nextStatus(
            MaintenanceWorkOrderStatus current,
            MaintenanceWorkOrderCommand command
    ) {
        return lookup(current, command)
                .orElseThrow(() -> new InvalidMaintenanceTransitionException(current, command));
    }

    public static boolean isAllowed(MaintenanceWorkOrderStatus current, MaintenanceWorkOrderCommand command) {
        return lookup(current, command).isPresent();
    }

    private static Optional<MaintenanceWorkOrderStatus> lookup(
            MaintenanceWorkOrderStatus current,
            MaintenanceWorkOrderCommand command
    ) {
        return Optional.ofNullable(TRANSITIONS.get(new Transition(current, command)));
    }

    private record Transition(MaintenanceWorkOrderStatus from, MaintenanceWorkOrderCommand command) {
    }
}
