package com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAssetId;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.event.MaintenanceEventType;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrderId;
import com.deanwagman.lumenmarsh.venueops.security.ActorIdentity;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class MaintenanceRecommendation {

    private final MaintenanceRecommendationId id;
    private final String observationId;
    private final Instant observedAt;
    private final String assetCode;
    private final MaintenanceAssetId assetId;
    private final MaintenanceSignalType signalType;
    private final MaintenanceRecommendationSeverity severity;
    private final Double value;
    private final String unit;
    private final String evidence;
    private final String recommendedAction;
    private MaintenanceRecommendationStatus status;
    private MaintenanceWorkOrderId workOrderId;
    private final Instant receivedAt;
    private Instant updatedAt;
    private long version;
    private UUID lastCommandId;
    private int uncommittedBumps;

    public MaintenanceRecommendation(
            MaintenanceRecommendationId id,
            String observationId,
            Instant observedAt,
            String assetCode,
            MaintenanceAssetId assetId,
            MaintenanceSignalType signalType,
            MaintenanceRecommendationSeverity severity,
            Double value,
            String unit,
            String evidence,
            String recommendedAction,
            MaintenanceRecommendationStatus status,
            MaintenanceWorkOrderId workOrderId,
            Instant receivedAt,
            Instant updatedAt,
            long version,
            UUID lastCommandId
    ) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.observationId = requireText(observationId, "observationId");
        this.observedAt = Objects.requireNonNull(observedAt, "observedAt is required");
        this.assetCode = requireText(assetCode, "assetCode");
        this.assetId = Objects.requireNonNull(assetId, "assetId is required");
        this.signalType = Objects.requireNonNull(signalType, "signalType is required");
        this.severity = Objects.requireNonNull(severity, "severity is required");
        this.value = value;
        this.unit = normalizeOptional(unit);
        this.evidence = requireText(evidence, "evidence");
        this.recommendedAction = requireText(recommendedAction, "recommendedAction");
        this.status = Objects.requireNonNull(status, "status is required");
        this.workOrderId = workOrderId;
        this.receivedAt = Objects.requireNonNull(receivedAt, "receivedAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
        this.version = version;
        this.lastCommandId = lastCommandId;
        this.uncommittedBumps = 0;
        if (version < 1) {
            throw new IllegalStateException("Recommendations start at version 1");
        }
    }

    public static MaintenanceRecommendation receive(
            MaintenanceRecommendationId id,
            String observationId,
            Instant observedAt,
            String assetCode,
            MaintenanceAssetId assetId,
            MaintenanceSignalType signalType,
            MaintenanceRecommendationSeverity severity,
            Double value,
            String unit,
            String evidence,
            String recommendedAction,
            Clock clock
    ) {
        Instant now = Instant.now(Objects.requireNonNull(clock, "clock is required"));
        return new MaintenanceRecommendation(
                id,
                observationId,
                observedAt,
                assetCode,
                assetId,
                signalType,
                severity,
                value,
                unit,
                evidence,
                recommendedAction,
                MaintenanceRecommendationStatus.PENDING_REVIEW,
                null,
                now,
                now,
                1L,
                null
        );
    }

    public MaintenanceEventType claimAcceptance(ActorIdentity actor, UUID commandId, Clock clock) {
        Objects.requireNonNull(actor, "actor is required");
        Objects.requireNonNull(commandId, "commandId is required");
        Objects.requireNonNull(clock, "clock is required");
        if (status != MaintenanceRecommendationStatus.PENDING_REVIEW) {
            throw new InvalidMaintenanceRecommendationTransitionException(
                    status,
                    MaintenanceRecommendationCommand.ACCEPT
            );
        }
        this.status = MaintenanceRecommendationStatus.ACCEPTED;
        this.lastCommandId = commandId;
        bump(clock);
        return MaintenanceEventType.MAINTENANCE_RECOMMENDATION_ACCEPTED;
    }

    public void attachWorkOrder(MaintenanceWorkOrderId workOrderId, Clock clock) {
        Objects.requireNonNull(workOrderId, "workOrderId is required");
        Objects.requireNonNull(clock, "clock is required");
        if (status != MaintenanceRecommendationStatus.ACCEPTED || this.workOrderId != null) {
            throw new InvalidMaintenanceRecommendationTransitionException(
                    status,
                    MaintenanceRecommendationCommand.ACCEPT
            );
        }
        this.workOrderId = workOrderId;
        this.status = MaintenanceRecommendationStatus.WORK_ORDER_CREATED;
        bump(clock);
    }

    public MaintenanceEventType dismiss(ActorIdentity actor, UUID commandId, Clock clock) {
        Objects.requireNonNull(actor, "actor is required");
        Objects.requireNonNull(commandId, "commandId is required");
        Objects.requireNonNull(clock, "clock is required");
        if (status != MaintenanceRecommendationStatus.PENDING_REVIEW) {
            throw new InvalidMaintenanceRecommendationTransitionException(
                    status,
                    MaintenanceRecommendationCommand.DISMISS
            );
        }
        this.status = MaintenanceRecommendationStatus.DISMISSED;
        this.lastCommandId = commandId;
        bump(clock);
        return MaintenanceEventType.MAINTENANCE_RECOMMENDATION_DISMISSED;
    }

    public boolean canContinueAcceptance(UUID commandId) {
        return status == MaintenanceRecommendationStatus.ACCEPTED
                && workOrderId == null
                && commandId.equals(lastCommandId);
    }

    public boolean isTerminalFor(UUID commandId) {
        return commandId.equals(lastCommandId)
                && (status == MaintenanceRecommendationStatus.WORK_ORDER_CREATED
                || status == MaintenanceRecommendationStatus.DISMISSED);
    }

    public MaintenanceRecommendationId id() {
        return id;
    }

    public String observationId() {
        return observationId;
    }

    public Instant observedAt() {
        return observedAt;
    }

    public String assetCode() {
        return assetCode;
    }

    public MaintenanceAssetId assetId() {
        return assetId;
    }

    public MaintenanceSignalType signalType() {
        return signalType;
    }

    public MaintenanceRecommendationSeverity severity() {
        return severity;
    }

    public Double value() {
        return value;
    }

    public String unit() {
        return unit;
    }

    public String evidence() {
        return evidence;
    }

    public String recommendedAction() {
        return recommendedAction;
    }

    public MaintenanceRecommendationStatus status() {
        return status;
    }

    public MaintenanceWorkOrderId workOrderId() {
        return workOrderId;
    }

    public Instant receivedAt() {
        return receivedAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public long version() {
        return version;
    }

    public UUID lastCommandId() {
        return lastCommandId;
    }

    public int uncommittedBumps() {
        return uncommittedBumps;
    }

    public void markVersionCommitted() {
        this.uncommittedBumps = 0;
    }

    private void bump(Clock clock) {
        this.version = this.version + 1;
        this.updatedAt = Instant.now(clock);
        this.uncommittedBumps = this.uncommittedBumps + 1;
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
