package com.deanwagman.lumenmarsh.venueops.flow.domain;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.security.ActorIdentity;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class FlowRecommendation {

    private final FlowRecommendationId id;
    private final FlowRecommendationType type;
    private FlowRecommendationStatus status;
    private final FlowRecommendationSeverity severity;
    private final AttractionId sourceAttractionId;
    private final LinkedHashSet<AttractionId> affectedAttractionIds;
    private final LinkedHashSet<AttractionId> recommendedDestinationIds;
    private final String summary;
    private final String explanation;
    private String guestMessage;
    private final Instant expiresAt;
    private final String relatedIncidentId;
    private final String relatedWorkOrderId;
    private long version;
    private final Instant createdAt;
    private Instant updatedAt;
    private final boolean simulated;
    private final List<FlowActivity> history;
    private int committedCount;

    private FlowRecommendation(
            FlowRecommendationId id,
            FlowRecommendationType type,
            FlowRecommendationStatus status,
            FlowRecommendationSeverity severity,
            AttractionId sourceAttractionId,
            Set<AttractionId> affectedAttractionIds,
            Set<AttractionId> recommendedDestinationIds,
            String summary,
            String explanation,
            String guestMessage,
            Instant expiresAt,
            String relatedIncidentId,
            String relatedWorkOrderId,
            long version,
            Instant createdAt,
            Instant updatedAt,
            boolean simulated,
            List<FlowActivity> history,
            int committedCount
    ) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.type = Objects.requireNonNull(type, "type is required");
        this.status = Objects.requireNonNull(status, "status is required");
        this.severity = Objects.requireNonNull(severity, "severity is required");
        this.sourceAttractionId = sourceAttractionId;
        this.affectedAttractionIds = new LinkedHashSet<>(affectedAttractionIds);
        this.recommendedDestinationIds = new LinkedHashSet<>(recommendedDestinationIds);
        this.summary = requireText(summary, "summary");
        this.explanation = requireText(explanation, "explanation");
        this.guestMessage = normalizeOptional(guestMessage);
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt is required");
        this.relatedIncidentId = normalizeOptional(relatedIncidentId);
        this.relatedWorkOrderId = normalizeOptional(relatedWorkOrderId);
        this.version = version;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
        this.simulated = simulated;
        this.history = new ArrayList<>(history);
        this.committedCount = committedCount;
        if (version < 1) {
            throw new IllegalStateException("Recommendations start at version 1");
        }
    }

    public static FlowRecommendation create(
            FlowRecommendationId id,
            FlowRecommendationType type,
            FlowRecommendationSeverity severity,
            AttractionId sourceAttractionId,
            List<AttractionId> affectedAttractionIds,
            List<AttractionId> recommendedDestinationIds,
            String summary,
            String explanation,
            String guestMessage,
            Instant expiresAt,
            String relatedIncidentId,
            String relatedWorkOrderId,
            boolean simulated,
            ActorIdentity actor,
            UUID commandId,
            String correlationId,
            Clock clock
    ) {
        Instant now = Instant.now(Objects.requireNonNull(clock, "clock is required"));
        FlowRecommendation recommendation = new FlowRecommendation(
                id,
                type,
                FlowRecommendationStatus.PENDING_REVIEW,
                severity,
                sourceAttractionId,
                unique(affectedAttractionIds),
                unique(recommendedDestinationIds),
                summary,
                explanation,
                guestMessage,
                expiresAt,
                relatedIncidentId,
                relatedWorkOrderId,
                1L,
                now,
                now,
                simulated,
                List.of(),
                0
        );
        recommendation.record(
                FlowEventType.RECOMMENDATION_CREATED,
                null,
                FlowRecommendationStatus.PENDING_REVIEW,
                actor,
                commandId,
                correlationId,
                null,
                Map.of("simulated", simulated),
                now
        );
        return recommendation;
    }

    public static FlowRecommendation rehydrate(
            FlowRecommendationId id,
            FlowRecommendationType type,
            FlowRecommendationStatus status,
            FlowRecommendationSeverity severity,
            AttractionId sourceAttractionId,
            List<AttractionId> affectedAttractionIds,
            List<AttractionId> recommendedDestinationIds,
            String summary,
            String explanation,
            String guestMessage,
            Instant expiresAt,
            String relatedIncidentId,
            String relatedWorkOrderId,
            long version,
            Instant createdAt,
            Instant updatedAt,
            boolean simulated,
            List<FlowActivity> history
    ) {
        return new FlowRecommendation(
                id,
                type,
                status,
                severity,
                sourceAttractionId,
                unique(affectedAttractionIds),
                unique(recommendedDestinationIds),
                summary,
                explanation,
                guestMessage,
                expiresAt,
                relatedIncidentId,
                relatedWorkOrderId,
                version,
                createdAt,
                updatedAt,
                simulated,
                history,
                history.size()
        );
    }

    public FlowActivity apply(
            FlowRecommendationCommand command,
            ActorIdentity actor,
            UUID commandId,
            String correlationId,
            String reason,
            String guestMessage,
            Clock clock
    ) {
        Objects.requireNonNull(command, "command is required");
        Objects.requireNonNull(actor, "actor is required");
        Objects.requireNonNull(commandId, "commandId is required");
        Objects.requireNonNull(clock, "clock is required");
        Instant now = Instant.now(clock);
        return switch (command) {
            case APPROVE -> transition(
                    FlowRecommendationStatus.PENDING_REVIEW,
                    FlowRecommendationStatus.APPROVED,
                    FlowEventType.RECOMMENDATION_APPROVED,
                    command,
                    actor,
                    commandId,
                    correlationId,
                    reason,
                    Map.of(),
                    now
            );
            case DISMISS -> dismiss(actor, commandId, correlationId, reason, now);
            case PUBLISH -> publish(actor, commandId, correlationId, reason, guestMessage, now);
            case WITHDRAW -> withdraw(actor, commandId, correlationId, reason, now);
        };
    }

    public FlowActivity expireIfDue(Clock clock) {
        Objects.requireNonNull(clock, "clock is required");
        Instant now = Instant.now(clock);
        if (!status.isOpen() || now.isBefore(expiresAt)) {
            return null;
        }
        FlowRecommendationStatus from = status;
        status = FlowRecommendationStatus.EXPIRED;
        return record(
                FlowEventType.RECOMMENDATION_EXPIRED,
                from,
                FlowRecommendationStatus.EXPIRED,
                ActorIdentity.system(),
                UUID.fromString("00000000-0000-4000-8000-000000000000"),
                "flow-expire",
                "Recommendation expired",
                Map.of("expiresAt", expiresAt.toString()),
                now
        );
    }

    private FlowActivity dismiss(
            ActorIdentity actor,
            UUID commandId,
            String correlationId,
            String reason,
            Instant now
    ) {
        if (status != FlowRecommendationStatus.PENDING_REVIEW && status != FlowRecommendationStatus.APPROVED) {
            throw new InvalidFlowRecommendationTransitionException(status, FlowRecommendationCommand.DISMISS);
        }
        requireReason(reason, FlowRecommendationCommand.DISMISS);
        FlowRecommendationStatus from = status;
        status = FlowRecommendationStatus.DISMISSED;
        return record(
                FlowEventType.RECOMMENDATION_DISMISSED,
                from,
                FlowRecommendationStatus.DISMISSED,
                actor,
                commandId,
                correlationId,
                reason,
                Map.of(),
                now
        );
    }

    private FlowActivity publish(
            ActorIdentity actor,
            UUID commandId,
            String correlationId,
            String reason,
            String inboundGuestMessage,
            Instant now
    ) {
        if (status != FlowRecommendationStatus.APPROVED) {
            throw new InvalidFlowRecommendationTransitionException(status, FlowRecommendationCommand.PUBLISH);
        }
        String message = inboundGuestMessage == null || inboundGuestMessage.isBlank() ? guestMessage : inboundGuestMessage.trim();
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("PUBLISH requires a guestMessage.");
        }
        this.guestMessage = message;
        FlowRecommendationStatus from = status;
        status = FlowRecommendationStatus.PUBLISHED;
        return record(
                FlowEventType.RECOMMENDATION_PUBLISHED,
                from,
                FlowRecommendationStatus.PUBLISHED,
                actor,
                commandId,
                correlationId,
                reason,
                Map.of("guestMessage", message),
                now
        );
    }

    private FlowActivity withdraw(
            ActorIdentity actor,
            UUID commandId,
            String correlationId,
            String reason,
            Instant now
    ) {
        if (status != FlowRecommendationStatus.PUBLISHED) {
            throw new InvalidFlowRecommendationTransitionException(status, FlowRecommendationCommand.WITHDRAW);
        }
        requireReason(reason, FlowRecommendationCommand.WITHDRAW);
        FlowRecommendationStatus from = status;
        status = FlowRecommendationStatus.WITHDRAWN;
        return record(
                FlowEventType.RECOMMENDATION_WITHDRAWN,
                from,
                FlowRecommendationStatus.WITHDRAWN,
                actor,
                commandId,
                correlationId,
                reason,
                Map.of(),
                now
        );
    }

    private FlowActivity transition(
            FlowRecommendationStatus required,
            FlowRecommendationStatus next,
            FlowEventType eventType,
            FlowRecommendationCommand command,
            ActorIdentity actor,
            UUID commandId,
            String correlationId,
            String reason,
            Map<String, Object> details,
            Instant now
    ) {
        if (status != required) {
            throw new InvalidFlowRecommendationTransitionException(status, command);
        }
        status = next;
        return record(eventType, required, next, actor, commandId, correlationId, reason, details, now);
    }

    private FlowActivity record(
            FlowEventType eventType,
            FlowRecommendationStatus from,
            FlowRecommendationStatus to,
            ActorIdentity actor,
            UUID commandId,
            String correlationId,
            String reason,
            Map<String, Object> details,
            Instant now
    ) {
        version += 1;
        if (eventType == FlowEventType.RECOMMENDATION_CREATED) {
            version = 1L;
        }
        updatedAt = now;
        long sequence = history.size() + 1L;
        FlowActivity activity = new FlowActivity(
                UUID.randomUUID(),
                id,
                sequence,
                eventType,
                from,
                to,
                actor.subject(),
                actor.displayName(),
                actor.type(),
                reason,
                details,
                commandId,
                correlationId == null || correlationId.isBlank() ? commandId.toString() : correlationId,
                now,
                version
        );
        history.add(activity);
        return activity;
    }

    public FlowRecommendationId id() {
        return id;
    }

    public FlowRecommendationType type() {
        return type;
    }

    public FlowRecommendationStatus status() {
        return status;
    }

    public FlowRecommendationSeverity severity() {
        return severity;
    }

    public AttractionId sourceAttractionId() {
        return sourceAttractionId;
    }

    public List<AttractionId> affectedAttractionIds() {
        return List.copyOf(affectedAttractionIds);
    }

    public List<AttractionId> recommendedDestinationIds() {
        return List.copyOf(recommendedDestinationIds);
    }

    public String summary() {
        return summary;
    }

    public String explanation() {
        return explanation;
    }

    public String guestMessage() {
        return guestMessage;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public String relatedIncidentId() {
        return relatedIncidentId;
    }

    public String relatedWorkOrderId() {
        return relatedWorkOrderId;
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

    public boolean simulated() {
        return simulated;
    }

    public List<FlowActivity> activity() {
        return List.copyOf(history);
    }

    public List<FlowActivity> uncommittedActivity() {
        if (committedCount >= history.size()) {
            return List.of();
        }
        return List.copyOf(history.subList(committedCount, history.size()));
    }

    public void markActivityCommitted() {
        committedCount = history.size();
    }

    public int uncommittedCount() {
        return Math.max(0, history.size() - committedCount);
    }

    private static void requireReason(String reason, FlowRecommendationCommand command) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(command + " requires a reason.");
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static LinkedHashSet<AttractionId> unique(List<AttractionId> ids) {
        LinkedHashSet<AttractionId> unique = new LinkedHashSet<>();
        if (ids != null) {
            unique.addAll(ids);
        }
        return unique;
    }
}
