package com.deanwagman.lumenmarsh.venueops.flow.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.flow.application.FlowObservationRepository;
import com.deanwagman.lumenmarsh.venueops.flow.domain.QueueObservation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface FlowObservationJpaRepository extends JpaRepository<FlowObservationEntity, String> {
    List<FlowObservationEntity> findByAttractionIdOrderByObservedAtDescReceivedAtDesc(String attractionId);
}

@Repository
@ConditionalOnProperty(name = "venueops.attractions.persistence", havingValue = "jpa")
public class JpaFlowObservationRepository implements FlowObservationRepository {

    private final FlowObservationJpaRepository observations;

    public JpaFlowObservationRepository(FlowObservationJpaRepository observations) {
        this.observations = observations;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QueueObservation> findById(UUID observationId) {
        return observations.findById(observationId.toString()).map(JpaFlowObservationRepository::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QueueObservation> findByAttraction(AttractionId attractionId, int limit) {
        return observations.findByAttractionIdOrderByObservedAtDescReceivedAtDesc(attractionId.value()).stream()
                .limit(Math.max(limit, 0))
                .map(JpaFlowObservationRepository::toDomain)
                .sorted(Comparator.comparing(QueueObservation::observedAt).reversed())
                .toList();
    }

    @Override
    @Transactional
    public void save(QueueObservation observation) {
        observations.save(new FlowObservationEntity(
                observation.observationId().toString(),
                observation.attractionId().value(),
                observation.observedAt(),
                observation.receivedAt(),
                observation.windowSeconds(),
                observation.queueLength(),
                observation.arrivals(),
                observation.boarded(),
                observation.operatingUnits(),
                observation.configuredUnits(),
                observation.sourceType(),
                observation.simulated()
        ));
    }

    private static QueueObservation toDomain(FlowObservationEntity entity) {
        return new QueueObservation(
                UUID.fromString(entity.getObservationId()),
                new AttractionId(entity.getAttractionId()),
                entity.getObservedAt(),
                entity.getReceivedAt(),
                entity.getWindowSeconds(),
                entity.getQueueLength(),
                entity.getArrivals(),
                entity.getBoarded(),
                entity.getOperatingUnits(),
                entity.getConfiguredUnits(),
                entity.getSourceType(),
                entity.isSimulated()
        );
    }
}
