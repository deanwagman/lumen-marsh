package com.deanwagman.lumenmarsh.venueops.flow.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.flow.application.FlowProcessedCommandRepository;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowEventType;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationId;
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
@Table(name = "flow_processed_commands")
class FlowProcessedCommandEntity {

    @Id
    @Column(name = "command_id", length = 36)
    private String commandId;

    @Column(name = "recommendation_id", nullable = false, length = 36)
    private String recommendationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private FlowEventType eventType;

    @Column(name = "resulting_version", nullable = false)
    private long resultingVersion;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_json", nullable = false, columnDefinition = "jsonb")
    private String resultJson;

    protected FlowProcessedCommandEntity() {
    }

    FlowProcessedCommandEntity(
            String commandId,
            String recommendationId,
            FlowEventType eventType,
            long resultingVersion,
            Instant occurredAt,
            String resultJson
    ) {
        this.commandId = commandId;
        this.recommendationId = recommendationId;
        this.eventType = eventType;
        this.resultingVersion = resultingVersion;
        this.occurredAt = occurredAt;
        this.resultJson = resultJson;
    }

    String getCommandId() {
        return commandId;
    }

    String getRecommendationId() {
        return recommendationId;
    }

    FlowEventType getEventType() {
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

interface FlowProcessedCommandJpaRepository extends JpaRepository<FlowProcessedCommandEntity, String> {
}

@Repository
@ConditionalOnProperty(name = "venueops.attractions.persistence", havingValue = "jpa")
public class JpaFlowProcessedCommandRepository implements FlowProcessedCommandRepository {

    private final FlowProcessedCommandJpaRepository commands;

    public JpaFlowProcessedCommandRepository(FlowProcessedCommandJpaRepository commands) {
        this.commands = commands;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProcessedCommand> findByCommandId(UUID commandId) {
        return commands.findById(commandId.toString()).map(entity -> new ProcessedCommand(
                UUID.fromString(entity.getCommandId()),
                new FlowRecommendationId(entity.getRecommendationId()),
                entity.getEventType(),
                entity.getResultingVersion(),
                entity.getOccurredAt(),
                entity.getResultJson()
        ));
    }

    @Override
    @Transactional
    public void save(ProcessedCommand command) {
        commands.save(new FlowProcessedCommandEntity(
                command.commandId().toString(),
                command.recommendationId().value(),
                command.eventType(),
                command.resultingVersion(),
                command.occurredAt(),
                command.resultJson()
        ));
    }
}
