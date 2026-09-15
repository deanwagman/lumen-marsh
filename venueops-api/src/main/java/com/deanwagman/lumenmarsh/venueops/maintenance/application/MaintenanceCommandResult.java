package com.deanwagman.lumenmarsh.venueops.maintenance.application;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.activity.MaintenanceActivity;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrder;

public record MaintenanceCommandResult(
        MaintenanceWorkOrder workOrder,
        MaintenanceActivity activity,
        boolean replay
) {
}
