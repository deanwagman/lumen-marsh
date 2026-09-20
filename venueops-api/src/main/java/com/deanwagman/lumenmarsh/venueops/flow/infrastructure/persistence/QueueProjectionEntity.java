package com.deanwagman.lumenmarsh.venueops.flow.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.flow.domain.QueueTrend;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "queue_projection")
public class QueueProjectionEntity {

    @Id
    @Column(name = "attraction_id", length = 64)
    private String attractionId;

    @Column(name = "observation_id", nullable = false, length = 36)
    private String observationId;

    @Column(name = "queue_length", nullable = false)
    private int queueLength;

    @Column(name = "arrivals_per_minute", nullable = false)
    private double arrivalsPerMinute;

    @Column(name = "throughput_per_minute", nullable = false)
    private double throughputPerMinute;

    @Column(name = "calculated_wait_minutes", nullable = false)
    private int calculatedWaitMinutes;

    @Column(name = "posted_wait_minutes", nullable = false)
    private int postedWaitMinutes;

    @Column(name = "operating_capacity_percent", nullable = false)
    private int operatingCapacityPercent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private QueueTrend trend;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private long version;

    @Column(nullable = false)
    private boolean simulated;

    protected QueueProjectionEntity() {
    }

    public QueueProjectionEntity(
            String attractionId,
            String observationId,
            int queueLength,
            double arrivalsPerMinute,
            double throughputPerMinute,
            int calculatedWaitMinutes,
            int postedWaitMinutes,
            int operatingCapacityPercent,
            QueueTrend trend,
            Instant observedAt,
            Instant receivedAt,
            Instant updatedAt,
            long version,
            boolean simulated
    ) {
        this.attractionId = attractionId;
        this.observationId = observationId;
        this.queueLength = queueLength;
        this.arrivalsPerMinute = arrivalsPerMinute;
        this.throughputPerMinute = throughputPerMinute;
        this.calculatedWaitMinutes = calculatedWaitMinutes;
        this.postedWaitMinutes = postedWaitMinutes;
        this.operatingCapacityPercent = operatingCapacityPercent;
        this.trend = trend;
        this.observedAt = observedAt;
        this.receivedAt = receivedAt;
        this.updatedAt = updatedAt;
        this.version = version;
        this.simulated = simulated;
    }

    public String getAttractionId() {
        return attractionId;
    }

    public String getObservationId() {
        return observationId;
    }

    public int getQueueLength() {
        return queueLength;
    }

    public double getArrivalsPerMinute() {
        return arrivalsPerMinute;
    }

    public double getThroughputPerMinute() {
        return throughputPerMinute;
    }

    public int getCalculatedWaitMinutes() {
        return calculatedWaitMinutes;
    }

    public int getPostedWaitMinutes() {
        return postedWaitMinutes;
    }

    public int getOperatingCapacityPercent() {
        return operatingCapacityPercent;
    }

    public QueueTrend getTrend() {
        return trend;
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

    public long getVersion() {
        return version;
    }

    public boolean isSimulated() {
        return simulated;
    }
}
