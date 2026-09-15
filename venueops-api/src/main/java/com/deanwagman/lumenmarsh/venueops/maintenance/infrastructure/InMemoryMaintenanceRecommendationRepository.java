package com.deanwagman.lumenmarsh.venueops.maintenance.infrastructure;

import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceRecommendationRepository;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.StaleMaintenanceRecommendationVersionException;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceRecommendation;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceRecommendationId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "venueops.attractions.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryMaintenanceRecommendationRepository implements MaintenanceRecommendationRepository {

    private final Map<MaintenanceRecommendationId, MaintenanceRecommendation> recommendations = new ConcurrentHashMap<>();

    @Override
    public Optional<MaintenanceRecommendation> findById(MaintenanceRecommendationId id) {
        return Optional.ofNullable(recommendations.get(id)).map(InMemoryMaintenanceRecommendationRepository::copyOf);
    }

    @Override
    public Optional<MaintenanceRecommendation> findByObservationId(String observationId) {
        return recommendations.values().stream()
                .filter(recommendation -> recommendation.observationId().equals(observationId))
                .findFirst()
                .map(InMemoryMaintenanceRecommendationRepository::copyOf);
    }

    @Override
    public Optional<MaintenanceRecommendation> findByCommandId(UUID commandId) {
        return recommendations.values().stream()
                .filter(recommendation -> commandId.equals(recommendation.lastCommandId()))
                .findFirst()
                .map(InMemoryMaintenanceRecommendationRepository::copyOf);
    }

    @Override
    public List<MaintenanceRecommendation> findAll() {
        return recommendations.values().stream()
                .map(InMemoryMaintenanceRecommendationRepository::copyOf)
                .sorted(Comparator.comparing(MaintenanceRecommendation::observedAt).reversed())
                .toList();
    }

    @Override
    public synchronized void save(MaintenanceRecommendation recommendation) {
        MaintenanceRecommendation existing = recommendations.get(recommendation.id());
        if (existing != null) {
            long expectedVersion = recommendation.version() - recommendation.uncommittedBumps();
            if (existing.version() != expectedVersion) {
                throw new StaleMaintenanceRecommendationVersionException(
                        recommendation.id(),
                        expectedVersion,
                        existing.version()
                );
            }
        }
        recommendations.put(recommendation.id(), copyOf(recommendation));
        recommendation.markVersionCommitted();
    }

    private static MaintenanceRecommendation copyOf(MaintenanceRecommendation recommendation) {
        return new MaintenanceRecommendation(
                recommendation.id(),
                recommendation.observationId(),
                recommendation.observedAt(),
                recommendation.assetCode(),
                recommendation.assetId(),
                recommendation.signalType(),
                recommendation.severity(),
                recommendation.value(),
                recommendation.unit(),
                recommendation.evidence(),
                recommendation.recommendedAction(),
                recommendation.status(),
                recommendation.workOrderId(),
                recommendation.receivedAt(),
                recommendation.updatedAt(),
                recommendation.version(),
                recommendation.lastCommandId()
        );
    }
}
