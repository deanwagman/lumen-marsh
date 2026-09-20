package com.deanwagman.lumenmarsh.venueops.flow.application;

import com.deanwagman.lumenmarsh.venueops.flow.domain.ForecastConfidence;
import com.deanwagman.lumenmarsh.venueops.flow.domain.QueueForecast;
import com.deanwagman.lumenmarsh.venueops.flow.domain.QueueFreshness;
import com.deanwagman.lumenmarsh.venueops.flow.domain.QueueProjection;
import com.deanwagman.lumenmarsh.venueops.flow.domain.QueueTrend;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendation;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationSeverity;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationStatus;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class FlowSnapshots {

    private FlowSnapshots() {
    }

    public record QueueProjectionSnapshot(
            String attractionId,
            UUID observationId,
            int queueLength,
            double arrivalsPerMinute,
            double throughputPerMinute,
            int calculatedWaitMinutes,
            int postedWaitMinutes,
            int operatingCapacityPercent,
            QueueTrend trend,
            QueueFreshness freshness,
            Instant observedAt,
            Instant receivedAt,
            Instant updatedAt,
            long version,
            boolean simulated
    ) {
        public static QueueProjectionSnapshot from(QueueProjection projection, Instant now) {
            return new QueueProjectionSnapshot(
                    projection.attractionId().value(),
                    projection.observationId(),
                    projection.queueLength(),
                    projection.arrivalsPerMinute(),
                    projection.throughputPerMinute(),
                    projection.calculatedWaitMinutes(),
                    projection.postedWaitMinutes(),
                    projection.operatingCapacityPercent(),
                    projection.trend(),
                    projection.freshness(now),
                    projection.observedAt(),
                    projection.receivedAt(),
                    projection.updatedAt(),
                    projection.version(),
                    projection.simulated()
            );
        }
    }

    public record QueueForecastSnapshot(
            UUID forecastId,
            String attractionId,
            Instant generatedAt,
            UUID basedOnObservationId,
            int horizonMinutes,
            int predictedQueueLength,
            int predictedWaitMinutes,
            ForecastConfidence confidence,
            List<String> assumptions,
            String explanation,
            boolean simulated
    ) {
        public static QueueForecastSnapshot from(QueueForecast forecast) {
            return new QueueForecastSnapshot(
                    forecast.forecastId(),
                    forecast.attractionId().value(),
                    forecast.generatedAt(),
                    forecast.basedOnObservationId(),
                    forecast.horizonMinutes(),
                    forecast.predictedQueueLength(),
                    forecast.predictedWaitMinutes(),
                    forecast.confidence(),
                    forecast.assumptions(),
                    forecast.explanation(),
                    forecast.simulated()
            );
        }
    }

    public record RecommendationSnapshot(
            String recommendationId,
            FlowRecommendationType type,
            FlowRecommendationStatus status,
            FlowRecommendationSeverity severity,
            String sourceAttractionId,
            List<String> affectedAttractionIds,
            List<String> recommendedDestinationIds,
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
        public static RecommendationSnapshot from(FlowRecommendation recommendation) {
            return new RecommendationSnapshot(
                    recommendation.id().value(),
                    recommendation.type(),
                    recommendation.status(),
                    recommendation.severity(),
                    recommendation.sourceAttractionId() == null ? null : recommendation.sourceAttractionId().value(),
                    recommendation.affectedAttractionIds().stream().map(id -> id.value()).toList(),
                    recommendation.recommendedDestinationIds().stream().map(id -> id.value()).toList(),
                    recommendation.summary(),
                    recommendation.explanation(),
                    recommendation.guestMessage(),
                    recommendation.expiresAt(),
                    recommendation.relatedIncidentId(),
                    recommendation.relatedWorkOrderId(),
                    recommendation.version(),
                    recommendation.createdAt(),
                    recommendation.updatedAt(),
                    recommendation.simulated()
            );
        }
    }

    public record GuestWaitSnapshot(
            String attractionId,
            String displayName,
            String originZoneId,
            String originZoneName,
            Integer postedWaitMinutes,
            String waitOutlook,
            String forecast30Minutes,
            String trend,
            String availability,
            QueueFreshness freshness,
            Instant updatedAt,
            boolean simulated
    ) {
    }

    public record GuestGuidanceSnapshot(
            String recommendationId,
            List<String> recommendedDestinationIds,
            String guestMessage,
            Instant updatedAt,
            boolean simulated
    ) {
        public static GuestGuidanceSnapshot from(FlowRecommendation recommendation) {
            return new GuestGuidanceSnapshot(
                    recommendation.id().value(),
                    recommendation.recommendedDestinationIds().stream().map(id -> id.value()).toList(),
                    recommendation.guestMessage(),
                    recommendation.updatedAt(),
                    recommendation.simulated()
            );
        }
    }
}
