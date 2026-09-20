package com.deanwagman.lumenmarsh.venueops.flow.infrastructure;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.flow.application.QueueForecastRepository;
import com.deanwagman.lumenmarsh.venueops.flow.domain.QueueForecast;
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
public class InMemoryQueueForecastRepository implements QueueForecastRepository {

    private final Map<UUID, QueueForecast> forecasts = new ConcurrentHashMap<>();

    @Override
    public Optional<QueueForecast> findById(UUID forecastId) {
        return Optional.ofNullable(forecasts.get(forecastId));
    }

    @Override
    public List<QueueForecast> findLatestByAttraction(AttractionId attractionId) {
        return forecasts.values().stream()
                .filter(forecast -> forecast.attractionId().equals(attractionId))
                .sorted(Comparator.comparing(QueueForecast::generatedAt).reversed()
                        .thenComparing(QueueForecast::horizonMinutes))
                .toList()
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        QueueForecast::horizonMinutes,
                        forecast -> forecast,
                        (newest, ignored) -> newest,
                        java.util.LinkedHashMap::new
                ))
                .values()
                .stream()
                .sorted(Comparator.comparing(QueueForecast::horizonMinutes))
                .toList();
    }

    @Override
    public void save(QueueForecast forecast) {
        forecasts.put(forecast.forecastId(), forecast);
    }
}
