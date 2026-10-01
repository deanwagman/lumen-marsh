package com.deanwagman.lumenmarsh.venueops.attraction.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionProcessedCommandRepository;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionEventType;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
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
@Table(name = "attraction_processed_commands")
class AttractionProcessedCommandEntity {

    @Id
    @Column(name = "command_id", length = 36)
    private String commandId;

    @Column(name = "attraction_id", nullable = false, length = 64)
    private String attractionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private AttractionEventType eventType;

    @Column(name = "resulting_version", nullable = false)
    private long resultingVersion;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_json", nullable = false, columnDefinition = "jsonb")
    private String resultJson;

    protected AttractionProcessedCommandEntity() {
    }

    AttractionProcessedCommandEntity(
            String commandId,
            String attractionId,
            AttractionEventType eventType,
            long resultingVersion,
            Instant occurredAt,
            String resultJson
    ) {
        this.commandId = commandId;
        this.attractionId = attractionId;
        this.eventType = eventType;
        this.resultingVersion = resultingVersion;
        this.occurredAt = occurredAt;
        this.resultJson = resultJson;
    }

    String getCommandId() {
        return commandId;
    }

    String getAttractionId() {
        return attractionId;
    }

    AttractionEventType getEventType() {
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

interface AttractionProcessedCommandJpaRepository extends JpaRepository<AttractionProcessedCommandEntity, String> {
}

@Repository
@ConditionalOnProperty(name = "venueops.attractions.persistence", havingValue = "jpa")
public class JpaAttractionProcessedCommandRepository implements AttractionProcessedCommandRepository {

    private final AttractionProcessedCommandJpaRepository commands;

    public JpaAttractionProcessedCommandRepository(AttractionProcessedCommandJpaRepository commands) {
        this.commands = commands;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProcessedCommand> findByCommandId(UUID commandId) {
        return commands.findById(commandId.toString()).map(entity -> new ProcessedCommand(
                UUID.fromString(entity.getCommandId()),
                new AttractionId(entity.getAttractionId()),
                entity.getEventType(),
                entity.getResultingVersion(),
                entity.getOccurredAt(),
                entity.getResultJson()
        ));
    }

    @Override
    @Transactional
    public void save(ProcessedCommand command) {
        commands.save(new AttractionProcessedCommandEntity(
                command.commandId().toString(),
                command.attractionId().value(),
                command.eventType(),
                command.resultingVersion(),
                command.occurredAt(),
                command.resultJson()
        ));
    }
}
