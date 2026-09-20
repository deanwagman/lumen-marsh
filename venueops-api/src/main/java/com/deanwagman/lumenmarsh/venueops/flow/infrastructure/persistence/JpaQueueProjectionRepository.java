package com.deanwagman.lumenmarsh.venueops.flow.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.flow.application.QueueProjectionRepository;
import com.deanwagman.lumenmarsh.venueops.flow.domain.QueueProjection;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface QueueProjectionJpaRepository extends JpaRepository<QueueProjectionEntity, String> {
}

@Repository
@ConditionalOnProperty(name = "venueops.attractions.persistence", havingValue = "jpa")
public class JpaQueueProjectionRepository implements QueueProjectionRepository {

    private final QueueProjectionJpaRepository projections;

    public JpaQueueProjectionRepository(QueueProjectionJpaRepository projections) {
        this.projections = projections;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QueueProjection> findByAttractionId(AttractionId attractionId) {
        return projections.findById(attractionId.value()).map(JpaQueueProjectionRepository::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QueueProjection> findAll() {
        return projections.findAll().stream().map(JpaQueueProjectionRepository::toDomain).toList();
    }

    @Override
    @Transactional
    public void save(QueueProjection projection) {
        projections.save(new QueueProjectionEntity(
                projection.attractionId().value(),
                projection.observationId().toString(),
                projection.queueLength(),
                projection.arrivalsPerMinute(),
                projection.throughputPerMinute(),
                projection.calculatedWaitMinutes(),
                projection.postedWaitMinutes(),
                projection.operatingCapacityPercent(),
                projection.trend(),
                projection.observedAt(),
                projection.receivedAt(),
                projection.updatedAt(),
                projection.version(),
                projection.simulated()
        ));
    }

    private static QueueProjection toDomain(QueueProjectionEntity entity) {
        return new QueueProjection(
                new AttractionId(entity.getAttractionId()),
                UUID.fromString(entity.getObservationId()),
                entity.getQueueLength(),
                entity.getArrivalsPerMinute(),
                entity.getThroughputPerMinute(),
                entity.getCalculatedWaitMinutes(),
                entity.getPostedWaitMinutes(),
                entity.getOperatingCapacityPercent(),
                entity.getTrend(),
                entity.getObservedAt(),
                entity.getReceivedAt(),
                entity.getUpdatedAt(),
                entity.getVersion(),
                entity.isSimulated()
        );
    }
}
