package com.deanwagman.lumenmarsh.venueops.maintenance.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.maintenance.application.ProcessedCommandRepository;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.event.MaintenanceEventType;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrderId;
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
@Table(name = "maintenance_processed_commands")
class MaintenanceProcessedCommandEntity {

    @Id
    @Column(name = "command_id", length = 36)
    private String commandId;

    @Column(name = "work_order_id", nullable = false, length = 36)
    private String workOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private MaintenanceEventType eventType;

    @Column(name = "resulting_version", nullable = false)
    private long resultingVersion;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_json", nullable = false, columnDefinition = "jsonb")
    private String resultJson;

    protected MaintenanceProcessedCommandEntity() {
    }

    MaintenanceProcessedCommandEntity(
            String commandId,
            String workOrderId,
            MaintenanceEventType eventType,
            long resultingVersion,
            Instant occurredAt,
            String resultJson
    ) {
        this.commandId = commandId;
        this.workOrderId = workOrderId;
        this.eventType = eventType;
        this.resultingVersion = resultingVersion;
        this.occurredAt = occurredAt;
        this.resultJson = resultJson;
    }

    String getCommandId() {
        return commandId;
    }

    String getWorkOrderId() {
        return workOrderId;
    }

    MaintenanceEventType getEventType() {
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

interface MaintenanceProcessedCommandJpaRepository extends JpaRepository<MaintenanceProcessedCommandEntity, String> {
}

@Repository
@ConditionalOnProperty(name = "venueops.attractions.persistence", havingValue = "jpa")
public class JpaProcessedCommandRepository implements ProcessedCommandRepository {

    private final MaintenanceProcessedCommandJpaRepository commands;

    public JpaProcessedCommandRepository(MaintenanceProcessedCommandJpaRepository commands) {
        this.commands = commands;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProcessedCommand> findByCommandId(UUID commandId) {
        return commands.findById(commandId.toString()).map(entity -> new ProcessedCommand(
                UUID.fromString(entity.getCommandId()),
                MaintenanceWorkOrderId.of(entity.getWorkOrderId()),
                entity.getEventType(),
                entity.getResultingVersion(),
                entity.getOccurredAt(),
                entity.getResultJson()
        ));
    }

    @Override
    @Transactional
    public void save(ProcessedCommand command) {
        commands.save(new MaintenanceProcessedCommandEntity(
                command.commandId().toString(),
                command.workOrderId().toString(),
                command.eventType(),
                command.resultingVersion(),
                command.occurredAt(),
                command.resultJson()
        ));
    }
}
