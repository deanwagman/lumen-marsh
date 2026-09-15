package com.deanwagman.lumenmarsh.venueops.incident.application;

import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentId;

import java.util.List;
import java.util.UUID;

public interface RelatedMaintenanceWorkOrders {

    List<RelatedWorkOrderSummary> forIncident(IncidentId incidentId);

    record RelatedWorkOrderSummary(
            UUID id,
            String workOrderNumber,
            String status,
            String priority,
            String summary
    ) {
    }
}
