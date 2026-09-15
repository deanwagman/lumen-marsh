package com.deanwagman.lumenmarsh.venueops.maintenance.application;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.event.MaintenanceEventType;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrderId;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ProcessedCommandRepository {

    Optional<ProcessedCommand> findByCommandId(UUID commandId);

    void save(ProcessedCommand command);

    record ProcessedCommand(
            UUID commandId,
            MaintenanceWorkOrderId workOrderId,
            MaintenanceEventType eventType,
            long resultingVersion,
            Instant occurredAt,
            String resultJson
    ) {
    }
}
