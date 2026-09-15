package com.deanwagman.lumenmarsh.venueops.maintenance.domain.activity;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.event.MaintenanceEventType;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrderId;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrderStatus;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record MaintenanceActivity(
        UUID id,
        MaintenanceWorkOrderId workOrderId,
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
    public MaintenanceActivity {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(workOrderId, "workOrderId is required");
        Objects.requireNonNull(eventType, "eventType is required");
        Objects.requireNonNull(actorSubject, "actorSubject is required");
        Objects.requireNonNull(actorDisplayName, "actorDisplayName is required");
        Objects.requireNonNull(commandId, "commandId is required");
        Objects.requireNonNull(correlationId, "correlationId is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
        details = details == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(details));
    }
}
