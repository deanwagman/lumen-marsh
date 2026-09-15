package com.deanwagman.lumenmarsh.venueops.maintenance.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentId;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceWorkOrderRepository;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.StaleMaintenanceWorkOrderVersionException;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.activity.MaintenanceActivity;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAssetId;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.checklist.MaintenanceChecklistItem;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.checklist.MaintenanceChecklistItemId;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceEvidence;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrder;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrderId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "venueops.attractions.persistence", havingValue = "jpa")
public class JpaMaintenanceWorkOrderRepository implements MaintenanceWorkOrderRepository {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final MaintenanceWorkOrderJpaRepository workOrders;
    private final MaintenanceChecklistItemJpaRepository checklistItems;
    private final MaintenanceActivityJpaRepository activities;
    private final MaintenanceEvidenceJpaRepository evidence;

    public JpaMaintenanceWorkOrderRepository(
            MaintenanceWorkOrderJpaRepository workOrders,
            MaintenanceChecklistItemJpaRepository checklistItems,
            MaintenanceActivityJpaRepository activities,
            MaintenanceEvidenceJpaRepository evidence
    ) {
        this.workOrders = workOrders;
        this.checklistItems = checklistItems;
        this.activities = activities;
        this.evidence = evidence;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MaintenanceWorkOrder> findById(MaintenanceWorkOrderId id) {
        return workOrders.findById(id.toString()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceWorkOrder> findAll() {
        return workOrders.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceWorkOrder> findByAssetId(MaintenanceAssetId assetId) {
        return workOrders.findByAssetId(assetId.toString()).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void save(MaintenanceWorkOrder workOrder) {
        String id = workOrder.id().toString();
        Optional<MaintenanceWorkOrderEntity> existing = workOrders.findById(id);
        if (existing.isEmpty()) {
            workOrders.save(toEntity(workOrder));
            persistChildren(workOrder);
            persistUncommitted(workOrder);
            return;
        }
        long expectedVersion = workOrder.version() - workOrder.uncommittedActivity().size();
        int updated = workOrders.updateOperationalState(
                id,
                workOrder.incidentId() == null ? null : workOrder.incidentId().value(),
                workOrder.status(),
                workOrder.assignedTeam(),
                workOrder.assignedActorSubject(),
                workOrder.estimatedRestoreAt(),
                workOrder.openedAt(),
                workOrder.workStartedAt(),
                workOrder.readyForTestingAt(),
                workOrder.completedAt(),
                workOrder.canceledAt(),
                workOrder.updatedAt(),
                workOrder.version(),
                expectedVersion
        );
        if (updated == 0) {
            throw new StaleMaintenanceWorkOrderVersionException(
                    workOrder.id(),
                    workOrder.workOrderNumber(),
                    expectedVersion,
                    existing.get().getVersion()
            );
        }
        persistChildren(workOrder);
        persistUncommitted(workOrder);
    }

    private void persistChildren(MaintenanceWorkOrder workOrder) {
        String id = workOrder.id().toString();
        for (MaintenanceChecklistItem item : workOrder.checklist()) {
            checklistItems.save(new MaintenanceChecklistItemEntity(
                    item.id().toString(),
                    id,
                    item.sequence(),
                    item.label(),
                    item.instructions(),
                    item.required(),
                    item.result(),
                    item.notes(),
                    item.completedBySubject(),
                    item.completedByDisplayName(),
                    item.completedAt(),
                    item.version()
            ));
        }
        for (MaintenanceEvidence item : workOrder.evidence()) {
            evidence.save(new MaintenanceEvidenceEntity(
                    item.id().toString(),
                    id,
                    item.label(),
                    item.contentType(),
                    item.uri(),
                    item.addedBySubject(),
                    item.addedAt()
            ));
        }
    }

    private void persistUncommitted(MaintenanceWorkOrder workOrder) {
        for (MaintenanceActivity activity : workOrder.uncommittedActivity()) {
            activities.save(toEntity(activity));
        }
        workOrder.markActivityCommitted();
    }

    private MaintenanceWorkOrder toDomain(MaintenanceWorkOrderEntity entity) {
        String id = entity.getId();
        List<MaintenanceChecklistItem> checklist = checklistItems.findByWorkOrderIdOrderBySequenceAsc(id).stream()
                .map(item -> new MaintenanceChecklistItem(
                        MaintenanceChecklistItemId.of(item.getId()),
                        item.getSequence(),
                        item.getLabel(),
                        item.getInstructions(),
                        item.isRequired(),
                        item.getResult(),
                        item.getNotes(),
                        item.getCompletedBySubject(),
                        item.getCompletedByDisplayName(),
                        item.getCompletedAt(),
                        item.getVersion()
                ))
                .toList();
        List<MaintenanceEvidence> evidenceItems = evidence.findByWorkOrderIdOrderByAddedAtAsc(id).stream()
                .map(item -> new MaintenanceEvidence(
                        UUID.fromString(item.getId()),
                        item.getLabel(),
                        item.getContentType(),
                        item.getUri(),
                        item.getAddedBySubject(),
                        item.getAddedAt()
                ))
                .toList();
        List<MaintenanceActivity> history = activities.findByWorkOrderIdOrderBySequenceAsc(id).stream()
                .map(JpaMaintenanceWorkOrderRepository::toDomain)
                .toList();
        return MaintenanceWorkOrder.rehydrate(
                MaintenanceWorkOrderId.of(entity.getId()),
                entity.getWorkOrderNumber(),
                MaintenanceAssetId.of(entity.getAssetId()),
                new AttractionId(entity.getAttractionId()),
                entity.getIncidentId() == null ? null : new IncidentId(entity.getIncidentId()),
                entity.getSourceType(),
                entity.getSourceReferenceId(),
                entity.getClassification(),
                entity.getPriority(),
                entity.getStatus(),
                entity.getSummary(),
                entity.getDescription(),
                entity.getAssignedTeam(),
                entity.getAssignedActorSubject(),
                entity.getEstimatedRestoreAt(),
                entity.getOpenedAt(),
                entity.getWorkStartedAt(),
                entity.getReadyForTestingAt(),
                entity.getCompletedAt(),
                entity.getCanceledAt(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                checklist,
                evidenceItems,
                history
        );
    }

    private static MaintenanceWorkOrderEntity toEntity(MaintenanceWorkOrder workOrder) {
        return new MaintenanceWorkOrderEntity(
                workOrder.id().toString(),
                workOrder.workOrderNumber(),
                workOrder.assetId().toString(),
                workOrder.attractionId().value(),
                workOrder.incidentId() == null ? null : workOrder.incidentId().value(),
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
                workOrder.updatedAt()
        );
    }

    private static MaintenanceActivityEntity toEntity(MaintenanceActivity activity) {
        return new MaintenanceActivityEntity(
                activity.id().toString(),
                activity.workOrderId().toString(),
                activity.sequence(),
                activity.eventType(),
                activity.fromStatus(),
                activity.toStatus(),
                activity.actorSubject(),
                activity.actorDisplayName(),
                activity.reason(),
                JSON.writeValueAsString(activity.details()),
                activity.commandId().toString(),
                activity.correlationId(),
                activity.occurredAt(),
                activity.resultingVersion()
        );
    }

    private static MaintenanceActivity toDomain(MaintenanceActivityEntity entity) {
        return new MaintenanceActivity(
                UUID.fromString(entity.getId()),
                MaintenanceWorkOrderId.of(entity.getWorkOrderId()),
                entity.getSequence(),
                entity.getEventType(),
                entity.getFromStatus(),
                entity.getToStatus(),
                entity.getActorSubject(),
                entity.getActorDisplayName(),
                entity.getReason(),
                details(entity.getDetails()),
                UUID.fromString(entity.getCommandId()),
                entity.getCorrelationId(),
                entity.getOccurredAt(),
                entity.getResultingVersion()
        );
    }

    private static Map<String, Object> details(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        JsonNode node = JSON.readTree(json);
        Map<String, Object> details = new LinkedHashMap<>();
        node.properties().forEach(entry -> details.put(entry.getKey(), scalar(entry.getValue())));
        return details;
    }

    private static Object scalar(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isNumber()) {
            return node.asLong();
        }
        if (node.isArray()) {
            List<Object> values = new ArrayList<>();
            node.forEach(item -> values.add(scalar(item)));
            return values;
        }
        return node.asString();
    }
}
