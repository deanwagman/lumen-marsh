package com.deanwagman.lumenmarsh.venueops.flow.domain;

import com.deanwagman.lumenmarsh.venueops.security.ActorType;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record FlowActivity(
        UUID id,
        FlowRecommendationId recommendationId,
        long sequence,
        FlowEventType eventType,
        FlowRecommendationStatus fromStatus,
        FlowRecommendationStatus toStatus,
        String actorSubject,
        String actorDisplayName,
        ActorType actorType,
        String reason,
        Map<String, Object> details,
        UUID commandId,
        String correlationId,
        Instant occurredAt,
        long resultingVersion
) {
    public FlowActivity {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(recommendationId, "recommendationId is required");
        Objects.requireNonNull(eventType, "eventType is required");
        Objects.requireNonNull(toStatus, "toStatus is required");
        Objects.requireNonNull(actorSubject, "actorSubject is required");
        Objects.requireNonNull(actorDisplayName, "actorDisplayName is required");
        Objects.requireNonNull(actorType, "actorType is required");
        Objects.requireNonNull(details, "details are required");
        Objects.requireNonNull(commandId, "commandId is required");
        Objects.requireNonNull(correlationId, "correlationId is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
        details = Map.copyOf(details);
    }
}
