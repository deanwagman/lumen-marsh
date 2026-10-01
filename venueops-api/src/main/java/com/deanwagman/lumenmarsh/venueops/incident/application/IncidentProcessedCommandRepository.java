package com.deanwagman.lumenmarsh.venueops.incident.application;

import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentEventType;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentId;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IncidentProcessedCommandRepository {

    Optional<ProcessedCommand> findByCommandId(UUID commandId);

    void save(ProcessedCommand command);

    record ProcessedCommand(
            UUID commandId,
            IncidentId incidentId,
            IncidentEventType eventType,
            long resultingVersion,
            Instant occurredAt,
            String resultJson
    ) {
    }
}
