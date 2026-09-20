package com.deanwagman.lumenmarsh.venueops.flow.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.flow.application.FlowRecommendationRepository;
import com.deanwagman.lumenmarsh.venueops.flow.application.StaleFlowRecommendationVersionException;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowActivity;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendation;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationId;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationSeverity;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationStatus;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

interface FlowRecommendationJpaRepository extends JpaRepository<FlowRecommendationEntity, String> {
    @Modifying
    @Query("""
            update FlowRecommendationEntity r
            set r.status = :status,
                r.guestMessage = :guestMessage,
                r.version = :version,
                r.updatedAt = :updatedAt
            where r.recommendationId = :id and r.version = :expectedVersion
            """)
    int updateState(
            @Param("id") String id,
            @Param("status") FlowRecommendationStatus status,
            @Param("guestMessage") String guestMessage,
            @Param("version") long version,
            @Param("updatedAt") java.time.Instant updatedAt,
            @Param("expectedVersion") long expectedVersion
    );
}

interface FlowRecommendationDestinationJpaRepository
        extends JpaRepository<FlowRecommendationDestinationEntity, FlowRecommendationDestinationEntity.FlowRecommendationDestinationId> {
    List<FlowRecommendationDestinationEntity> findByIdRecommendationId(String recommendationId);

    void deleteByIdRecommendationId(String recommendationId);
}

interface FlowActivityJpaRepository extends JpaRepository<FlowActivityEntity, String> {
    List<FlowActivityEntity> findByRecommendationIdOrderBySequenceAsc(String recommendationId);
}

@Repository
@ConditionalOnProperty(name = "venueops.attractions.persistence", havingValue = "jpa")
public class JpaFlowRecommendationRepository implements FlowRecommendationRepository {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final FlowRecommendationJpaRepository recommendations;
    private final FlowRecommendationDestinationJpaRepository destinations;
    private final FlowActivityJpaRepository activities;

    public JpaFlowRecommendationRepository(
            FlowRecommendationJpaRepository recommendations,
            FlowRecommendationDestinationJpaRepository destinations,
            FlowActivityJpaRepository activities
    ) {
        this.recommendations = recommendations;
        this.destinations = destinations;
        this.activities = activities;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FlowRecommendation> findById(FlowRecommendationId id) {
        return recommendations.findById(id.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlowRecommendation> findAll() {
        return recommendations.findAll().stream()
                .map(this::toDomain)
                .sorted(Comparator.comparing(FlowRecommendation::updatedAt).reversed())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlowRecommendation> find(
            FlowRecommendationStatus status,
            FlowRecommendationSeverity severity,
            FlowRecommendationType type,
            AttractionId attractionId
    ) {
        return findAll().stream()
                .filter(recommendation -> status == null || recommendation.status() == status)
                .filter(recommendation -> severity == null || recommendation.severity() == severity)
                .filter(recommendation -> type == null || recommendation.type() == type)
                .filter(recommendation -> attractionId == null
                        || Objects.equals(recommendation.sourceAttractionId(), attractionId)
                        || recommendation.affectedAttractionIds().contains(attractionId)
                        || recommendation.recommendedDestinationIds().contains(attractionId))
                .toList();
    }

    @Override
    @Transactional
    public void save(FlowRecommendation recommendation) {
        String id = recommendation.id().value();
        Optional<FlowRecommendationEntity> existing = recommendations.findById(id);
        if (existing.isEmpty()) {
            recommendations.save(toEntity(recommendation));
            replaceDestinations(recommendation);
            persistUncommitted(recommendation);
            recommendation.markActivityCommitted();
            return;
        }
        long expectedVersion = recommendation.version() - recommendation.uncommittedCount();
        int updated = recommendations.updateState(
                id,
                recommendation.status(),
                recommendation.guestMessage(),
                recommendation.version(),
                recommendation.updatedAt(),
                expectedVersion
        );
        if (updated == 0) {
            throw new StaleFlowRecommendationVersionException(
                    recommendation.id(),
                    expectedVersion,
                    existing.get().getVersion()
            );
        }
        replaceDestinations(recommendation);
        persistUncommitted(recommendation);
        recommendation.markActivityCommitted();
    }

    private void replaceDestinations(FlowRecommendation recommendation) {
        String id = recommendation.id().value();
        destinations.deleteByIdRecommendationId(id);
        for (AttractionId attractionId : recommendation.affectedAttractionIds()) {
            destinations.save(new FlowRecommendationDestinationEntity(id, attractionId.value(), "AFFECTED"));
        }
        for (AttractionId attractionId : recommendation.recommendedDestinationIds()) {
            destinations.save(new FlowRecommendationDestinationEntity(id, attractionId.value(), "RECOMMENDED"));
        }
    }

    private void persistUncommitted(FlowRecommendation recommendation) {
        for (FlowActivity activity : recommendation.uncommittedActivity()) {
            activities.save(new FlowActivityEntity(
                    activity.id().toString(),
                    activity.recommendationId().value(),
                    activity.sequence(),
                    activity.eventType(),
                    activity.fromStatus(),
                    activity.toStatus(),
                    activity.actorSubject(),
                    activity.actorDisplayName(),
                    activity.actorType(),
                    activity.reason(),
                    JSON.writeValueAsString(activity.details()),
                    activity.commandId().toString(),
                    activity.correlationId(),
                    activity.occurredAt(),
                    activity.resultingVersion()
            ));
        }
    }

    private FlowRecommendation toDomain(FlowRecommendationEntity entity) {
        List<FlowRecommendationDestinationEntity> links = destinations.findByIdRecommendationId(entity.getRecommendationId());
        List<AttractionId> affected = links.stream()
                .filter(link -> "AFFECTED".equals(link.getId().getKind()))
                .map(link -> new AttractionId(link.getId().getAttractionId()))
                .toList();
        List<AttractionId> recommended = links.stream()
                .filter(link -> "RECOMMENDED".equals(link.getId().getKind()))
                .map(link -> new AttractionId(link.getId().getAttractionId()))
                .toList();
        List<FlowActivity> history = activities.findByRecommendationIdOrderBySequenceAsc(entity.getRecommendationId())
                .stream()
                .map(this::toActivity)
                .toList();
        return FlowRecommendation.rehydrate(
                new FlowRecommendationId(entity.getRecommendationId()),
                entity.getType(),
                entity.getStatus(),
                entity.getSeverity(),
                entity.getSourceAttractionId() == null ? null : new AttractionId(entity.getSourceAttractionId()),
                affected,
                recommended,
                entity.getSummary(),
                entity.getExplanation(),
                entity.getGuestMessage(),
                entity.getExpiresAt(),
                entity.getRelatedIncidentId(),
                entity.getRelatedWorkOrderId(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.isSimulated(),
                history
        );
    }

    private FlowActivity toActivity(FlowActivityEntity entity) {
        return new FlowActivity(
                UUID.fromString(entity.getId()),
                new FlowRecommendationId(entity.getRecommendationId()),
                entity.getSequence(),
                entity.getEventType(),
                entity.getFromStatus(),
                entity.getToStatus(),
                entity.getActorSubject(),
                entity.getActorDisplayName(),
                entity.getActorType(),
                entity.getReason(),
                Map.of(),
                UUID.fromString(entity.getCommandId()),
                entity.getCorrelationId(),
                entity.getOccurredAt(),
                entity.getResultingVersion()
        );
    }

    private static FlowRecommendationEntity toEntity(FlowRecommendation recommendation) {
        return new FlowRecommendationEntity(
                recommendation.id().value(),
                recommendation.type(),
                recommendation.status(),
                recommendation.severity(),
                recommendation.sourceAttractionId() == null ? null : recommendation.sourceAttractionId().value(),
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
