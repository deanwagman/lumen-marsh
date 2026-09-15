package com.deanwagman.lumenmarsh.venueops.maintenance.api;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrderCommand;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record MaintenanceWorkOrderCommandRequest(
        @NotNull UUID commandId,
        @NotNull MaintenanceWorkOrderCommand type,
        @NotNull Long expectedVersion,
        String reason,
        Map<String, Object> data
) {
}
