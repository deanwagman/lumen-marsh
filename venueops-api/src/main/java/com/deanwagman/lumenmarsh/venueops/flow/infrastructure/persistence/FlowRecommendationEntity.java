package com.deanwagman.lumenmarsh.venueops.flow.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationSeverity;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationStatus;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "flow_recommendation")
public class FlowRecommendationEntity {

    @Id
    @Column(name = "recommendation_id", length = 36)
    private String recommendationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FlowRecommendationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FlowRecommendationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FlowRecommendationSeverity severity;

    @Column(name = "source_attraction_id", length = 64)
    private String sourceAttractionId;

    @Column(nullable = false, length = 512)
    private String summary;

    @Column(nullable = false, columnDefinition = "text")
    private String explanation;

    @Column(name = "guest_message", columnDefinition = "text")
    private String guestMessage;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "related_incident_id", length = 36)
    private String relatedIncidentId;

    @Column(name = "related_work_order_id", length = 36)
    private String relatedWorkOrderId;

    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private boolean simulated;

    protected FlowRecommendationEntity() {
    }

    public FlowRecommendationEntity(
            String recommendationId,
            FlowRecommendationType type,
            FlowRecommendationStatus status,
            FlowRecommendationSeverity severity,
            String sourceAttractionId,
            String summary,
            String explanation,
            String guestMessage,
            Instant expiresAt,
            String relatedIncidentId,
            String relatedWorkOrderId,
            long version,
            Instant createdAt,
            Instant updatedAt,
            boolean simulated
    ) {
        this.recommendationId = recommendationId;
        this.type = type;
        this.status = status;
        this.severity = severity;
        this.sourceAttractionId = sourceAttractionId;
        this.summary = summary;
        this.explanation = explanation;
        this.guestMessage = guestMessage;
        this.expiresAt = expiresAt;
        this.relatedIncidentId = relatedIncidentId;
        this.relatedWorkOrderId = relatedWorkOrderId;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.simulated = simulated;
    }

    public String getRecommendationId() {
        return recommendationId;
    }

    public FlowRecommendationType getType() {
        return type;
    }

    public FlowRecommendationStatus getStatus() {
        return status;
    }

    public FlowRecommendationSeverity getSeverity() {
        return severity;
    }

    public String getSourceAttractionId() {
        return sourceAttractionId;
    }

    public String getSummary() {
        return summary;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getGuestMessage() {
        return guestMessage;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getRelatedIncidentId() {
        return relatedIncidentId;
    }

    public String getRelatedWorkOrderId() {
        return relatedWorkOrderId;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isSimulated() {
        return simulated;
    }
}
