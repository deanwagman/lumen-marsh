package com.deanwagman.lumenmarsh.venueops.flow.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.flow.domain.ForecastConfidence;
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
@Table(name = "queue_forecast")
public class QueueForecastEntity {

    @Id
    @Column(name = "forecast_id", length = 36)
    private String forecastId;

    @Column(name = "attraction_id", nullable = false, length = 64)
    private String attractionId;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "based_on_observation_id", nullable = false, length = 36)
    private String basedOnObservationId;

    @Column(name = "horizon_minutes", nullable = false)
    private int horizonMinutes;

    @Column(name = "predicted_queue_length", nullable = false)
    private int predictedQueueLength;

    @Column(name = "predicted_wait_minutes", nullable = false)
    private int predictedWaitMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ForecastConfidence confidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String assumptions;

    @Column(nullable = false, columnDefinition = "text")
    private String explanation;

    @Column(nullable = false)
    private boolean simulated;

    protected QueueForecastEntity() {
    }

    public QueueForecastEntity(
            String forecastId,
            String attractionId,
            Instant generatedAt,
            String basedOnObservationId,
            int horizonMinutes,
            int predictedQueueLength,
            int predictedWaitMinutes,
            ForecastConfidence confidence,
            String assumptions,
            String explanation,
            boolean simulated
    ) {
        this.forecastId = forecastId;
        this.attractionId = attractionId;
        this.generatedAt = generatedAt;
        this.basedOnObservationId = basedOnObservationId;
        this.horizonMinutes = horizonMinutes;
        this.predictedQueueLength = predictedQueueLength;
        this.predictedWaitMinutes = predictedWaitMinutes;
        this.confidence = confidence;
        this.assumptions = assumptions;
        this.explanation = explanation;
        this.simulated = simulated;
    }

    public String getForecastId() {
        return forecastId;
    }

    public String getAttractionId() {
        return attractionId;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public String getBasedOnObservationId() {
        return basedOnObservationId;
    }

    public int getHorizonMinutes() {
        return horizonMinutes;
    }

    public int getPredictedQueueLength() {
        return predictedQueueLength;
    }

    public int getPredictedWaitMinutes() {
        return predictedWaitMinutes;
    }

    public ForecastConfidence getConfidence() {
        return confidence;
    }

    public String getAssumptions() {
        return assumptions;
    }

    public String getExplanation() {
        return explanation;
    }

    public boolean isSimulated() {
        return simulated;
    }
}
