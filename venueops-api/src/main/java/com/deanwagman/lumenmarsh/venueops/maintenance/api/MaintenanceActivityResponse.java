package com.deanwagman.lumenmarsh.venueops.maintenance.api;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.activity.MaintenanceActivity;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.event.MaintenanceEventType;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrderStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MaintenanceActivityResponse(
        UUID id,
        long sequence,
        MaintenanceEventType eventType,
        MaintenanceWorkOrderStatus fromStatus,
        MaintenanceWorkOrderStatus toStatus,
        String actorSubject,
        String actorDisplayName,
        String reason,
        Map<String, Object> details,
        UUID commandId,
        String correlationId,
        Instant occurredAt,
        long resultingVersion
) {
    public static MaintenanceActivityResponse from(MaintenanceActivity activity) {
        return new MaintenanceActivityResponse(
                activity.id(),
                activity.sequence(),
                activity.eventType(),
                activity.fromStatus(),
                activity.toStatus(),
                activity.actorSubject(),
                activity.actorDisplayName(),
                activity.reason(),
                activity.details(),
                activity.commandId(),
                activity.correlationId(),
                activity.occurredAt(),
                activity.resultingVersion()
        );
    }
}
