package com.deanwagman.lumenmarsh.venueops.maintenance.infrastructure;

import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceWorkOrderRepository;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.StaleMaintenanceWorkOrderVersionException;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAssetId;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrder;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrderId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "venueops.attractions.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryMaintenanceWorkOrderRepository implements MaintenanceWorkOrderRepository {

    private final Map<MaintenanceWorkOrderId, MaintenanceWorkOrder> workOrders = new ConcurrentHashMap<>();

    @Override
    public Optional<MaintenanceWorkOrder> findById(MaintenanceWorkOrderId id) {
        return Optional.ofNullable(workOrders.get(id)).map(InMemoryMaintenanceWorkOrderRepository::copyOf);
    }

    @Override
    public List<MaintenanceWorkOrder> findAll() {
        return workOrders.values().stream()
                .map(InMemoryMaintenanceWorkOrderRepository::copyOf)
                .sorted(Comparator.comparing(MaintenanceWorkOrder::updatedAt).reversed())
                .toList();
    }

    @Override
    public List<MaintenanceWorkOrder> findByAssetId(MaintenanceAssetId assetId) {
        return findAll().stream()
                .filter(workOrder -> workOrder.assetId().equals(assetId))
                .toList();
    }

    @Override
    public synchronized void save(MaintenanceWorkOrder workOrder) {
        MaintenanceWorkOrder existing = workOrders.get(workOrder.id());
        if (existing != null) {
            long expectedVersion = workOrder.version() - workOrder.uncommittedActivity().size();
            if (existing.version() != expectedVersion) {
                throw new StaleMaintenanceWorkOrderVersionException(
                        workOrder.id(),
                        workOrder.workOrderNumber(),
                        expectedVersion,
                        existing.version()
                );
            }
        }
        workOrders.put(workOrder.id(), copyOf(workOrder));
        workOrder.markActivityCommitted();
    }

    private static MaintenanceWorkOrder copyOf(MaintenanceWorkOrder workOrder) {
        return MaintenanceWorkOrder.rehydrate(
                workOrder.id(),
                workOrder.workOrderNumber(),
                workOrder.assetId(),
                workOrder.attractionId(),
                workOrder.incidentId(),
                workOrder.sourceType(),
                workOrder.sourceReferenceId(),
                workOrder.classification(),
                workOrder.priority(),
                workOrder.status(),
                workOrder.summary(),
                workOrder.description(),
                workOrder.assignedTeam(),
                workOrder.assignedActorSubject(),
                workOrder.estimatedRestoreAt(),
                workOrder.openedAt(),
                workOrder.workStartedAt(),
                workOrder.readyForTestingAt(),
                workOrder.completedAt(),
                workOrder.canceledAt(),
                workOrder.version(),
                workOrder.createdAt(),
                workOrder.updatedAt(),
                workOrder.checklist(),
                workOrder.evidence(),
                workOrder.activity()
        );
    }
}
