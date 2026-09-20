package com.deanwagman.lumenmarsh.venueops.flow.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.flow.domain.QueueObservationSourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "flow_observation")
public class FlowObservationEntity {

    @Id
    @Column(name = "observation_id", length = 36)
    private String observationId;

    @Column(name = "attraction_id", nullable = false, length = 64)
    private String attractionId;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "window_seconds", nullable = false)
    private int windowSeconds;

    @Column(name = "queue_length", nullable = false)
    private int queueLength;

    @Column(nullable = false)
    private int arrivals;

    @Column(nullable = false)
    private int boarded;

    @Column(name = "operating_units", nullable = false)
    private int operatingUnits;

    @Column(name = "configured_units", nullable = false)
    private int configuredUnits;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 32)
    private QueueObservationSourceType sourceType;

    @Column(nullable = false)
    private boolean simulated;

    protected FlowObservationEntity() {
    }

    public FlowObservationEntity(
            String observationId,
            String attractionId,
            Instant observedAt,
            Instant receivedAt,
            int windowSeconds,
            int queueLength,
            int arrivals,
            int boarded,
            int operatingUnits,
            int configuredUnits,
            QueueObservationSourceType sourceType,
            boolean simulated
    ) {
        this.observationId = observationId;
        this.attractionId = attractionId;
        this.observedAt = observedAt;
        this.receivedAt = receivedAt;
        this.windowSeconds = windowSeconds;
        this.queueLength = queueLength;
        this.arrivals = arrivals;
        this.boarded = boarded;
        this.operatingUnits = operatingUnits;
        this.configuredUnits = configuredUnits;
        this.sourceType = sourceType;
        this.simulated = simulated;
    }

    public String getObservationId() {
        return observationId;
    }

    public String getAttractionId() {
        return attractionId;
    }

    public Instant getObservedAt() {
        return observedAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }

    public int getQueueLength() {
        return queueLength;
    }

    public int getArrivals() {
        return arrivals;
    }

    public int getBoarded() {
        return boarded;
    }

    public int getOperatingUnits() {
        return operatingUnits;
    }

    public int getConfiguredUnits() {
        return configuredUnits;
    }

    public QueueObservationSourceType getSourceType() {
        return sourceType;
    }

    public boolean isSimulated() {
        return simulated;
    }
}
