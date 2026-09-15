package com.deanwagman.lumenmarsh.venueops.maintenance.application;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAssetId;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceClassification;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenancePriority;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrderStatus;

import java.time.Instant;

public record MaintenanceWorkOrderFilter(
        MaintenanceWorkOrderStatus status,
        MaintenancePriority priority,
        MaintenanceClassification classification,
        String attractionId,
        MaintenanceAssetId assetId,
        String incidentId,
        String assignedTeam,
        Instant createdFrom,
        Instant createdTo,
        MaintenanceWorkOrderLifecycle lifecycle,
        int page,
        int size
) {
    public MaintenanceWorkOrderFilter {
        if (lifecycle == null) {
            lifecycle = MaintenanceWorkOrderLifecycle.ALL;
        }
        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = MaintenanceAssetFilter.DEFAULT_SIZE;
        }
        if (size > MaintenanceAssetFilter.MAX_SIZE) {
            size = MaintenanceAssetFilter.MAX_SIZE;
        }
    }
}
