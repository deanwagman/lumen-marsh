package com.deanwagman.lumenmarsh.venueops.weather.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationOperatorStatus;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationSeverity;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "weather_recommendation")
public class WeatherRecommendationEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "rule_id", nullable = false, length = 128)
    private String ruleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private WeatherRecommendationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator_status", nullable = false, length = 32)
    private WeatherRecommendationOperatorStatus operatorStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private WeatherRecommendationSeverity severity;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @Column(nullable = false, columnDefinition = "text")
    private String evidence;

    @Column(name = "recommended_action", nullable = false, columnDefinition = "text")
    private String recommendedAction;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "source_version", nullable = false)
    private long sourceVersion;

    @Column(nullable = false)
    private long version;

    @Column(name = "linked_incident_id", length = 36)
    private String linkedIncidentId;

    protected WeatherRecommendationEntity() {
    }

    public WeatherRecommendationEntity(
            String id,
            String ruleId,
            WeatherRecommendationStatus status,
            WeatherRecommendationOperatorStatus operatorStatus,
            WeatherRecommendationSeverity severity,
            String summary,
            String evidence,
            String recommendedAction,
            Instant observedAt,
            Instant receivedAt,
            Instant updatedAt,
            long sourceVersion,
            long version,
            String linkedIncidentId
    ) {
        this.id = id;
        this.ruleId = ruleId;
        this.status = status;
        this.operatorStatus = operatorStatus;
        this.severity = severity;
        this.summary = summary;
        this.evidence = evidence;
        this.recommendedAction = recommendedAction;
        this.observedAt = observedAt;
        this.receivedAt = receivedAt;
        this.updatedAt = updatedAt;
        this.sourceVersion = sourceVersion;
        this.version = version;
        this.linkedIncidentId = linkedIncidentId;
    }

    public String getId() {
        return id;
    }

    public String getRuleId() {
        return ruleId;
    }

    public WeatherRecommendationStatus getStatus() {
        return status;
    }

    public WeatherRecommendationOperatorStatus getOperatorStatus() {
        return operatorStatus;
    }

    public WeatherRecommendationSeverity getSeverity() {
        return severity;
    }

    public String getSummary() {
        return summary;
    }

    public String getEvidence() {
        return evidence;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public Instant getObservedAt() {
        return observedAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getSourceVersion() {
        return sourceVersion;
    }

    public long getVersion() {
        return version;
    }

    public String getLinkedIncidentId() {
        return linkedIncidentId;
    }
}
