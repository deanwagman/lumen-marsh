package com.deanwagman.lumenmarsh.venueops.weather.application;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentNotFoundException;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentRepository;
import com.deanwagman.lumenmarsh.venueops.incident.domain.Incident;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentId;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentSeverity;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentType;
import com.deanwagman.lumenmarsh.venueops.weather.domain.StaleWeatherRecommendationSourceException;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendation;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationCommand;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationId;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationInbound;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationOperatorStatus;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationSeverity;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationStatus;
import com.deanwagman.lumenmarsh.venueops.weather.infrastructure.InMemoryWeatherRecommendationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WeatherRecommendationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-01T16:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final WeatherRecommendationId ID = new WeatherRecommendationId("rec-lightning-1");

    private InMemoryWeatherRecommendationRepository recommendations;
    private IncidentRepository incidents;
    private RecordingPublisher publisher;
    private WeatherRecommendationService service;

    @BeforeEach
    void setUp() {
        recommendations = new InMemoryWeatherRecommendationRepository();
        incidents = mock(IncidentRepository.class);
        publisher = new RecordingPublisher();
        service = new WeatherRecommendationService(recommendations, incidents, CLOCK, publisher);
    }

    @Test
    void ingestCreatesThenTreatsRetryAsDuplicateWithoutPublishingAgain() {
        WeatherRecommendationIngestResult created = service.ingest(activeInbound(1));
        WeatherRecommendationIngestResult duplicate = service.ingest(activeInbound(1));

        assertThat(created.created()).isTrue();
        assertThat(duplicate.duplicate()).isTrue();
        assertThat(duplicate.recommendation().version()).isEqualTo(1);
        assertThat(duplicate.recommendation().activity()).hasSize(1);
        assertThat(publisher.updates).hasSize(1);
        assertThat(publisher.updates.getFirst().sseEventName()).isEqualTo("weather.recommendation.updated");
    }

    @Test
    void newerSourceVersionPublishesClearedEvent() {
        service.ingest(activeInbound(1));
        publisher.updates.clear();

        WeatherRecommendationIngestResult updated = service.ingest(clearedInbound(2));

        assertThat(updated.duplicate()).isFalse();
        assertThat(updated.created()).isFalse();
        assertThat(updated.recommendation().status()).isEqualTo(WeatherRecommendationStatus.CLEARED);
        assertThat(publisher.updates).hasSize(1);
        assertThat(publisher.updates.getFirst().sseEventName()).isEqualTo("weather.recommendation.cleared");
    }

    @Test
    void staleSourceVersionIsRejected() {
        service.ingest(activeInbound(1));
        service.ingest(clearedInbound(2));

        assertThatThrownBy(() -> service.ingest(activeInbound(1)))
                .isInstanceOf(StaleWeatherRecommendationSourceException.class);
        assertThat(service.get(ID).sourceVersion()).isEqualTo(2);
    }

    @Test
    void linkIncidentRequiresAnExistingIncident() {
        service.ingest(activeInbound(1));

        assertThatThrownBy(() -> service.execute(
                ID,
                WeatherRecommendationCommand.LINK_INCIDENT,
                "Operator One",
                null,
                1L,
                "missing-incident"
        )).isInstanceOf(IncidentNotFoundException.class);

        Incident incident = Incident.report(
                "Lightning activity near western basin",
                IncidentType.WEATHER,
                IncidentSeverity.MAJOR,
                "Prefill from recommendation",
                List.of(),
                "Operator One",
                CLOCK
        );
        when(incidents.findById(incident.id())).thenReturn(Optional.of(incident));

        WeatherRecommendation linked = service.execute(
                ID,
                WeatherRecommendationCommand.LINK_INCIDENT,
                "Operator One",
                "Opened weather incident",
                1L,
                incident.id().value()
        );

        assertThat(linked.operatorStatus()).isEqualTo(WeatherRecommendationOperatorStatus.LINKED);
        assertThat(linked.linkedIncidentId()).isEqualTo(incident.id());
        assertThat(publisher.updates.getLast().sseEventName()).isEqualTo("weather.recommendation.updated");
    }

    @Test
    void staleOperatorVersionIsRejected() {
        service.ingest(activeInbound(1));

        assertThatThrownBy(() -> service.execute(
                ID,
                WeatherRecommendationCommand.ACKNOWLEDGE,
                "Operator One",
                null,
                99L,
                null
        )).isInstanceOf(StaleWeatherRecommendationVersionException.class);
    }

    private static WeatherRecommendationInbound activeInbound(long sourceVersion) {
        return new WeatherRecommendationInbound(
                ID,
                "simulated-lightning-hold",
                WeatherRecommendationStatus.ACTIVE,
                WeatherRecommendationSeverity.WARNING,
                "Place Mangrove Run and Cypress Coil on weather hold",
                "Simulated lightning strike 1.2 miles from the western basin.",
                "Place Mangrove Run and Cypress Coil on weather hold",
                List.of(new AttractionId("mangrove-run"), new AttractionId("cypress-coil")),
                NOW,
                sourceVersion
        );
    }

    private static WeatherRecommendationInbound clearedInbound(long sourceVersion) {
        return new WeatherRecommendationInbound(
                ID,
                "simulated-lightning-hold",
                WeatherRecommendationStatus.CLEARED,
                WeatherRecommendationSeverity.INFO,
                "Lightning hold can be reviewed for clearance",
                "Simulated lightning is outside the hold radius.",
                "Review weather holds for return-to-service",
                List.of(new AttractionId("mangrove-run"), new AttractionId("cypress-coil")),
                NOW,
                sourceVersion
        );
    }

    private static final class RecordingPublisher implements WeatherRecommendationUpdatePublisher {
        private final List<WeatherRecommendationOperationalUpdate> updates = new ArrayList<>();

        @Override
        public void publish(WeatherRecommendationOperationalUpdate update) {
            updates.add(update);
        }
    }
}
