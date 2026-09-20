package com.deanwagman.lumenmarsh.venueops.flow.infrastructure;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.flow.application.FlowRecommendationRepository;
import com.deanwagman.lumenmarsh.venueops.flow.application.StaleFlowRecommendationVersionException;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendation;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationId;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationSeverity;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationStatus;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "venueops.attractions.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryFlowRecommendationRepository implements FlowRecommendationRepository {

    private final Map<String, FlowRecommendation> recommendations = new ConcurrentHashMap<>();

    @Override
    public Optional<FlowRecommendation> findById(FlowRecommendationId id) {
        return Optional.ofNullable(recommendations.get(id.value())).map(InMemoryFlowRecommendationRepository::copyOf);
    }

    @Override
    public List<FlowRecommendation> findAll() {
        return recommendations.values().stream()
                .map(InMemoryFlowRecommendationRepository::copyOf)
                .sorted(Comparator.comparing(FlowRecommendation::updatedAt).reversed())
                .toList();
    }

    @Override
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
    public synchronized void save(FlowRecommendation recommendation) {
        FlowRecommendation existing = recommendations.get(recommendation.id().value());
        if (existing != null) {
            long expectedVersion = recommendation.version() - recommendation.uncommittedCount();
            if (existing.version() != expectedVersion) {
                throw new StaleFlowRecommendationVersionException(
                        recommendation.id(),
                        expectedVersion,
                        existing.version()
                );
            }
        }
        recommendations.put(recommendation.id().value(), copyOf(recommendation));
        recommendation.markActivityCommitted();
    }

    private static FlowRecommendation copyOf(FlowRecommendation recommendation) {
        return FlowRecommendation.rehydrate(
                recommendation.id(),
                recommendation.type(),
                recommendation.status(),
                recommendation.severity(),
                recommendation.sourceAttractionId(),
                recommendation.affectedAttractionIds(),
                recommendation.recommendedDestinationIds(),
                recommendation.summary(),
                recommendation.explanation(),
                recommendation.guestMessage(),
                recommendation.expiresAt(),
                recommendation.relatedIncidentId(),
                recommendation.relatedWorkOrderId(),
                recommendation.version(),
                recommendation.createdAt(),
                recommendation.updatedAt(),
                recommendation.simulated(),
                recommendation.activity()
        );
    }
}
