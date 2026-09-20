package com.deanwagman.lumenmarsh.venueops.flow.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowEventType;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationStatus;
import com.deanwagman.lumenmarsh.venueops.security.ActorType;
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
@Table(name = "flow_activity")
public class FlowActivityEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "recommendation_id", nullable = false, length = 36)
    private String recommendationId;

    @Column(nullable = false)
    private long sequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private FlowEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 32)
    private FlowRecommendationStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 32)
    private FlowRecommendationStatus toStatus;

    @Column(name = "actor_subject", nullable = false)
    private String actorSubject;

    @Column(name = "actor_display_name", nullable = false)
    private String actorDisplayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 32)
    private ActorType actorType;

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

    protected FlowActivityEntity() {
    }

    public FlowActivityEntity(
            String id,
            String recommendationId,
            long sequence,
            FlowEventType eventType,
            FlowRecommendationStatus fromStatus,
            FlowRecommendationStatus toStatus,
            String actorSubject,
            String actorDisplayName,
            ActorType actorType,
            String reason,
            String details,
            String commandId,
            String correlationId,
            Instant occurredAt,
            long resultingVersion
    ) {
        this.id = id;
        this.recommendationId = recommendationId;
        this.sequence = sequence;
        this.eventType = eventType;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actorSubject = actorSubject;
        this.actorDisplayName = actorDisplayName;
        this.actorType = actorType;
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

    public String getRecommendationId() {
        return recommendationId;
    }

    public long getSequence() {
        return sequence;
    }

    public FlowEventType getEventType() {
        return eventType;
    }

    public FlowRecommendationStatus getFromStatus() {
        return fromStatus;
    }

    public FlowRecommendationStatus getToStatus() {
        return toStatus;
    }

    public String getActorSubject() {
        return actorSubject;
    }

    public String getActorDisplayName() {
        return actorDisplayName;
    }

    public ActorType getActorType() {
        return actorType;
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
