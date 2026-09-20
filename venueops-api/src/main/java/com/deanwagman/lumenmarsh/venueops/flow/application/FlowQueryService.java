package com.deanwagman.lumenmarsh.venueops.flow.application;

import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionNotFoundException;
import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionRepository;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.Attraction;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatus;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendation;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationId;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationSeverity;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationStatus;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationType;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowZones;
import com.deanwagman.lumenmarsh.venueops.flow.domain.GuestWaitLanguage;
import com.deanwagman.lumenmarsh.venueops.flow.domain.QueueForecast;
import com.deanwagman.lumenmarsh.venueops.flow.domain.QueueFreshness;
import com.deanwagman.lumenmarsh.venueops.flow.domain.QueueObservation;
import com.deanwagman.lumenmarsh.venueops.flow.domain.QueueProjection;
import com.deanwagman.lumenmarsh.venueops.flow.domain.QueueTrend;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class FlowQueryService {

    private final AttractionRepository attractions;
    private final QueueProjectionRepository projections;
    private final QueueForecastRepository forecasts;
    private final FlowObservationRepository observations;
    private final FlowRecommendationService recommendations;
    private final Clock clock;

    public FlowQueryService(
            AttractionRepository attractions,
            QueueProjectionRepository projections,
            QueueForecastRepository forecasts,
            FlowObservationRepository observations,
            FlowRecommendationService recommendations,
            Clock clock
    ) {
        this.attractions = Objects.requireNonNull(attractions);
        this.projections = Objects.requireNonNull(projections);
        this.forecasts = Objects.requireNonNull(forecasts);
        this.observations = Objects.requireNonNull(observations);
        this.recommendations = Objects.requireNonNull(recommendations);
        this.clock = Objects.requireNonNull(clock);
    }

    public OperatorOverview overview() {
        recommendations.expireDue();
        Instant now = Instant.now(clock);
        List<Attraction> catalog = attractions.findAll();
        List<OperatorAttractionCard> cards = catalog.stream()
                .map(attraction -> card(attraction, now))
                .toList();
        int guests = cards.stream().mapToInt(card -> card.queueLength() == null ? 0 : card.queueLength()).sum();
        int rising = (int) cards.stream().filter(card -> card.trend() == QueueTrend.RISING).count();
        int stale = (int) cards.stream().filter(card -> card.freshness() == QueueFreshness.STALE).count();
        int pending = (int) recommendations.list().stream()
                .filter(recommendation -> recommendation.status() == FlowRecommendationStatus.PENDING_REVIEW)
                .count();
        int capacity = cards.isEmpty()
                ? 0
                : (int) Math.round(cards.stream()
                .mapToInt(card -> card.operatingCapacityPercent() == null ? 0 : card.operatingCapacityPercent())
                .average()
                .orElse(0));
        Instant lastSync = cards.stream()
                .map(OperatorAttractionCard::observedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        boolean simulated = cards.stream().anyMatch(OperatorAttractionCard::simulated)
                || catalog.isEmpty();
        return new OperatorOverview(guests, rising, stale, pending, capacity, lastSync, cards, simulated);
    }

    public OperatorAttractionDetail attraction(AttractionId attractionId) {
        Attraction attraction = attractions.findById(attractionId)
                .orElseThrow(() -> new AttractionNotFoundException(attractionId));
        Instant now = Instant.now(clock);
        QueueProjection projection = projections.findByAttractionId(attractionId).orElse(null);
        List<QueueForecast> latest = forecasts.findLatestByAttraction(attractionId);
        List<QueueObservation> history = observations.findByAttraction(attractionId, 50);
        return new OperatorAttractionDetail(
                attraction.id().value(),
                attraction.name(),
                attraction.status().name(),
                attraction.waitMinutes(),
                projection == null ? null : FlowSnapshots.QueueProjectionSnapshot.from(projection, now),
                latest.stream().map(FlowSnapshots.QueueForecastSnapshot::from).toList(),
                history,
                projection != null && projection.simulated()
        );
    }

    public PageResult<FlowRecommendation> recommendations(
            FlowRecommendationStatus status,
            FlowRecommendationSeverity severity,
            FlowRecommendationType type,
            AttractionId attractionId,
            int page,
            int size
    ) {
        recommendations.expireDue();
        int safePage = Math.max(page, 0);
        int safeSize = size < 1 ? 20 : Math.min(size, 100);
        List<FlowRecommendation> items = recommendations.list().stream()
                .filter(recommendation -> status == null || recommendation.status() == status)
                .filter(recommendation -> severity == null || recommendation.severity() == severity)
                .filter(recommendation -> type == null || recommendation.type() == type)
                .filter(recommendation -> attractionId == null
                        || Objects.equals(recommendation.sourceAttractionId(), attractionId)
                        || recommendation.affectedAttractionIds().contains(attractionId)
                        || recommendation.recommendedDestinationIds().contains(attractionId))
                .sorted(Comparator.comparing(FlowRecommendation::updatedAt).reversed())
                .toList();
        int from = Math.min(safePage * safeSize, items.size());
        int to = Math.min(from + safeSize, items.size());
        return new PageResult<>(items.subList(from, to), safePage, safeSize, items.size());
    }

    public FlowRecommendation recommendation(FlowRecommendationId id) {
        return recommendations.get(id);
    }

    public GuestOverview guestOverview() {
        Instant now = Instant.now(clock);
        List<FlowSnapshots.GuestWaitSnapshot> waits = attractions.findAll().stream()
                .map(attraction -> guestWait(attraction, now))
                .toList();
        List<FlowSnapshots.GuestGuidanceSnapshot> guidance = recommendations.list().stream()
                .filter(recommendation -> recommendation.status() == FlowRecommendationStatus.PUBLISHED)
                .map(FlowSnapshots.GuestGuidanceSnapshot::from)
                .toList();
        Instant updatedAt = waits.stream()
                .map(FlowSnapshots.GuestWaitSnapshot::updatedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(now);
        boolean simulated = waits.stream().anyMatch(FlowSnapshots.GuestWaitSnapshot::simulated)
                || guidance.stream().anyMatch(FlowSnapshots.GuestGuidanceSnapshot::simulated);
        return new GuestOverview(waits, guidance, updatedAt, simulated);
    }

    public List<FlowSnapshots.GuestGuidanceSnapshot> guestRecommendations() {
        return recommendations.list().stream()
                .filter(recommendation -> recommendation.status() == FlowRecommendationStatus.PUBLISHED)
                .map(FlowSnapshots.GuestGuidanceSnapshot::from)
                .toList();
    }

    public FlowSnapshots.GuestWaitSnapshot guestWaitForecast(AttractionId attractionId) {
        Attraction attraction = attractions.findById(attractionId)
                .orElseThrow(() -> new AttractionNotFoundException(attractionId));
        return guestWait(attraction, Instant.now(clock));
    }

    public Object operatorSnapshot() {
        return overview();
    }

    public Object guestSnapshot() {
        return guestOverview();
    }

    private OperatorAttractionCard card(Attraction attraction, Instant now) {
        QueueProjection projection = projections.findByAttractionId(attraction.id()).orElse(null);
        List<QueueForecast> latest = forecasts.findLatestByAttraction(attraction.id());
        return new OperatorAttractionCard(
                attraction.id().value(),
                attraction.name(),
                attraction.status().name(),
                attraction.waitMinutes(),
                projection == null ? null : projection.calculatedWaitMinutes(),
                forecastMinutes(latest, 15),
                forecastMinutes(latest, 30),
                forecastMinutes(latest, 60),
                projection == null ? null : projection.queueLength(),
                projection == null ? null : projection.arrivalsPerMinute(),
                projection == null ? null : projection.throughputPerMinute(),
                projection == null ? null : projection.operatingCapacityPercent(),
                projection == null ? null : projection.trend(),
                projection == null ? QueueFreshness.STALE : projection.freshness(now),
                projection == null ? null : projection.observedAt(),
                projection != null && projection.simulated()
        );
    }

    private FlowSnapshots.GuestWaitSnapshot guestWait(Attraction attraction, Instant now) {
        QueueProjection projection = projections.findByAttractionId(attraction.id()).orElse(null);
        Optional<QueueForecast> thirty = forecasts.findLatestByAttraction(attraction.id()).stream()
                .filter(forecast -> forecast.horizonMinutes() == 30)
                .findFirst();
        QueueFreshness freshness = projection == null ? QueueFreshness.STALE : projection.freshness(now);
        Integer posted = attraction.waitMinutes();
        String outlook = posted == null ? null : GuestWaitLanguage.aboutMinutes(posted);
        String forecast30 = thirty
                .filter(item -> freshness != QueueFreshness.STALE)
                .map(item -> GuestWaitLanguage.aboutMinutes(item.predictedWaitMinutes()))
                .orElse(null);
        QueueTrend trend = projection == null ? QueueTrend.STABLE : projection.trend();
        return new FlowSnapshots.GuestWaitSnapshot(
                attraction.id().value(),
                attraction.name(),
                FlowZones.zoneId(attraction.id().value()),
                FlowZones.zoneName(FlowZones.zoneId(attraction.id().value())),
                posted,
                outlook,
                forecast30,
                GuestWaitLanguage.direction(trend),
                GuestWaitLanguage.availability(attraction.status() == AttractionStatus.OPERATING),
                freshness,
                projection == null ? attraction.updatedAt() : projection.observedAt(),
                projection != null && projection.simulated()
        );
    }

    private static Integer forecastMinutes(List<QueueForecast> forecasts, int horizon) {
        return forecasts.stream()
                .filter(forecast -> forecast.horizonMinutes() == horizon)
                .map(QueueForecast::predictedWaitMinutes)
                .findFirst()
                .orElse(null);
    }

    public record OperatorOverview(
            int guestsInQueues,
            int risingWaitCount,
            int staleAttractionCount,
            int pendingRecommendationCount,
            int parkCapacityPercent,
            Instant lastSynchronizedAt,
            List<OperatorAttractionCard> attractions,
            boolean simulated
    ) {
    }

    public record OperatorAttractionCard(
            String attractionId,
            String displayName,
            String status,
            Integer postedWaitMinutes,
            Integer calculatedWaitMinutes,
            Integer predictedWaitMinutes15,
            Integer predictedWaitMinutes30,
            Integer predictedWaitMinutes60,
            Integer queueLength,
            Double arrivalsPerMinute,
            Double throughputPerMinute,
            Integer operatingCapacityPercent,
            QueueTrend trend,
            QueueFreshness freshness,
            Instant observedAt,
            boolean simulated
    ) {
    }

    public record OperatorAttractionDetail(
            String attractionId,
            String displayName,
            String status,
            Integer postedWaitMinutes,
            FlowSnapshots.QueueProjectionSnapshot projection,
            List<FlowSnapshots.QueueForecastSnapshot> forecasts,
            List<QueueObservation> recentObservations,
            boolean simulated
    ) {
    }

    public record GuestOverview(
            List<FlowSnapshots.GuestWaitSnapshot> attractions,
            List<FlowSnapshots.GuestGuidanceSnapshot> publishedGuidance,
            Instant updatedAt,
            boolean simulated
    ) {
    }
}
