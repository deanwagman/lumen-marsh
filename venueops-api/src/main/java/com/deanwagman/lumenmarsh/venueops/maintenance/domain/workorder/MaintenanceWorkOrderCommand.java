package com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.event.MaintenanceEventType;

public enum MaintenanceWorkOrderCommand {
    OPEN(MaintenanceEventType.WORK_ORDER_OPENED, false, true),
    ASSIGN(MaintenanceEventType.WORK_ORDER_ASSIGNED, false, true),
    START_WORK(MaintenanceEventType.WORK_STARTED, false, true),
    REQUEST_INSPECTION(MaintenanceEventType.INSPECTION_REQUESTED, false, true),
    REJECT_INSPECTION(MaintenanceEventType.INSPECTION_REJECTED, true, true),
    APPROVE_INSPECTION(MaintenanceEventType.INSPECTION_APPROVED, true, true),
    COMPLETE(MaintenanceEventType.WORK_ORDER_COMPLETED, true, true),
    CANCEL(MaintenanceEventType.WORK_ORDER_CANCELED, true, true),
    REASSIGN(MaintenanceEventType.WORK_ORDER_REASSIGNED, true, false),
    SET_ESTIMATED_RESTORE(MaintenanceEventType.RESTORE_ESTIMATE_UPDATED, false, false),
    RECORD_CHECKLIST_RESULT(MaintenanceEventType.CHECKLIST_RESULT_RECORDED, false, false),
    LINK_INCIDENT(MaintenanceEventType.WORK_ORDER_INCIDENT_LINKED, false, false),
    ADD_NOTE(MaintenanceEventType.NOTE_ADDED, true, false),
    ADD_EVIDENCE(MaintenanceEventType.EVIDENCE_ADDED, false, false);

    private final MaintenanceEventType eventType;
    private final boolean requiresReason;
    private final boolean changesStatus;

    MaintenanceWorkOrderCommand(MaintenanceEventType eventType, boolean requiresReason, boolean changesStatus) {
        this.eventType = eventType;
        this.requiresReason = requiresReason;
        this.changesStatus = changesStatus;
    }

    public MaintenanceEventType eventType() {
        return eventType;
    }

    public boolean requiresReason() {
        return requiresReason;
    }

    public boolean changesStatus() {
        return changesStatus;
    }

    public boolean requiresSupervisor() {
        return this == REJECT_INSPECTION || this == APPROVE_INSPECTION || this == COMPLETE;
    }

    public boolean allowsTerminalWorkOrder() {
        return this == ADD_NOTE;
    }
}
