package com.deanwagman.lumenmarsh.venueops.flow.application;

import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionNotFoundException;
import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionRepository;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.Attraction;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.flow.domain.InvalidFlowObservationException;
import com.deanwagman.lumenmarsh.venueops.flow.domain.QueueObservation;
import com.deanwagman.lumenmarsh.venueops.flow.domain.QueueObservationSourceType;
import com.deanwagman.lumenmarsh.venueops.flow.domain.QueueProjection;
import com.deanwagman.lumenmarsh.venueops.flow.domain.QueueProjectionCalculator;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class FlowObservationService {

    private final FlowObservationRepository observations;
    private final QueueProjectionRepository projections;
    private final AttractionRepository attractions;
    private final FlowUpdatePublisher publisher;
    private final FlowMetrics metrics;
    private final Clock clock;

    public FlowObservationService(
            FlowObservationRepository observations,
            QueueProjectionRepository projections,
            AttractionRepository attractions,
            FlowUpdatePublisher publisher,
            FlowMetrics metrics,
            Clock clock
    ) {
        this.observations = Objects.requireNonNull(observations);
        this.projections = Objects.requireNonNull(projections);
        this.attractions = Objects.requireNonNull(attractions);
        this.publisher = Objects.requireNonNull(publisher);
        this.metrics = Objects.requireNonNull(metrics);
        this.clock = Objects.requireNonNull(clock);
    }

    @Transactional
    public FlowObservationIngestResult ingest(
            UUID observationId,
            AttractionId attractionId,
            Instant observedAt,
            int windowSeconds,
            int queueLength,
            int arrivals,
            int boarded,
            int operatingUnits,
            int configuredUnits,
            QueueObservationSourceType sourceType,
            boolean simulated
    ) {
        try {
            Attraction attraction = attractions.findById(attractionId)
                    .orElseThrow(() -> new AttractionNotFoundException(attractionId));
            var existing = observations.findById(observationId);
            if (existing.isPresent()) {
                metrics.received();
                metrics.replayed();
                long version = projections.findByAttractionId(attractionId).map(QueueProjection::version).orElse(0L);
                return FlowObservationIngestResult.replay(existing.get(), version);
            }
            QueueObservation observation = QueueObservation.accept(
                    observationId,
                    attractionId,
                    observedAt,
                    windowSeconds,
                    queueLength,
                    arrivals,
                    boarded,
                    operatingUnits,
                    configuredUnits,
                    sourceType,
                    simulated,
                    clock
            );
            observations.save(observation);
            QueueProjection current = projections.findByAttractionId(attractionId).orElse(null);
            List<QueueObservation> previous = observations.findByAttraction(attractionId, 4).stream()
                    .filter(item -> !item.observationId().equals(observationId))
                    .limit(3)
                    .toList();
            Instant now = Instant.now(clock);
            int posted = attraction.waitMinutes() == null ? 0 : attraction.waitMinutes();
            QueueProjection next = QueueProjectionCalculator.fromObservation(
                    observation,
                    previous,
                    current,
                    posted,
                    now
            );
            boolean applied = current == null || next.version() != current.version();
            if (applied) {
                projections.save(next);
                AfterCommit.run(() -> {
                    publisher.publish(new FlowOperationalUpdate(
                            observation.observationId().toString(),
                            FlowUpdateEventType.QUEUE_UPDATED,
                            now,
                            FlowSnapshots.QueueProjectionSnapshot.from(next, now)
                    ));
                    publisher.publish(new GuestFlowOperationalUpdate(
                            observation.observationId().toString(),
                            GuestFlowUpdateEventType.UPDATED,
                            now,
                            guestWait(attraction, next, now)
                    ));
                });
            }
            metrics.received();
            return FlowObservationIngestResult.accepted(observation, applied, next.version());
        } catch (InvalidFlowObservationException ex) {
            metrics.rejected();
            throw ex;
        }
    }

    private static FlowSnapshots.GuestWaitSnapshot guestWait(
            Attraction attraction,
            QueueProjection projection,
            Instant now
    ) {
        return new FlowSnapshots.GuestWaitSnapshot(
                attraction.id().value(),
                attraction.name(),
                com.deanwagman.lumenmarsh.venueops.flow.domain.FlowZones.zoneId(attraction.id().value()),
                com.deanwagman.lumenmarsh.venueops.flow.domain.FlowZones.zoneName(
                        com.deanwagman.lumenmarsh.venueops.flow.domain.FlowZones.zoneId(attraction.id().value())
                ),
                projection.postedWaitMinutes(),
                com.deanwagman.lumenmarsh.venueops.flow.domain.GuestWaitLanguage.aboutMinutes(projection.postedWaitMinutes()),
                null,
                com.deanwagman.lumenmarsh.venueops.flow.domain.GuestWaitLanguage.direction(projection.trend()),
                com.deanwagman.lumenmarsh.venueops.flow.domain.GuestWaitLanguage.availability(
                        attraction.status() == com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatus.OPERATING
                ),
                projection.freshness(now),
                projection.observedAt(),
                projection.simulated()
        );
    }
}
