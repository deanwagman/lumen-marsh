package com.deanwagman.lumenmarsh.venueops.maintenance.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.event.MaintenanceEventType;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "maintenance_activity")
public class MaintenanceActivityEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "work_order_id", nullable = false, length = 36)
    private String workOrderId;

    @Column(nullable = false)
    private long sequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private MaintenanceEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 32)
    private MaintenanceWorkOrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 32)
    private MaintenanceWorkOrderStatus toStatus;

    @Column(name = "actor_subject", nullable = false)
    private String actorSubject;

    @Column(name = "actor_display_name", nullable = false)
    private String actorDisplayName;

    @Column(columnDefinition = "text")
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String details;

    @Column(name = "command_id", nullable = false, length = 36)
    private String commandId;

    @Column(name = "correlation_id", nullable = false, length = 128)
    private String correlationId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "resulting_version", nullable = false)
    private long resultingVersion;

    protected MaintenanceActivityEntity() {
    }

    public MaintenanceActivityEntity(
            String id,
            String workOrderId,
            long sequence,
            MaintenanceEventType eventType,
            MaintenanceWorkOrderStatus fromStatus,
            MaintenanceWorkOrderStatus toStatus,
            String actorSubject,
            String actorDisplayName,
            String reason,
            String details,
            String commandId,
            String correlationId,
            Instant occurredAt,
            long resultingVersion
    ) {
        this.id = id;
        this.workOrderId = workOrderId;
        this.sequence = sequence;
        this.eventType = eventType;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actorSubject = actorSubject;
        this.actorDisplayName = actorDisplayName;
        this.reason = reason;
        this.details = details;
        this.commandId = commandId;
        this.correlationId = correlationId;
        this.occurredAt = occurredAt;
        this.resultingVersion = resultingVersion;
    }

    public String getId() {
        return id;
    }

    public String getWorkOrderId() {
        return workOrderId;
    }

    public long getSequence() {
        return sequence;
    }

    public MaintenanceEventType getEventType() {
        return eventType;
    }

    public MaintenanceWorkOrderStatus getFromStatus() {
        return fromStatus;
    }

    public MaintenanceWorkOrderStatus getToStatus() {
        return toStatus;
    }

    public String getActorSubject() {
        return actorSubject;
    }

    public String getActorDisplayName() {
        return actorDisplayName;
    }

    public String getReason() {
        return reason;
    }

    public String getDetails() {
        return details;
    }

    public String getCommandId() {
        return commandId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public long getResultingVersion() {
        return resultingVersion;
    }
}
