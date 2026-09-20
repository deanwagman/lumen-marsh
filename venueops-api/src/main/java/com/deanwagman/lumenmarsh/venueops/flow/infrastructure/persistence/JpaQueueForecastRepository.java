package com.deanwagman.lumenmarsh.venueops.flow.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.flow.application.QueueForecastRepository;
import com.deanwagman.lumenmarsh.venueops.flow.domain.QueueForecast;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface QueueForecastJpaRepository extends JpaRepository<QueueForecastEntity, String> {
    List<QueueForecastEntity> findByAttractionIdOrderByGeneratedAtDescHorizonMinutesAsc(String attractionId);
}

@Repository
@ConditionalOnProperty(name = "venueops.attractions.persistence", havingValue = "jpa")
public class JpaQueueForecastRepository implements QueueForecastRepository {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final QueueForecastJpaRepository forecasts;

    public JpaQueueForecastRepository(QueueForecastJpaRepository forecasts) {
        this.forecasts = forecasts;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QueueForecast> findById(UUID forecastId) {
        return forecasts.findById(forecastId.toString()).map(JpaQueueForecastRepository::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QueueForecast> findLatestByAttraction(AttractionId attractionId) {
        return forecasts.findByAttractionIdOrderByGeneratedAtDescHorizonMinutesAsc(attractionId.value()).stream()
                .map(JpaQueueForecastRepository::toDomain)
                .collect(java.util.stream.Collectors.toMap(
                        QueueForecast::horizonMinutes,
                        forecast -> forecast,
                        (newest, ignored) -> newest,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .sorted(Comparator.comparing(QueueForecast::horizonMinutes))
                .toList();
    }

    @Override
    @Transactional
    public void save(QueueForecast forecast) {
        forecasts.save(new QueueForecastEntity(
                forecast.forecastId().toString(),
                forecast.attractionId().value(),
                forecast.generatedAt(),
                forecast.basedOnObservationId().toString(),
                forecast.horizonMinutes(),
                forecast.predictedQueueLength(),
                forecast.predictedWaitMinutes(),
                forecast.confidence(),
                JSON.writeValueAsString(forecast.assumptions()),
                forecast.explanation(),
                forecast.simulated()
        ));
    }

    private static QueueForecast toDomain(QueueForecastEntity entity) {
        List<String> assumptions = JSON.readValue(entity.getAssumptions(), new TypeReference<List<String>>() {
        });
        return new QueueForecast(
                UUID.fromString(entity.getForecastId()),
                new AttractionId(entity.getAttractionId()),
                entity.getGeneratedAt(),
                UUID.fromString(entity.getBasedOnObservationId()),
                entity.getHorizonMinutes(),
                entity.getPredictedQueueLength(),
                entity.getPredictedWaitMinutes(),
                entity.getConfidence(),
                assumptions,
                entity.getExplanation(),
                entity.isSimulated()
        );
    }
}
