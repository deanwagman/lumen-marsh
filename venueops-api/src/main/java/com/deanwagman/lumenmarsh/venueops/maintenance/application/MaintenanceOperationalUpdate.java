package com.deanwagman.lumenmarsh.venueops.maintenance.application;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.activity.MaintenanceActivity;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrder;

import java.time.Instant;

public record MaintenanceOperationalUpdate(
        String eventId,
        String eventType,
        String aggregateType,
        String aggregateId,
        long aggregateVersion,
        String attractionId,
        String incidentId,
        String actorSubject,
        String actorDisplayName,
        String correlationId,
        Instant occurredAt,
        MaintenanceWorkOrderSnapshot workOrder
) {
    public static final String SNAPSHOT_EVENT = "maintenance.work-orders.snapshot";
    public static final String UPDATED_EVENT = "maintenance.work-order.updated";

    public String sseEventName() {
        return UPDATED_EVENT;
    }

    public static MaintenanceOperationalUpdate from(MaintenanceActivity activity, MaintenanceWorkOrder workOrder) {
        return new MaintenanceOperationalUpdate(
                activity.id().toString(),
                activity.eventType().name(),
                "MAINTENANCE_WORK_ORDER",
                workOrder.id().toString(),
                workOrder.version(),
                workOrder.attractionId().value(),
                workOrder.incidentId() == null ? null : workOrder.incidentId().value(),
                activity.actorSubject(),
                activity.actorDisplayName(),
                activity.correlationId(),
                activity.occurredAt(),
                MaintenanceWorkOrderSnapshot.from(workOrder)
        );
    }

    public record MaintenanceWorkOrderSnapshot(
            String id,
            String workOrderNumber,
            String status,
            String priority,
            long version,
            Instant updatedAt
    ) {
        public static MaintenanceWorkOrderSnapshot from(MaintenanceWorkOrder workOrder) {
            return new MaintenanceWorkOrderSnapshot(
                    workOrder.id().toString(),
                    workOrder.workOrderNumber(),
                    workOrder.status().name(),
                    workOrder.priority().name(),
                    workOrder.version(),
                    workOrder.updatedAt()
            );
        }
    }
}
