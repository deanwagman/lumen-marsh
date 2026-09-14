package com.deanwagman.lumenmarsh.venueops.attraction.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionEventType;
import com.deanwagman.lumenmarsh.venueops.security.ActorAuditContext;
import com.deanwagman.lumenmarsh.venueops.security.ActorIdentity;
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
@Table(name = "attraction_activity")
public class AttractionActivityEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "attraction_id", nullable = false, length = 64)
    private String attractionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private AttractionEventType type;

    @Column(nullable = false)
    private String actor;

    @Column(name = "actor_subject", nullable = false)
    private String actorSubject;

    @Column(name = "actor_display_name", nullable = false)
    private String actorDisplayName;

    @Column(name = "actor_type", nullable = false, length = 32)
    private String actorType;

    @Column(name = "actor_issuer", nullable = false)
    private String actorIssuer;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "previous_version", nullable = false)
    private long previousVersion;

    @Column(name = "resulting_version", nullable = false)
    private long resultingVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    protected AttractionActivityEntity() {
    }

    public AttractionActivityEntity(
            String id,
            String attractionId,
            AttractionEventType type,
            String actor,
            String reason,
            Instant occurredAt,
            long previousVersion,
            long resultingVersion,
            String payload
    ) {
        ActorIdentity identity = ActorAuditContext.currentOrLegacy(actor);
        this.id = id;
        this.attractionId = attractionId;
        this.type = type;
        this.actor = identity.auditLabel();
        this.actorSubject = identity.subject();
        this.actorDisplayName = identity.displayName();
        this.actorType = identity.type().name();
        this.actorIssuer = identity.issuer();
        this.reason = reason;
        this.occurredAt = occurredAt;
        this.previousVersion = previousVersion;
        this.resultingVersion = resultingVersion;
        this.payload = payload;
    }

    public String getId() {
        return id;
    }

    public String getAttractionId() {
        return attractionId;
    }

    public AttractionEventType getType() {
        return type;
    }

    public String getActor() {
        return actor;
    }

    public String getActorSubject() {
        return actorSubject;
    }

    public String getActorDisplayName() {
        return actorDisplayName;
    }

    public ActorType getActorTypeEnum() {
        return ActorType.valueOf(actorType);
    }

    public String getActorIssuer() {
        return actorIssuer;
    }

    public String getReason() {
        return reason;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public long getPreviousVersion() {
        return previousVersion;
    }

    public long getResultingVersion() {
        return resultingVersion;
    }

    public String getPayload() {
        return payload;
    }
}
