package com.deanwagman.lumenmarsh.venueops.weather.infrastructure;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.weather.application.StaleWeatherRecommendationVersionException;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendation;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationId;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationInbound;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationSeverity;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryWeatherRecommendationRepositoryTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-01T16:00:00Z"), ZoneOffset.UTC);

    @Test
    void concurrentSavesRejectStaleVersions() {
        InMemoryWeatherRecommendationRepository repository = new InMemoryWeatherRecommendationRepository();
        WeatherRecommendation original = WeatherRecommendation.receive(inbound(), CLOCK);
        repository.save(original);

        WeatherRecommendation first = repository.findById(original.id()).orElseThrow();
        WeatherRecommendation second = repository.findById(original.id()).orElseThrow();
        first.acknowledge("Operator One", null, CLOCK);
        second.acknowledge("Operator One", null, CLOCK);
        repository.save(first);

        assertThatThrownBy(() -> repository.save(second))
                .isInstanceOf(StaleWeatherRecommendationVersionException.class);
        assertThat(repository.findById(original.id()).orElseThrow().version()).isEqualTo(2);
    }

    private static WeatherRecommendationInbound inbound() {
        return new WeatherRecommendationInbound(
                new WeatherRecommendationId("rec-1"),
                "simulated-lightning-hold",
                WeatherRecommendationStatus.ACTIVE,
                WeatherRecommendationSeverity.WARNING,
                "Place Mangrove Run on weather hold",
                "Simulated lightning nearby.",
                "Place Mangrove Run on weather hold",
                List.of(new AttractionId("mangrove-run")),
                Instant.parse("2026-09-01T16:00:00Z"),
                1L
        );
    }
}
