package com.deanwagman.lumenmarsh.venueops.maintenance.api;

import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceCommandResult;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.activity.MaintenanceActivity;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.event.MaintenanceEventType;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrder;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrderStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MaintenanceCommandResponse(
        WorkOrderSummary workOrder,
        ActivitySummary activity,
        boolean replay
) {
    public static MaintenanceCommandResponse from(MaintenanceCommandResult result) {
        MaintenanceWorkOrder workOrder = result.workOrder();
        MaintenanceActivity activity = result.activity();
        return new MaintenanceCommandResponse(
                new WorkOrderSummary(
                        workOrder.id().value(),
                        workOrder.workOrderNumber(),
                        workOrder.status(),
                        workOrder.version(),
                        workOrder.updatedAt()
                ),
                activity == null ? null : new ActivitySummary(
                        activity.eventType(),
                        activity.actorDisplayName(),
                        activity.occurredAt(),
                        activity.resultingVersion(),
                        activity.details()
                ),
                result.replay()
        );
    }

    public record WorkOrderSummary(
            UUID id,
            String workOrderNumber,
            MaintenanceWorkOrderStatus status,
            long version,
            Instant updatedAt
    ) {
    }

    public record ActivitySummary(
            MaintenanceEventType eventType,
            String actorDisplayName,
            Instant occurredAt,
            long resultingVersion,
            Map<String, Object> details
    ) {
    }
}
