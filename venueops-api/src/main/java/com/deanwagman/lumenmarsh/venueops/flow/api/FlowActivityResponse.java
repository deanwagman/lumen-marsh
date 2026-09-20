package com.deanwagman.lumenmarsh.venueops.flow.api;

import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowActivity;
import com.deanwagman.lumenmarsh.venueops.security.ActorType;

import java.time.Instant;
import java.util.UUID;

public record FlowActivityResponse(
        UUID id,
        long sequence,
        String eventType,
        String fromStatus,
        String toStatus,
        String actor,
        ActorType actorType,
        String reason,
        UUID commandId,
        String correlationId,
        Instant occurredAt,
        long resultingVersion
) {
    public static FlowActivityResponse from(FlowActivity activity) {
        return new FlowActivityResponse(
                activity.id(),
                activity.sequence(),
                activity.eventType().name(),
                activity.fromStatus() == null ? null : activity.fromStatus().name(),
                activity.toStatus().name(),
                activity.actorDisplayName(),
                activity.actorType(),
                activity.reason(),
                activity.commandId(),
                activity.correlationId(),
                activity.occurredAt(),
                activity.resultingVersion()
        );
    }
}
