package com.deanwagman.lumenmarsh.venueops.maintenance.application;

import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionNotFoundException;
import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionRepository;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.Attraction;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatus;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentService;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentId;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.activity.MaintenanceActivity;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAsset;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAssetId;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.checklist.ChecklistResult;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.checklist.MaintenanceChecklistItemId;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.ChecklistItemDraft;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceClassification;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceEvidence;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenancePriority;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceSourceType;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrder;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrderCommand;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrderId;
import com.deanwagman.lumenmarsh.venueops.security.ActorIdentity;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class MaintenanceWorkOrderCommandService {

    private final MaintenanceWorkOrderRepository workOrders;
    private final MaintenanceAssetRepository assets;
    private final AttractionRepository attractions;
    private final IncidentService incidents;
    private final ProcessedCommandRepository processedCommands;
    private final WorkOrderNumberGenerator numbers;
    private final MaintenanceUpdatePublisher publisher;
    private final MaintenanceMetrics metrics;
    private final Clock clock;

    public MaintenanceWorkOrderCommandService(
            MaintenanceWorkOrderRepository workOrders,
            MaintenanceAssetRepository assets,
            AttractionRepository attractions,
            IncidentService incidents,
            ProcessedCommandRepository processedCommands,
            WorkOrderNumberGenerator numbers,
            MaintenanceUpdatePublisher publisher,
            MaintenanceMetrics metrics,
            Clock clock
    ) {
        this.workOrders = Objects.requireNonNull(workOrders);
        this.assets = Objects.requireNonNull(assets);
        this.attractions = Objects.requireNonNull(attractions);
        this.incidents = Objects.requireNonNull(incidents);
        this.processedCommands = Objects.requireNonNull(processedCommands);
        this.numbers = Objects.requireNonNull(numbers);
        this.publisher = Objects.requireNonNull(publisher);
        this.metrics = Objects.requireNonNull(metrics);
        this.clock = Objects.requireNonNull(clock);
    }

    @Transactional
    public MaintenanceCommandResult create(
            UUID commandId,
            String correlationId,
            MaintenanceAssetId assetId,
            IncidentId incidentId,
            MaintenanceSourceType sourceType,
            String sourceReferenceId,
            MaintenanceClassification classification,
            MaintenancePriority priority,
            String summary,
            String description,
            List<ChecklistItemDraft> checklist,
            ActorIdentity actor
    ) {
        return processedCommands.findByCommandId(commandId)
                .map(processed -> replay(processed, null))
                .orElseGet(() -> createNew(
                        commandId,
                        correlationId,
                        assetId,
                        incidentId,
                        sourceType,
                        sourceReferenceId,
                        classification,
                        priority,
                        summary,
                        description,
                        checklist,
                        actor
                ));
    }

    @Transactional
    public MaintenanceCommandResult execute(
            MaintenanceWorkOrderId id,
            UUID commandId,
            String correlationId,
            MaintenanceWorkOrderCommand command,
            long expectedVersion,
            String reason,
            Map<String, Object> data,
            ActorIdentity actor,
            boolean supervisor
    ) {
        return processedCommands.findByCommandId(commandId)
                .map(processed -> replay(processed, id))
                .orElseGet(() -> executeNew(id, commandId, correlationId, command, expectedVersion, reason, data, actor, supervisor));
    }

    private MaintenanceCommandResult createNew(
            UUID commandId,
            String correlationId,
            MaintenanceAssetId assetId,
            IncidentId incidentId,
            MaintenanceSourceType sourceType,
            String sourceReferenceId,
            MaintenanceClassification classification,
            MaintenancePriority priority,
            String summary,
            String description,
            List<ChecklistItemDraft> checklist,
            ActorIdentity actor
    ) {
        MaintenanceAsset asset = assets.findById(assetId).orElseThrow(() -> new MaintenanceAssetNotFoundException(assetId));
        AttractionId attractionId = asset.attractionId();
        if (attractions.findById(attractionId).isEmpty()) {
            throw new AttractionNotFoundException(attractionId);
        }
        if (incidentId != null) {
            incidents.get(incidentId);
        }
        MaintenanceWorkOrder workOrder = MaintenanceWorkOrder.draft(
                MaintenanceWorkOrderId.random(),
                numbers.next(clock),
                asset.id(),
                attractionId,
                incidentId,
                sourceType == null ? MaintenanceSourceType.MANUAL : sourceType,
                sourceReferenceId,
                classification,
                priority,
                summary,
                description,
                checklist,
                actor,
                commandId,
                correlationId,
                clock
        );
        MaintenanceActivity activity = lastUncommitted(workOrder);
        workOrders.save(workOrder);
        if (incidentId != null) {
            incidents.recordLinkedWorkOrder(
                    incidentId,
                    workOrder.id().toString(),
                    workOrder.workOrderNumber(),
                    actor.auditLabel(),
                    "Maintenance work order " + workOrder.workOrderNumber() + " linked"
            );
        }
        remember(commandId, workOrder, activity);
        AfterCommit.run(() -> publisher.publish(MaintenanceOperationalUpdate.from(activity, workOrder)));
        metrics.created(workOrder);
        return new MaintenanceCommandResult(workOrder, activity, false);
    }

    private MaintenanceCommandResult executeNew(
            MaintenanceWorkOrderId id,
            UUID commandId,
            String correlationId,
            MaintenanceWorkOrderCommand command,
            long expectedVersion,
            String reason,
            Map<String, Object> data,
            ActorIdentity actor,
            boolean supervisor
    ) {
        MaintenanceWorkOrder workOrder = workOrders.findById(id)
                .orElseThrow(() -> new MaintenanceWorkOrderNotFoundException(id));
        if (workOrder.version() != expectedVersion) {
            metrics.stale();
            throw new StaleMaintenanceWorkOrderVersionException(
                    workOrder.id(),
                    workOrder.workOrderNumber(),
                    expectedVersion,
                    workOrder.version()
            );
        }
        try {
            apply(workOrder, command, commandId, correlationId, reason, data, actor, supervisor);
        } catch (RuntimeException ex) {
            metrics.rejected(command);
            throw ex;
        }
        MaintenanceActivity activity = lastUncommitted(workOrder);
        workOrders.save(workOrder);
        if (command == MaintenanceWorkOrderCommand.LINK_INCIDENT && workOrder.incidentId() != null) {
            incidents.recordLinkedWorkOrder(
                    workOrder.incidentId(),
                    workOrder.id().toString(),
                    workOrder.workOrderNumber(),
                    actor.auditLabel(),
                    reason
            );
        }
        remember(commandId, workOrder, activity);
        AfterCommit.run(() -> publisher.publish(MaintenanceOperationalUpdate.from(activity, workOrder)));
        metrics.transitioned(workOrder, command, activity);
        return new MaintenanceCommandResult(workOrder, activity, false);
    }

    private void apply(
            MaintenanceWorkOrder workOrder,
            MaintenanceWorkOrderCommand command,
            UUID commandId,
            String correlationId,
            String reason,
            Map<String, Object> data,
            ActorIdentity actor,
            boolean supervisor
    ) {
        switch (command) {
            case OPEN -> workOrder.open(actor, commandId, correlationId, reason, clock);
            case ASSIGN -> workOrder.assign(text(data, "teamId"), text(data, "actorSubject"), actor, commandId, correlationId, reason, clock);
            case START_WORK -> workOrder.startWork(actor, commandId, correlationId, reason, clock);
            case REQUEST_INSPECTION -> workOrder.requestInspection(actor, commandId, correlationId, reason, clock);
            case REJECT_INSPECTION -> workOrder.rejectInspection(actor, commandId, correlationId, reason, clock, supervisor);
            case APPROVE_INSPECTION -> workOrder.approveInspection(actor, commandId, correlationId, reason, clock, supervisor);
            case COMPLETE -> workOrder.complete(
                    actor,
                    commandId,
                    correlationId,
                    reason,
                    clock,
                    supervisor,
                    attractionStatus(workOrder.attractionId())
            );
            case CANCEL -> workOrder.cancel(actor, commandId, correlationId, reason, clock, supervisor);
            case REASSIGN -> workOrder.reassign(text(data, "teamId"), text(data, "actorSubject"), actor, commandId, correlationId, reason, clock);
            case SET_ESTIMATED_RESTORE -> workOrder.setEstimatedRestore(
                    instant(data, "estimatedRestoreAt"),
                    actor,
                    commandId,
                    correlationId,
                    reason,
                    clock
            );
            case RECORD_CHECKLIST_RESULT -> workOrder.recordChecklistResult(
                    MaintenanceChecklistItemId.of(requireText(data, "checklistItemId", "RECORD_CHECKLIST_RESULT requires checklistItemId")),
                    result(data),
                    text(data, "notes"),
                    actor,
                    commandId,
                    correlationId,
                    reason,
                    clock
            );
            case LINK_INCIDENT -> {
                IncidentId incidentId = new IncidentId(requireText(data, "incidentId", "LINK_INCIDENT requires incidentId"));
                incidents.get(incidentId);
                workOrder.linkIncident(incidentId, actor, commandId, correlationId, reason, clock);
            }
            case ADD_NOTE -> workOrder.addNote(requireText(data, "note", "ADD_NOTE requires note"), actor, commandId, correlationId, reason, clock);
            case ADD_EVIDENCE -> workOrder.addEvidence(
                    new MaintenanceEvidence(
                            UUID.randomUUID(),
                            requireText(data, "label", "ADD_EVIDENCE requires label"),
                            requireText(data, "contentType", "ADD_EVIDENCE requires contentType"),
                            requireText(data, "uri", "ADD_EVIDENCE requires uri"),
                            actor.subject(),
                            Instant.now(clock)
                    ),
                    actor,
                    commandId,
                    correlationId,
                    reason,
                    clock
            );
        }
    }

    private AttractionStatus attractionStatus(AttractionId attractionId) {
        Attraction attraction = attractions.findById(attractionId)
                .orElseThrow(() -> new AttractionNotFoundException(attractionId));
        return attraction.status();
    }

    private MaintenanceCommandResult replay(
            ProcessedCommandRepository.ProcessedCommand processed,
            MaintenanceWorkOrderId requestedId
    ) {
        if (requestedId != null && !processed.workOrderId().equals(requestedId)) {
            throw new ConflictingMaintenanceCommandException(
                    processed.commandId(),
                    processed.workOrderId().toString(),
                    requestedId.toString()
            );
        }
        return MaintenanceCommandResultSnapshot.read(processed.resultJson());
    }

    private void remember(UUID commandId, MaintenanceWorkOrder workOrder, MaintenanceActivity activity) {
        processedCommands.save(new ProcessedCommandRepository.ProcessedCommand(
                commandId,
                workOrder.id(),
                activity.eventType(),
                activity.resultingVersion(),
                activity.occurredAt(),
                MaintenanceCommandResultSnapshot.write(workOrder, activity)
        ));
    }

    private static MaintenanceActivity lastUncommitted(MaintenanceWorkOrder workOrder) {
        List<MaintenanceActivity> uncommitted = workOrder.uncommittedActivity();
        if (uncommitted.isEmpty()) {
            throw new IllegalStateException("Accepted command produced no activity");
        }
        return uncommitted.getLast();
    }

    private static ChecklistResult result(Map<String, Object> data) {
        String value = requireText(data, "result", "RECORD_CHECKLIST_RESULT requires result");
        return ChecklistResult.valueOf(value);
    }

    private static Instant instant(Map<String, Object> data, String key) {
        Object value = data == null ? null : data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        return Instant.parse(value.toString());
    }

    private static String text(Map<String, Object> data, String key) {
        Object value = data == null ? null : data.get(key);
        return value == null ? null : value.toString();
    }

    private static String requireText(Map<String, Object> data, String key, String message) {
        String value = text(data, key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
