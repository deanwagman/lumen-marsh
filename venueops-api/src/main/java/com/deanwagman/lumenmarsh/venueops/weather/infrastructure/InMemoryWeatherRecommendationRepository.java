package com.deanwagman.lumenmarsh.venueops.weather.infrastructure;

import com.deanwagman.lumenmarsh.venueops.weather.application.StaleWeatherRecommendationVersionException;
import com.deanwagman.lumenmarsh.venueops.weather.application.WeatherRecommendationRepository;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendation;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "venueops.attractions.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryWeatherRecommendationRepository implements WeatherRecommendationRepository {

    private final Map<WeatherRecommendationId, WeatherRecommendation> recommendations = new ConcurrentHashMap<>();

    @Override
    public Optional<WeatherRecommendation> findById(WeatherRecommendationId id) {
        return Optional.ofNullable(recommendations.get(id)).map(InMemoryWeatherRecommendationRepository::copyOf);
    }

    @Override
    public List<WeatherRecommendation> findAll() {
        return recommendations.values().stream()
                .map(InMemoryWeatherRecommendationRepository::copyOf)
                .sorted(Comparator.comparing(WeatherRecommendation::updatedAt).reversed())
                .toList();
    }

    @Override
    public synchronized void save(WeatherRecommendation recommendation) {
        WeatherRecommendation existing = recommendations.get(recommendation.id());
        if (existing != null) {
            long expectedVersion = recommendation.version() - recommendation.uncommittedActivity().size();
            if (existing.version() != expectedVersion) {
                throw new StaleWeatherRecommendationVersionException(
                        recommendation.id(),
                        expectedVersion,
                        existing.version()
                );
            }
        }
        recommendations.put(recommendation.id(), copyOf(recommendation));
        recommendation.markActivityCommitted();
    }

    private static WeatherRecommendation copyOf(WeatherRecommendation recommendation) {
        return WeatherRecommendation.rehydrate(
                recommendation.id(),
                recommendation.ruleId(),
                recommendation.status(),
                recommendation.operatorStatus(),
                recommendation.severity(),
                recommendation.summary(),
                recommendation.evidence(),
                recommendation.recommendedAction(),
                recommendation.affectedAttractionIds(),
                recommendation.observedAt(),
                recommendation.receivedAt(),
                recommendation.updatedAt(),
                recommendation.sourceVersion(),
                recommendation.version(),
                recommendation.linkedIncidentId(),
                recommendation.activity()
        );
    }
}
