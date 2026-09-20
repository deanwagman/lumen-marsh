package com.deanwagman.lumenmarsh.venueops.flow.application;

import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionNotFoundException;
import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionRepository;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.flow.domain.ForecastConfidence;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendation;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationId;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationSeverity;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationType;
import com.deanwagman.lumenmarsh.venueops.flow.domain.QueueForecast;
import com.deanwagman.lumenmarsh.venueops.flow.domain.QueueFreshness;
import com.deanwagman.lumenmarsh.venueops.flow.domain.QueueProjection;
import com.deanwagman.lumenmarsh.venueops.security.ActorIdentity;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class FlowForecastService {

    public record HorizonInbound(
            UUID forecastId,
            int horizonMinutes,
            int predictedQueueLength,
            int predictedWaitMinutes,
            ForecastConfidence confidence,
            List<String> assumptions,
            String explanation
    ) {
    }

    public record RecommendationProposal(
            UUID recommendationId,
            FlowRecommendationType type,
            FlowRecommendationSeverity severity,
            String sourceAttractionId,
            List<String> affectedAttractionIds,
            List<String> recommendedDestinationIds,
            String summary,
            String explanation,
            String guestMessage,
            Instant expiresAt,
            String relatedIncidentId,
            String relatedWorkOrderId
    ) {
    }

    public record ForecastIngestResult(
            boolean replay,
            List<QueueForecast> forecasts,
            FlowRecommendation recommendation,
            boolean recommendationAccepted,
            String recommendationSkipReason
    ) {
    }

    private final QueueForecastRepository forecasts;
    private final FlowObservationRepository observations;
    private final QueueProjectionRepository projections;
    private final AttractionRepository attractions;
    private final FlowRecommendationRepository recommendations;
    private final FlowUpdatePublisher publisher;
    private final FlowRelatedOperations relatedOperations;
    private final Clock clock;

    public FlowForecastService(
            QueueForecastRepository forecasts,
            FlowObservationRepository observations,
            QueueProjectionRepository projections,
            AttractionRepository attractions,
            FlowRecommendationRepository recommendations,
            FlowUpdatePublisher publisher,
            FlowRelatedOperations relatedOperations,
            Clock clock
    ) {
        this.forecasts = Objects.requireNonNull(forecasts);
        this.observations = Objects.requireNonNull(observations);
        this.projections = Objects.requireNonNull(projections);
        this.attractions = Objects.requireNonNull(attractions);
        this.recommendations = Objects.requireNonNull(recommendations);
        this.publisher = Objects.requireNonNull(publisher);
        this.relatedOperations = Objects.requireNonNull(relatedOperations);
        this.clock = Objects.requireNonNull(clock);
    }

    @Transactional
    public ForecastIngestResult ingest(
            AttractionId attractionId,
            Instant generatedAt,
            UUID basedOnObservationId,
            boolean simulated,
            List<HorizonInbound> horizons,
            RecommendationProposal proposal,
            ActorIdentity actor,
            String correlationId
    ) {
        if (attractions.findById(attractionId).isEmpty()) {
            throw new AttractionNotFoundException(attractionId);
        }
        if (observations.findById(basedOnObservationId).isEmpty()) {
            throw new FlowObservationNotFoundException(basedOnObservationId);
        }
        if (horizons == null || horizons.size() != 3) {
            throw new IllegalArgumentException("Forecast ingest requires 15-, 30-, and 60-minute horizons.");
        }
        Set<Integer> horizonMinutes = new HashSet<>();
        for (HorizonInbound horizon : horizons) {
            horizonMinutes.add(horizon.horizonMinutes());
        }
        if (!horizonMinutes.containsAll(QueueForecast.REQUIRED_HORIZONS) || horizonMinutes.size() != 3) {
            throw new IllegalArgumentException("Forecast ingest requires 15-, 30-, and 60-minute horizons.");
        }
        boolean replay = horizons.stream().allMatch(horizon -> forecasts.findById(horizon.forecastId()).isPresent());
        List<QueueForecast> stored = horizons.stream()
                .map(horizon -> forecasts.findById(horizon.forecastId()).orElseGet(() -> {
                    QueueForecast forecast = new QueueForecast(
                            horizon.forecastId(),
                            attractionId,
                            generatedAt,
                            basedOnObservationId,
                            horizon.horizonMinutes(),
                            horizon.predictedQueueLength(),
                            horizon.predictedWaitMinutes(),
                            horizon.confidence(),
                            horizon.assumptions() == null ? List.of() : horizon.assumptions(),
                            horizon.explanation(),
                            simulated
                    );
                    forecasts.save(forecast);
                    return forecast;
                }))
                .toList();
        Instant now = Instant.now(clock);
        AfterCommit.run(() -> publisher.publish(new FlowOperationalUpdate(
                stored.getFirst().forecastId().toString(),
                FlowUpdateEventType.FORECAST_UPDATED,
                now,
                stored.stream().map(FlowSnapshots.QueueForecastSnapshot::from).toList()
        )));
        if (proposal == null) {
            return new ForecastIngestResult(replay, stored, null, false, null);
        }
        FlowRecommendationId recommendationId = FlowRecommendationId.of(proposal.recommendationId());
        var existing = recommendations.findById(recommendationId);
        if (existing.isPresent()) {
            return new ForecastIngestResult(replay, stored, existing.get(), false, "REPLAY");
        }
        QueueProjection projection = projections.findByAttractionId(attractionId).orElse(null);
        if (projection == null || projection.freshness(now) == QueueFreshness.STALE) {
            return new ForecastIngestResult(replay, stored, null, false, "STALE_DATA");
        }
        List<AttractionId> affected = ids(proposal.affectedAttractionIds());
        List<AttractionId> destinations = ids(proposal.recommendedDestinationIds());
        AttractionId source = proposal.sourceAttractionId() == null
                ? attractionId
                : new AttractionId(proposal.sourceAttractionId());
        requireKnown(source);
        affected.forEach(this::requireKnown);
        destinations.forEach(this::requireKnown);
        FlowRelatedOperations.Links links = relatedOperations.linksFor(source);
        String relatedIncidentId = proposal.relatedIncidentId() == null
                ? links.incidentId()
                : proposal.relatedIncidentId();
        String relatedWorkOrderId = proposal.relatedWorkOrderId() == null
                ? links.workOrderId()
                : proposal.relatedWorkOrderId();
        Instant expiresAt = proposal.expiresAt() == null ? now.plusSeconds(7200) : proposal.expiresAt();
        FlowRecommendation recommendation = FlowRecommendation.create(
                recommendationId,
                proposal.type(),
                proposal.severity(),
                source,
                affected,
                destinations,
                proposal.summary(),
                proposal.explanation(),
                proposal.guestMessage(),
                expiresAt,
                relatedIncidentId,
                relatedWorkOrderId,
                simulated,
                actor,
                proposal.recommendationId(),
                correlationId,
                clock
        );
        recommendations.save(recommendation);
        AfterCommit.run(() -> publisher.publish(new FlowOperationalUpdate(
                recommendation.id().value(),
                FlowUpdateEventType.RECOMMENDATION_CREATED,
                recommendation.updatedAt(),
                FlowSnapshots.RecommendationSnapshot.from(recommendation)
        )));
        return new ForecastIngestResult(replay, stored, recommendation, true, null);
    }

    private void requireKnown(AttractionId attractionId) {
        if (attractions.findById(attractionId).isEmpty()) {
            throw new AttractionNotFoundException(attractionId);
        }
    }

    private static List<AttractionId> ids(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().map(AttractionId::new).toList();
    }
}
