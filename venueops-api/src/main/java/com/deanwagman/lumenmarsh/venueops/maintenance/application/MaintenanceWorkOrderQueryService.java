package com.deanwagman.lumenmarsh.venueops.maintenance.application;

import com.deanwagman.lumenmarsh.venueops.incident.application.ActiveHighPriorityWorkOrdersException;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentResolutionGuard;
import com.deanwagman.lumenmarsh.venueops.incident.application.RelatedMaintenanceWorkOrders;
import com.deanwagman.lumenmarsh.venueops.incident.domain.Incident;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentId;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.activity.MaintenanceActivity;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrder;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrderId;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class MaintenanceWorkOrderQueryService implements RelatedMaintenanceWorkOrders, IncidentResolutionGuard {

    private final MaintenanceWorkOrderRepository workOrders;

    public MaintenanceWorkOrderQueryService(MaintenanceWorkOrderRepository workOrders) {
        this.workOrders = Objects.requireNonNull(workOrders);
    }

    public PageResult<MaintenanceWorkOrder> list(MaintenanceWorkOrderFilter filter) {
        List<MaintenanceWorkOrder> matches = workOrders.findAll().stream()
                .filter(workOrder -> matches(workOrder, filter))
                .sorted(workOrderOrder())
                .toList();
        return MaintenanceAssetQueryService.page(matches, filter.page(), filter.size());
    }

    public MaintenanceWorkOrder get(MaintenanceWorkOrderId id) {
        return workOrders.findById(id).orElseThrow(() -> new MaintenanceWorkOrderNotFoundException(id));
    }

    public List<MaintenanceActivity> activity(MaintenanceWorkOrderId id) {
        return get(id).activity();
    }

    @Override
    public List<RelatedWorkOrderSummary> forIncident(IncidentId incidentId) {
        return workOrders.findAll().stream()
                .filter(workOrder -> workOrder.incidentId() != null && workOrder.incidentId().equals(incidentId))
                .sorted(workOrderOrder())
                .map(workOrder -> new RelatedWorkOrderSummary(
                        workOrder.id().value(),
                        workOrder.workOrderNumber(),
                        workOrder.status().name(),
                        workOrder.priority().name(),
                        workOrder.summary()
                ))
                .toList();
    }

    @Override
    public void assertResolutionAllowed(Incident incident, boolean confirmActiveWorkOrders) {
        List<String> active = workOrders.findAll().stream()
                .filter(workOrder -> workOrder.incidentId() != null && workOrder.incidentId().equals(incident.id()))
                .filter(MaintenanceWorkOrder::isHighPriorityActive)
                .map(MaintenanceWorkOrder::workOrderNumber)
                .toList();
        if (!active.isEmpty() && !confirmActiveWorkOrders) {
            throw new ActiveHighPriorityWorkOrdersException(active);
        }
    }

    static Comparator<MaintenanceWorkOrder> workOrderOrder() {
        return Comparator
                .comparing(MaintenanceWorkOrder::priority)
                .thenComparing(MaintenanceWorkOrder::updatedAt, Comparator.reverseOrder())
                .thenComparing(workOrder -> workOrder.id().toString());
    }

    private static boolean matches(MaintenanceWorkOrder workOrder, MaintenanceWorkOrderFilter filter) {
        if (filter.status() != null && workOrder.status() != filter.status()) {
            return false;
        }
        if (filter.priority() != null && workOrder.priority() != filter.priority()) {
            return false;
        }
        if (filter.classification() != null && workOrder.classification() != filter.classification()) {
            return false;
        }
        if (filter.attractionId() != null && !filter.attractionId().isBlank()
                && !workOrder.attractionId().value().equals(filter.attractionId())) {
            return false;
        }
        if (filter.assetId() != null && !workOrder.assetId().equals(filter.assetId())) {
            return false;
        }
        if (filter.incidentId() != null && !filter.incidentId().isBlank()) {
            if (workOrder.incidentId() == null || !workOrder.incidentId().value().equals(filter.incidentId())) {
                return false;
            }
        }
        if (filter.assignedTeam() != null && !filter.assignedTeam().isBlank()
                && !filter.assignedTeam().equals(workOrder.assignedTeam())) {
            return false;
        }
        if (filter.createdFrom() != null && workOrder.createdAt().isBefore(filter.createdFrom())) {
            return false;
        }
        if (filter.createdTo() != null && workOrder.createdAt().isAfter(filter.createdTo())) {
            return false;
        }
        if (filter.lifecycle() == MaintenanceWorkOrderLifecycle.ACTIVE && workOrder.status().isTerminal()) {
            return false;
        }
        if (filter.lifecycle() == MaintenanceWorkOrderLifecycle.TERMINAL && !workOrder.status().isTerminal()) {
            return false;
        }
        return true;
    }
}
