package com.deanwagman.lumenmarsh.venueops.attraction.application;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionEventType;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AttractionProcessedCommandRepository {

    Optional<ProcessedCommand> findByCommandId(UUID commandId);

    void save(ProcessedCommand command);

    record ProcessedCommand(
            UUID commandId,
            AttractionId attractionId,
            AttractionEventType eventType,
            long resultingVersion,
            Instant occurredAt,
            String resultJson
    ) {
    }
}
