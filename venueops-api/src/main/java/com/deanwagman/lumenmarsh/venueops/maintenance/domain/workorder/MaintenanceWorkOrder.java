package com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatus;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentId;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.activity.MaintenanceActivity;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAssetId;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.checklist.ChecklistResult;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.checklist.MaintenanceChecklistItem;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.checklist.MaintenanceChecklistItemId;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.event.MaintenanceEventType;
import com.deanwagman.lumenmarsh.venueops.security.ActorIdentity;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class MaintenanceWorkOrder {

    private final MaintenanceWorkOrderId id;
    private final String workOrderNumber;
    private final MaintenanceAssetId assetId;
    private final AttractionId attractionId;
    private IncidentId incidentId;
    private final MaintenanceSourceType sourceType;
    private final String sourceReferenceId;
    private final MaintenanceClassification classification;
    private final MaintenancePriority priority;
    private MaintenanceWorkOrderStatus status;
    private final String summary;
    private final String description;
    private String assignedTeam;
    private String assignedActorSubject;
    private Instant estimatedRestoreAt;
    private Instant openedAt;
    private Instant workStartedAt;
    private Instant readyForTestingAt;
    private Instant completedAt;
    private Instant canceledAt;
    private long version;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<MaintenanceChecklistItem> checklist;
    private final List<MaintenanceEvidence> evidence;
    private final List<MaintenanceActivity> history;
    private int committedCount;

    private MaintenanceWorkOrder(
            MaintenanceWorkOrderId id,
            String workOrderNumber,
            MaintenanceAssetId assetId,
            AttractionId attractionId,
            IncidentId incidentId,
            MaintenanceSourceType sourceType,
            String sourceReferenceId,
            MaintenanceClassification classification,
            MaintenancePriority priority,
            MaintenanceWorkOrderStatus status,
            String summary,
            String description,
            String assignedTeam,
            String assignedActorSubject,
            Instant estimatedRestoreAt,
            Instant openedAt,
            Instant workStartedAt,
            Instant readyForTestingAt,
            Instant completedAt,
            Instant canceledAt,
            long version,
            Instant createdAt,
            Instant updatedAt,
            List<MaintenanceChecklistItem> checklist,
            List<MaintenanceEvidence> evidence,
            List<MaintenanceActivity> history,
            int committedCount
    ) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.workOrderNumber = requireText(workOrderNumber, "workOrderNumber");
        this.assetId = Objects.requireNonNull(assetId, "assetId is required");
        this.attractionId = Objects.requireNonNull(attractionId, "attractionId is required");
        this.incidentId = incidentId;
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType is required");
        this.sourceReferenceId = normalizeOptional(sourceReferenceId);
        this.classification = Objects.requireNonNull(classification, "classification is required");
        this.priority = Objects.requireNonNull(priority, "priority is required");
        this.status = Objects.requireNonNull(status, "status is required");
        this.summary = requireText(summary, "summary");
        this.description = normalizeOptional(description);
        this.assignedTeam = normalizeOptional(assignedTeam);
        this.assignedActorSubject = normalizeOptional(assignedActorSubject);
        this.estimatedRestoreAt = estimatedRestoreAt;
        this.openedAt = openedAt;
        this.workStartedAt = workStartedAt;
        this.readyForTestingAt = readyForTestingAt;
        this.completedAt = completedAt;
        this.canceledAt = canceledAt;
        this.version = version;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
        this.checklist = new ArrayList<>(checklist.stream()
                .sorted(Comparator.comparingInt(MaintenanceChecklistItem::sequence))
                .map(MaintenanceChecklistItem::copy)
                .toList());
        this.evidence = new ArrayList<>(evidence);
        this.history = new ArrayList<>(history);
        this.committedCount = committedCount;
        assertInvariants();
    }

    public static MaintenanceWorkOrder draft(
            MaintenanceWorkOrderId id,
            String workOrderNumber,
            MaintenanceAssetId assetId,
            AttractionId attractionId,
            IncidentId incidentId,
            MaintenanceSourceType sourceType,
            String sourceReferenceId,
            MaintenanceClassification classification,
            MaintenancePriority priority,
            String summary,
            String description,
            List<ChecklistItemDraft> checklistDrafts,
            ActorIdentity actor,
            UUID commandId,
            String correlationId,
            Clock clock
    ) {
        requireActor(actor);
        requireClock(clock);
        Objects.requireNonNull(commandId, "commandId is required");
        Instant occurredAt = Instant.now(clock);
        List<MaintenanceChecklistItem> items = toChecklist(checklistDrafts);
        MaintenanceWorkOrder workOrder = new MaintenanceWorkOrder(
                id,
                workOrderNumber,
                assetId,
                attractionId,
                incidentId,
                sourceType,
                sourceReferenceId,
                classification,
                priority,
                MaintenanceWorkOrderStatus.DRAFT,
                summary,
                description,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                1L,
                occurredAt,
                occurredAt,
                items,
                List.of(),
                List.of(),
                0
        );
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("assetId", assetId.toString());
        details.put("classification", classification.name());
        details.put("priority", priority.name());
        if (incidentId != null) {
            details.put("incidentId", incidentId.value());
        }
        workOrder.record(
                MaintenanceEventType.WORK_ORDER_CREATED,
                null,
                MaintenanceWorkOrderStatus.DRAFT,
                actor,
                null,
                details,
                commandId,
                correlationId,
                occurredAt,
                false
        );
        return workOrder;
    }

    public static MaintenanceWorkOrder rehydrate(
            MaintenanceWorkOrderId id,
            String workOrderNumber,
            MaintenanceAssetId assetId,
            AttractionId attractionId,
            IncidentId incidentId,
            MaintenanceSourceType sourceType,
            String sourceReferenceId,
            MaintenanceClassification classification,
            MaintenancePriority priority,
            MaintenanceWorkOrderStatus status,
            String summary,
            String description,
            String assignedTeam,
            String assignedActorSubject,
            Instant estimatedRestoreAt,
            Instant openedAt,
            Instant workStartedAt,
            Instant readyForTestingAt,
            Instant completedAt,
            Instant canceledAt,
            long version,
            Instant createdAt,
            Instant updatedAt,
            List<MaintenanceChecklistItem> checklist,
            List<MaintenanceEvidence> evidence,
            List<MaintenanceActivity> activity
    ) {
        List<MaintenanceActivity> history = activity == null ? List.of() : List.copyOf(activity);
        return new MaintenanceWorkOrder(
                id,
                workOrderNumber,
                assetId,
                attractionId,
                incidentId,
                sourceType,
                sourceReferenceId,
                classification,
                priority,
                status,
                summary,
                description,
                assignedTeam,
                assignedActorSubject,
                estimatedRestoreAt,
                openedAt,
                workStartedAt,
                readyForTestingAt,
                completedAt,
                canceledAt,
                version,
                createdAt,
                updatedAt,
                checklist == null ? List.of() : checklist,
                evidence == null ? List.of() : evidence,
                history,
                history.size()
        );
    }

    public void open(ActorIdentity actor, UUID commandId, String correlationId, String reason, Clock clock) {
        applyStatus(
                MaintenanceWorkOrderCommand.OPEN,
                actor,
                commandId,
                correlationId,
                reason,
                clock,
                Map.of(),
                false,
                () -> this.openedAt = Instant.now(clock)
        );
    }

    public void assign(
            String teamId,
            String actorSubject,
            ActorIdentity actor,
            UUID commandId,
            String correlationId,
            String reason,
            Clock clock
    ) {
        String team = normalizeOptional(teamId);
        String assignee = normalizeOptional(actorSubject);
        if (team == null && assignee == null) {
            throw new MaintenancePrerequisiteException("ASSIGN requires a maintenance team or actor");
        }
        applyStatus(
                MaintenanceWorkOrderCommand.ASSIGN,
                actor,
                commandId,
                correlationId,
                reason,
                clock,
                assignmentDetails(team, assignee),
                false,
                () -> {
                    this.assignedTeam = team;
                    this.assignedActorSubject = assignee;
                }
        );
    }

    public void startWork(ActorIdentity actor, UUID commandId, String correlationId, String reason, Clock clock) {
        applyStatus(
                MaintenanceWorkOrderCommand.START_WORK,
                actor,
                commandId,
                correlationId,
                reason,
                clock,
                Map.of(),
                false,
                () -> {
                    if (this.workStartedAt == null) {
                        this.workStartedAt = Instant.now(clock);
                    }
                }
        );
    }

    public void requestInspection(ActorIdentity actor, UUID commandId, String correlationId, String reason, Clock clock) {
        if (checklist.stream().anyMatch(MaintenanceChecklistItem::blocksInspectionRequest)) {
            throw new MaintenancePrerequisiteException(
                    "Required checklist items must be passed or marked not applicable before inspection"
            );
        }
        applyStatus(
                MaintenanceWorkOrderCommand.REQUEST_INSPECTION,
                actor,
                commandId,
                correlationId,
                reason,
                clock,
                Map.of(),
                false,
                () -> {
                }
        );
    }

    public void rejectInspection(
            ActorIdentity actor,
            UUID commandId,
            String correlationId,
            String reason,
            Clock clock,
            boolean supervisor
    ) {
        requireSupervisor(MaintenanceWorkOrderCommand.REJECT_INSPECTION, supervisor);
        applyStatus(
                MaintenanceWorkOrderCommand.REJECT_INSPECTION,
                actor,
                commandId,
                correlationId,
                reason,
                clock,
                Map.of(),
                supervisor,
                () -> {
                }
        );
    }

    public void approveInspection(
            ActorIdentity actor,
            UUID commandId,
            String correlationId,
            String reason,
            Clock clock,
            boolean supervisor
    ) {
        requireSupervisor(MaintenanceWorkOrderCommand.APPROVE_INSPECTION, supervisor);
        if (checklist.stream().anyMatch(MaintenanceChecklistItem::blocksInspectionApproval)) {
            throw new MaintenancePrerequisiteException("Failed or pending required checklist items block inspection approval");
        }
        applyStatus(
                MaintenanceWorkOrderCommand.APPROVE_INSPECTION,
                actor,
                commandId,
                correlationId,
                reason,
                clock,
                Map.of(),
                supervisor,
                () -> this.readyForTestingAt = Instant.now(clock)
        );
    }

    public void complete(
            ActorIdentity actor,
            UUID commandId,
            String correlationId,
            String reason,
            Clock clock,
            boolean supervisor,
            AttractionStatus attractionStatus
    ) {
        requireSupervisor(MaintenanceWorkOrderCommand.COMPLETE, supervisor);
        Objects.requireNonNull(attractionStatus, "attractionStatus is required");
        // COMPLETE waits for the attraction testing pipeline. CLOSED, OPERATING, and WEATHER_HOLD
        // are allowed: overnight work can finish on a closed attraction, and weather holds are independent.
        if (blocksCompletion(attractionStatus)) {
            throw new MaintenancePrerequisiteException(
                    "Work order " + workOrderNumber + " cannot complete until attraction testing and return-to-service approval finish"
            );
        }
        applyStatus(
                MaintenanceWorkOrderCommand.COMPLETE,
                actor,
                commandId,
                correlationId,
                reason,
                clock,
                Map.of("attractionStatus", attractionStatus.name()),
                supervisor,
                () -> this.completedAt = Instant.now(clock)
        );
    }

    public void cancel(
            ActorIdentity actor,
            UUID commandId,
            String correlationId,
            String reason,
            Clock clock,
            boolean supervisor
    ) {
        if (priority.requiresSupervisorToCancel() && !supervisor) {
            throw new MaintenanceSupervisorRequiredException("Canceling P1 or P2 work requires a supervisor");
        }
        applyStatus(
                MaintenanceWorkOrderCommand.CANCEL,
                actor,
                commandId,
                correlationId,
                reason,
                clock,
                Map.of(),
                supervisor,
                () -> this.canceledAt = Instant.now(clock)
        );
    }

    public void reassign(
            String teamId,
            String actorSubject,
            ActorIdentity actor,
            UUID commandId,
            String correlationId,
            String reason,
            Clock clock
    ) {
        requireMutable(MaintenanceWorkOrderCommand.REASSIGN);
        if (status != MaintenanceWorkOrderStatus.ASSIGNED && status != MaintenanceWorkOrderStatus.IN_PROGRESS) {
            throw new InvalidMaintenanceTransitionException(status, MaintenanceWorkOrderCommand.REASSIGN);
        }
        String team = normalizeOptional(teamId);
        String assignee = normalizeOptional(actorSubject);
        if (team == null && assignee == null) {
            throw new MaintenancePrerequisiteException("REASSIGN requires a maintenance team or actor");
        }
        requireActor(actor);
        requireClock(clock);
        requireCommand(commandId, correlationId);
        requireReason(MaintenanceWorkOrderCommand.REASSIGN, reason);
        Instant occurredAt = Instant.now(clock);
        this.assignedTeam = team;
        this.assignedActorSubject = assignee;
        record(
                MaintenanceEventType.WORK_ORDER_REASSIGNED,
                status,
                status,
                actor,
                reason,
                assignmentDetails(team, assignee),
                commandId,
                correlationId,
                occurredAt,
                true
        );
    }

    public void setEstimatedRestore(
            Instant estimatedRestoreAt,
            ActorIdentity actor,
            UUID commandId,
            String correlationId,
            String reason,
            Clock clock
    ) {
        requireMutable(MaintenanceWorkOrderCommand.SET_ESTIMATED_RESTORE);
        requireActor(actor);
        requireClock(clock);
        requireCommand(commandId, correlationId);
        Instant occurredAt = Instant.now(clock);
        if (estimatedRestoreAt != null && !estimatedRestoreAt.isAfter(occurredAt)) {
            throw new MaintenancePrerequisiteException("Restoration estimate must be in the future or explicitly removed");
        }
        this.estimatedRestoreAt = estimatedRestoreAt;
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("estimatedRestoreAt", estimatedRestoreAt == null ? null : estimatedRestoreAt.toString());
        record(
                MaintenanceEventType.RESTORE_ESTIMATE_UPDATED,
                status,
                status,
                actor,
                reason,
                details,
                commandId,
                correlationId,
                occurredAt,
                true
        );
    }

    public void recordChecklistResult(
            MaintenanceChecklistItemId checklistItemId,
            ChecklistResult result,
            String notes,
            ActorIdentity actor,
            UUID commandId,
            String correlationId,
            String reason,
            Clock clock
    ) {
        requireMutable(MaintenanceWorkOrderCommand.RECORD_CHECKLIST_RESULT);
        requireActor(actor);
        requireClock(clock);
        requireCommand(commandId, correlationId);
        MaintenanceChecklistItem item = checklist.stream()
                .filter(candidate -> candidate.id().equals(checklistItemId))
                .findFirst()
                .orElseThrow(() -> new MaintenancePrerequisiteException("Checklist item not found: " + checklistItemId));
        Instant occurredAt = Instant.now(clock);
        item.recordResult(result, notes, actor.subject(), actor.displayName(), occurredAt);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("checklistItemId", checklistItemId.toString());
        details.put("result", result.name());
        if (notes != null && !notes.isBlank()) {
            details.put("notes", notes.trim());
        }
        record(
                MaintenanceEventType.CHECKLIST_RESULT_RECORDED,
                status,
                status,
                actor,
                reason,
                details,
                commandId,
                correlationId,
                occurredAt,
                true
        );
    }

    public void linkIncident(
            IncidentId incidentId,
            ActorIdentity actor,
            UUID commandId,
            String correlationId,
            String reason,
            Clock clock
    ) {
        requireMutable(MaintenanceWorkOrderCommand.LINK_INCIDENT);
        Objects.requireNonNull(incidentId, "incidentId is required");
        requireActor(actor);
        requireClock(clock);
        requireCommand(commandId, correlationId);
        if (this.incidentId != null && this.incidentId.equals(incidentId)) {
            throw new MaintenancePrerequisiteException("Work order is already linked to incident " + incidentId);
        }
        Instant occurredAt = Instant.now(clock);
        this.incidentId = incidentId;
        record(
                MaintenanceEventType.WORK_ORDER_INCIDENT_LINKED,
                status,
                status,
                actor,
                reason,
                Map.of("incidentId", incidentId.value()),
                commandId,
                correlationId,
                occurredAt,
                true
        );
    }

    public void addNote(
            String note,
            ActorIdentity actor,
            UUID commandId,
            String correlationId,
            String reason,
            Clock clock
    ) {
        requireActor(actor);
        requireClock(clock);
        requireCommand(commandId, correlationId);
        requireReason(MaintenanceWorkOrderCommand.ADD_NOTE, reason == null ? note : reason);
        Instant occurredAt = Instant.now(clock);
        record(
                MaintenanceEventType.NOTE_ADDED,
                status,
                status,
                actor,
                requireText(note, "note"),
                Map.of("note", note.trim()),
                commandId,
                correlationId,
                occurredAt,
                true
        );
    }

    public void addEvidence(
            MaintenanceEvidence evidenceItem,
            ActorIdentity actor,
            UUID commandId,
            String correlationId,
            String reason,
            Clock clock
    ) {
        requireMutable(MaintenanceWorkOrderCommand.ADD_EVIDENCE);
        requireActor(actor);
        requireClock(clock);
        requireCommand(commandId, correlationId);
        Objects.requireNonNull(evidenceItem, "evidence is required");
        Instant occurredAt = Instant.now(clock);
        this.evidence.add(evidenceItem);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("evidenceId", evidenceItem.id().toString());
        details.put("label", evidenceItem.label());
        details.put("contentType", evidenceItem.contentType());
        record(
                MaintenanceEventType.EVIDENCE_ADDED,
                status,
                status,
                actor,
                reason,
                details,
                commandId,
                correlationId,
                occurredAt,
                true
        );
    }

    public RecommendedAttractionAction recommendedAttractionAction() {
        if (status.isTerminal()) {
            return null;
        }
        if (status == MaintenanceWorkOrderStatus.READY_FOR_TESTING) {
            return new RecommendedAttractionAction(
                    "START_TESTING",
                    "Work order " + workOrderNumber + " is ready for operational testing."
            );
        }
        if (priority.isUrgentCorrective() && classification == MaintenanceClassification.CORRECTIVE) {
            return new RecommendedAttractionAction(
                    "REPORT_TECHNICAL_FAULT",
                    "P1 corrective work order " + workOrderNumber + " is active."
            );
        }
        return null;
    }

    public MaintenanceWorkOrderId id() {
        return id;
    }

    public String workOrderNumber() {
        return workOrderNumber;
    }

    public MaintenanceAssetId assetId() {
        return assetId;
    }

    public AttractionId attractionId() {
        return attractionId;
    }

    public IncidentId incidentId() {
        return incidentId;
    }

    public MaintenanceSourceType sourceType() {
        return sourceType;
    }

    public String sourceReferenceId() {
        return sourceReferenceId;
    }

    public MaintenanceClassification classification() {
        return classification;
    }

    public MaintenancePriority priority() {
        return priority;
    }

    public MaintenanceWorkOrderStatus status() {
        return status;
    }

    public String summary() {
        return summary;
    }

    public String description() {
        return description;
    }

    public String assignedTeam() {
        return assignedTeam;
    }

    public String assignedActorSubject() {
        return assignedActorSubject;
    }

    public Instant estimatedRestoreAt() {
        return estimatedRestoreAt;
    }

    public Instant openedAt() {
        return openedAt;
    }

    public Instant workStartedAt() {
        return workStartedAt;
    }

    public Instant readyForTestingAt() {
        return readyForTestingAt;
    }

    public Instant completedAt() {
        return completedAt;
    }

    public Instant canceledAt() {
        return canceledAt;
    }

    public long version() {
        return version;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public List<MaintenanceChecklistItem> checklist() {
        return checklist.stream().map(MaintenanceChecklistItem::copy).toList();
    }

    public List<MaintenanceEvidence> evidence() {
        return List.copyOf(evidence);
    }

    public List<MaintenanceActivity> activity() {
        return List.copyOf(history);
    }

    public List<MaintenanceActivity> uncommittedActivity() {
        return List.copyOf(history.subList(committedCount, history.size()));
    }

    public void markActivityCommitted() {
        committedCount = history.size();
    }

    public boolean isHighPriorityActive() {
        return status.isActive() && priority.requiresSupervisorToCancel();
    }

    private void applyStatus(
            MaintenanceWorkOrderCommand command,
            ActorIdentity actor,
            UUID commandId,
            String correlationId,
            String reason,
            Clock clock,
            Map<String, Object> details,
            boolean supervisor,
            Runnable mutation
    ) {
        requireMutable(command);
        requireActor(actor);
        requireClock(clock);
        requireCommand(commandId, correlationId);
        requireReason(command, reason);
        if (command.requiresSupervisor() && !supervisor) {
            throw new MaintenanceSupervisorRequiredException(command + " requires a supervisor");
        }
        MaintenanceWorkOrderStatus previous = this.status;
        MaintenanceWorkOrderStatus next = MaintenanceWorkOrderStateMachine.nextStatus(previous, command);
        Instant occurredAt = Instant.now(clock);
        mutation.run();
        this.status = next;
        MaintenanceEventType eventType = command == MaintenanceWorkOrderCommand.APPROVE_INSPECTION
                ? MaintenanceEventType.INSPECTION_APPROVED
                : command.eventType();
        record(eventType, previous, next, actor, reason, details, commandId, correlationId, occurredAt, true);
    }

    private void record(
            MaintenanceEventType eventType,
            MaintenanceWorkOrderStatus fromStatus,
            MaintenanceWorkOrderStatus toStatus,
            ActorIdentity actor,
            String reason,
            Map<String, Object> details,
            UUID commandId,
            String correlationId,
            Instant occurredAt,
            boolean bumpVersion
    ) {
        if (bumpVersion) {
            this.version = this.version + 1;
            this.updatedAt = occurredAt;
        }
        history.add(new MaintenanceActivity(
                UUID.randomUUID(),
                id,
                nextSequence(),
                eventType,
                fromStatus,
                toStatus,
                actor.subject(),
                actor.displayName(),
                normalizeOptional(reason),
                sanitize(details),
                commandId,
                requireText(correlationId, "correlationId"),
                occurredAt,
                this.version
        ));
        assertInvariants();
    }

    private long nextSequence() {
        return history.isEmpty() ? 1L : history.getLast().sequence() + 1;
    }

    private void requireMutable(MaintenanceWorkOrderCommand command) {
        if (status.isTerminal() && !command.allowsTerminalWorkOrder()) {
            throw new InvalidMaintenanceTransitionException(status, command);
        }
    }

    private void assertInvariants() {
        if (version < 1) {
            throw new IllegalStateException("Work orders start at version 1");
        }
        if (history.isEmpty() && committedCount == 0 && status != MaintenanceWorkOrderStatus.DRAFT) {
            throw new IllegalStateException("Work orders must have an audited create event");
        }
    }

    private static List<MaintenanceChecklistItem> toChecklist(List<ChecklistItemDraft> drafts) {
        if (drafts == null || drafts.isEmpty()) {
            return List.of();
        }
        List<MaintenanceChecklistItem> items = new ArrayList<>();
        for (ChecklistItemDraft draft : drafts) {
            items.add(MaintenanceChecklistItem.pending(
                    MaintenanceChecklistItemId.random(),
                    draft.sequence(),
                    draft.label(),
                    draft.instructions(),
                    draft.required()
            ));
        }
        return items;
    }

    private static Map<String, Object> assignmentDetails(String team, String assignee) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("teamId", team);
        details.put("actorSubject", assignee);
        return details;
    }

    private static Map<String, Object> sanitize(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> sanitized = new LinkedHashMap<>();
        details.forEach((key, value) -> {
            if (key == null || value == null) {
                return;
            }
            String lower = key.toLowerCase();
            if (lower.contains("authorization")
                    || lower.contains("token")
                    || lower.contains("secret")
                    || lower.contains("password")
                    || lower.contains("header")) {
                return;
            }
            sanitized.put(key, value);
        });
        return sanitized;
    }

    private static boolean blocksCompletion(AttractionStatus attractionStatus) {
        return attractionStatus == AttractionStatus.TECHNICAL_DELAY
                || attractionStatus == AttractionStatus.TESTING
                || attractionStatus == AttractionStatus.RETURNING_TO_SERVICE;
    }

    private static void requireSupervisor(MaintenanceWorkOrderCommand command, boolean supervisor) {
        if (!supervisor) {
            throw new MaintenanceSupervisorRequiredException(command + " requires a supervisor");
        }
    }

    private static void requireActor(ActorIdentity actor) {
        Objects.requireNonNull(actor, "actor is required");
    }

    private static void requireClock(Clock clock) {
        Objects.requireNonNull(clock, "clock is required");
    }

    private static void requireCommand(UUID commandId, String correlationId) {
        Objects.requireNonNull(commandId, "commandId is required");
        requireText(correlationId, "correlationId");
    }

    private static void requireReason(MaintenanceWorkOrderCommand command, String reason) {
        if (command.requiresReason() && isBlank(reason)) {
            throw new IllegalArgumentException(command + " requires a meaningful reason");
        }
    }

    private static String normalizeOptional(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record RecommendedAttractionAction(String command, String reason) {
    }
}
