package com.deanwagman.lumenmarsh.venueops.flow.application;

import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowEventType;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationId;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface FlowProcessedCommandRepository {

    Optional<ProcessedCommand> findByCommandId(UUID commandId);

    void save(ProcessedCommand command);

    record ProcessedCommand(
            UUID commandId,
            FlowRecommendationId recommendationId,
            FlowEventType eventType,
            long resultingVersion,
            Instant occurredAt,
            String resultJson
    ) {
    }
}
