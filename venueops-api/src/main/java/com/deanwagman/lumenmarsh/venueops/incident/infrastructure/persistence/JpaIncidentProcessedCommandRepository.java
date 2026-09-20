package com.deanwagman.lumenmarsh.venueops.incident.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentProcessedCommandRepository;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentEventType;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "incident_processed_commands")
class IncidentProcessedCommandEntity {

    @Id
    @Column(name = "command_id", length = 36)
    private String commandId;

    @Column(name = "incident_id", nullable = false, length = 36)
    private String incidentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private IncidentEventType eventType;

    @Column(name = "resulting_version", nullable = false)
    private long resultingVersion;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_json", nullable = false, columnDefinition = "jsonb")
    private String resultJson;

    protected IncidentProcessedCommandEntity() {
    }

    IncidentProcessedCommandEntity(
            String commandId,
            String incidentId,
            IncidentEventType eventType,
            long resultingVersion,
            Instant occurredAt,
            String resultJson
    ) {
        this.commandId = commandId;
        this.incidentId = incidentId;
        this.eventType = eventType;
        this.resultingVersion = resultingVersion;
        this.occurredAt = occurredAt;
        this.resultJson = resultJson;
    }

    String getCommandId() {
        return commandId;
    }

    String getIncidentId() {
        return incidentId;
    }

    IncidentEventType getEventType() {
        return eventType;
    }

    long getResultingVersion() {
        return resultingVersion;
    }

    Instant getOccurredAt() {
        return occurredAt;
    }

    String getResultJson() {
        return resultJson;
    }
}

interface IncidentProcessedCommandJpaRepository extends JpaRepository<IncidentProcessedCommandEntity, String> {
}

@Repository
@ConditionalOnProperty(name = "venueops.attractions.persistence", havingValue = "jpa")
public class JpaIncidentProcessedCommandRepository implements IncidentProcessedCommandRepository {

    private final IncidentProcessedCommandJpaRepository commands;

    public JpaIncidentProcessedCommandRepository(IncidentProcessedCommandJpaRepository commands) {
        this.commands = commands;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProcessedCommand> findByCommandId(UUID commandId) {
        return commands.findById(commandId.toString()).map(entity -> new ProcessedCommand(
                UUID.fromString(entity.getCommandId()),
                new IncidentId(entity.getIncidentId()),
                entity.getEventType(),
                entity.getResultingVersion(),
                entity.getOccurredAt(),
                entity.getResultJson()
        ));
    }

    @Override
    @Transactional
    public void save(ProcessedCommand command) {
        commands.save(new IncidentProcessedCommandEntity(
                command.commandId().toString(),
                command.incidentId().value(),
                command.eventType(),
                command.resultingVersion(),
                command.occurredAt(),
                command.resultJson()
        ));
    }
}
