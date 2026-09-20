package com.deanwagman.lumenmarsh.venueops.flow.application;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentRepository;
import com.deanwagman.lumenmarsh.venueops.incident.domain.Incident;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceWorkOrderRepository;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrder;

import java.util.Comparator;
import java.util.Objects;

public class FlowRelatedOperations {

    public record Links(String incidentId, String workOrderId) {
    }

    private final IncidentRepository incidents;
    private final MaintenanceWorkOrderRepository workOrders;

    public FlowRelatedOperations(
            IncidentRepository incidents,
            MaintenanceWorkOrderRepository workOrders
    ) {
        this.incidents = Objects.requireNonNull(incidents);
        this.workOrders = Objects.requireNonNull(workOrders);
    }

    public Links linksFor(AttractionId attractionId) {
        String incidentId = incidents.findAll().stream()
                .filter(incident -> !incident.status().isResolved())
                .filter(incident -> incident.attractionIds().contains(attractionId))
                .max(Comparator.comparing(Incident::updatedAt))
                .map(incident -> incident.id().value())
                .orElse(null);
        String workOrderId = workOrders.findAll().stream()
                .filter(workOrder -> workOrder.status().isActive())
                .filter(workOrder -> attractionId.equals(workOrder.attractionId()))
                .max(Comparator.comparing(MaintenanceWorkOrder::updatedAt))
                .map(workOrder -> workOrder.id().value().toString())
                .orElse(null);
        return new Links(incidentId, workOrderId);
    }
}
