package com.deanwagman.lumenmarsh.venueops.weather.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentId;
import com.deanwagman.lumenmarsh.venueops.weather.application.StaleWeatherRecommendationVersionException;
import com.deanwagman.lumenmarsh.venueops.weather.application.WeatherRecommendationRepository;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendation;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationActivity;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "venueops.attractions.persistence", havingValue = "jpa")
public class JpaWeatherRecommendationRepository implements WeatherRecommendationRepository {

    private final WeatherRecommendationJpaRepository recommendations;
    private final WeatherRecommendationAttractionJpaRepository links;
    private final WeatherRecommendationActivityJpaRepository activities;

    public JpaWeatherRecommendationRepository(
            WeatherRecommendationJpaRepository recommendations,
            WeatherRecommendationAttractionJpaRepository links,
            WeatherRecommendationActivityJpaRepository activities
    ) {
        this.recommendations = recommendations;
        this.links = links;
        this.activities = activities;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WeatherRecommendation> findById(WeatherRecommendationId id) {
        return recommendations.findById(id.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WeatherRecommendation> findAll() {
        return recommendations.findAll().stream()
                .sorted(Comparator.comparing(WeatherRecommendationEntity::getUpdatedAt).reversed())
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void save(WeatherRecommendation recommendation) {
        String id = recommendation.id().value();
        Optional<WeatherRecommendationEntity> existing = recommendations.findById(id);
        if (existing.isEmpty()) {
            recommendations.save(toEntity(recommendation));
            replaceLinks(recommendation);
            persistUncommitted(recommendation);
            return;
        }

        long expectedVersion = recommendation.version() - recommendation.uncommittedActivity().size();
        int updated = recommendations.updateOperationalState(
                id,
                recommendation.ruleId(),
                recommendation.status(),
                recommendation.operatorStatus(),
                recommendation.severity(),
                recommendation.summary(),
                recommendation.evidence(),
                recommendation.recommendedAction(),
                recommendation.observedAt(),
                recommendation.updatedAt(),
                recommendation.sourceVersion(),
                recommendation.version(),
                recommendation.linkedIncidentId() == null ? null : recommendation.linkedIncidentId().value(),
                expectedVersion
        );
        if (updated == 0) {
            throw new StaleWeatherRecommendationVersionException(
                    recommendation.id(),
                    expectedVersion,
                    existing.get().getVersion()
            );
        }
        replaceLinks(recommendation);
        persistUncommitted(recommendation);
    }

    private void replaceLinks(WeatherRecommendation recommendation) {
        String id = recommendation.id().value();
        links.deleteByRecommendationId(id);
        for (AttractionId attractionId : recommendation.affectedAttractionIds()) {
            links.save(new WeatherRecommendationAttractionEntity(id, attractionId.value()));
        }
    }

    private void persistUncommitted(WeatherRecommendation recommendation) {
        for (WeatherRecommendationActivity activity : recommendation.uncommittedActivity()) {
            activities.save(WeatherRecommendationActivityMapper.toEntity(activity));
        }
        recommendation.markActivityCommitted();
    }

    private WeatherRecommendation toDomain(WeatherRecommendationEntity entity) {
        List<AttractionId> attractionIds = links
                .findByRecommendationIdOrderByAttractionIdAsc(entity.getId())
                .stream()
                .map(link -> new AttractionId(link.getAttractionId()))
                .toList();
        List<WeatherRecommendationActivity> history = activities
                .findByRecommendationIdOrderByResultingVersionAsc(entity.getId())
                .stream()
                .map(WeatherRecommendationActivityMapper::toDomain)
                .toList();
        return WeatherRecommendation.rehydrate(
                new WeatherRecommendationId(entity.getId()),
                entity.getRuleId(),
                entity.getStatus(),
                entity.getOperatorStatus(),
                entity.getSeverity(),
                entity.getSummary(),
                entity.getEvidence(),
                entity.getRecommendedAction(),
                attractionIds,
                entity.getObservedAt(),
                entity.getReceivedAt(),
                entity.getUpdatedAt(),
                entity.getSourceVersion(),
                entity.getVersion(),
                entity.getLinkedIncidentId() == null ? null : new IncidentId(entity.getLinkedIncidentId()),
                history
        );
    }

    private static WeatherRecommendationEntity toEntity(WeatherRecommendation recommendation) {
        return new WeatherRecommendationEntity(
                recommendation.id().value(),
                recommendation.ruleId(),
                recommendation.status(),
                recommendation.operatorStatus(),
                recommendation.severity(),
                recommendation.summary(),
                recommendation.evidence(),
                recommendation.recommendedAction(),
                recommendation.observedAt(),
                recommendation.receivedAt(),
                recommendation.updatedAt(),
                recommendation.sourceVersion(),
                recommendation.version(),
                recommendation.linkedIncidentId() == null ? null : recommendation.linkedIncidentId().value()
        );
    }
}
