package com.deanwagman.lumenmarsh.venueops.incident.application;

import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionNotFoundException;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.Attraction;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionType;
import com.deanwagman.lumenmarsh.venueops.attraction.infrastructure.InMemoryAttractionRepository;
import com.deanwagman.lumenmarsh.venueops.incident.domain.Incident;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentCommand;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentEventType;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentId;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentSeverity;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentStatus;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentType;
import com.deanwagman.lumenmarsh.venueops.incident.domain.InvalidIncidentTransitionException;
import com.deanwagman.lumenmarsh.venueops.incident.infrastructure.InMemoryIncidentRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IncidentServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-01T15:30:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private InMemoryIncidentRepository incidents;
    private InMemoryAttractionRepository attractions;
    private RecordingPublisher publisher;
    private RecordingIncidentPublisher incidentPublisher;
    private IncidentService service;

    @BeforeEach
    void setUp() {
        incidents = new InMemoryIncidentRepository();
        attractions = new InMemoryAttractionRepository();
        publisher = new RecordingPublisher();
        incidentPublisher = new RecordingIncidentPublisher();
        service = new IncidentService(incidents, attractions, CLOCK, publisher, incidentPublisher);
        attractions.save(Attraction.create(new AttractionId("mangrove-run"), "Mangrove Run", "Luminous Wetlands", AttractionType.BOAT_EXPEDITION, CLOCK));
        attractions.save(Attraction.create(new AttractionId("cypress-coil"), "Cypress Coil", "Cypress Basin", AttractionType.LAUNCH_COASTER, CLOCK));
    }

    @Test
    void reportPersistsLifecycleStartWithoutGuestAdvisoryEvent() {
        Incident incident = service.report(
                "Lightning activity near western basin",
                IncidentType.WEATHER,
                IncidentSeverity.MAJOR,
                "Repeated strikes detected within the hold radius.",
                List.of("mangrove-run", "cypress-coil"),
                "Operator One"
        );

        assertThat(incident.status()).isEqualTo(IncidentStatus.REPORTED);
        assertThat(incident.version()).isEqualTo(1);
        assertThat(service.get(incident.id()).attractionIds()).hasSize(2);
        assertThat(publisher.updates).isEmpty();
        assertThat(incidentPublisher.updates).extracting(IncidentOperationalUpdate::sseEventName)
                .containsExactly("incident.reported");
    }

    @Test
    void unknownAttractionLinksAreRejectedWithoutSaving() {
        assertThatThrownBy(() -> service.report(
                "Unknown ride incident",
                IncidentType.TECHNICAL,
                IncidentSeverity.MINOR,
                null,
                List.of("missing-ride"),
                "Operator One"
        )).isInstanceOf(AttractionNotFoundException.class);

        assertThat(service.list()).isEmpty();
        assertThat(publisher.updates).isEmpty();
        assertThat(incidentPublisher.updates).isEmpty();
    }

    @Test
    void internalCommandsDoNotPublishGuestAdvisoryEvents() {
        Incident incident = reported();
        Incident updated = execute(incident.id(), IncidentCommand.ACKNOWLEDGE, 1L, null, null, null, null, null);

        assertThat(updated.status()).isEqualTo(IncidentStatus.ACKNOWLEDGED);
        assertThat(updated.version()).isEqualTo(2);
        assertThat(updated.activity().getLast().type()).isEqualTo(IncidentEventType.INCIDENT_ACKNOWLEDGED);
        assertThat(publisher.updates).isEmpty();
        assertThat(incidentPublisher.updates).extracting(IncidentOperationalUpdate::eventType)
                .contains(IncidentUpdateEventType.INCIDENT_UPDATED);
    }

    @Test
    void publishGuestAdvisoryEmitsPublishedEvent() {
        Incident incident = reported();
        execute(incident.id(), IncidentCommand.ACKNOWLEDGE, 1L, null, null, null, null, null);
        Incident published = execute(
                incident.id(),
                IncidentCommand.PUBLISH_GUEST_ADVISORY,
                2L,
                null,
                null,
                null,
                null,
                "Weather advisory"
        );

        assertThat(published.hasActiveGuestAdvisory()).isTrue();
        assertThat(publisher.updates).hasSize(1);
        GuestAdvisoryOperationalUpdate update = publisher.updates.getFirst();
        assertThat(update.eventType()).isEqualTo(GuestAdvisoryUpdateEventType.ADVISORY_PUBLISHED);
        assertThat(update.advisory().title()).isEqualTo("Weather advisory");
        assertThat(update.advisory().version()).isEqualTo(3);
        assertThat(incidentPublisher.updates).isNotEmpty();
    }

    @Test
    void invalidTransitionsPublishNothing() {
        Incident incident = reported();
        assertThatThrownBy(() -> execute(incident.id(), IncidentCommand.RESOLVE, 1L, "Too soon", null, null, null, null))
                .isInstanceOf(InvalidIncidentTransitionException.class);
        assertThat(service.get(incident.id()).version()).isEqualTo(1);
        assertThat(publisher.updates).isEmpty();
        assertThat(incidentPublisher.updates).extracting(IncidentOperationalUpdate::sseEventName)
                .containsExactly("incident.reported");
    }

    @Test
    void staleVersionsPublishNothing() {
        Incident incident = reported();
        assertThatThrownBy(() -> execute(incident.id(), IncidentCommand.ACKNOWLEDGE, 9L, null, null, null, null, null))
                .isInstanceOf(StaleIncidentVersionException.class);
        assertThat(service.get(incident.id()).status()).isEqualTo(IncidentStatus.REPORTED);
        assertThat(publisher.updates).isEmpty();
        assertThat(incidentPublisher.updates).extracting(IncidentOperationalUpdate::sseEventName)
                .containsExactly("incident.reported");
    }

    @Test
    void repositoryFailuresPublishNothing() {
        IncidentRepository failingRepository = mock(IncidentRepository.class);
        when(failingRepository.findById(any())).thenReturn(Optional.empty());
        IncidentService reporting = new IncidentService(failingRepository, attractions, CLOCK, publisher, incidentPublisher);
        doThrow(new RuntimeException("write failed")).when(failingRepository).save(any());

        assertThatThrownBy(() -> reporting.report(
                "Lightning activity near western basin",
                IncidentType.WEATHER,
                IncidentSeverity.MAJOR,
                null,
                List.of("mangrove-run"),
                "Operator One"
        )).hasMessage("write failed");
        assertThat(publisher.updates).isEmpty();
        assertThat(incidentPublisher.updates).isEmpty();
    }

    @Test
    void linkingUnknownAttractionIsRejected() {
        Incident incident = reported();
        assertThatThrownBy(() -> execute(
                incident.id(),
                IncidentCommand.LINK_ATTRACTION,
                1L,
                null,
                null,
                null,
                "missing-ride",
                null
        )).isInstanceOf(AttractionNotFoundException.class);
        assertThat(service.get(incident.id()).version()).isEqualTo(1);
        assertThat(publisher.updates).isEmpty();
        assertThat(incidentPublisher.updates).extracting(IncidentOperationalUpdate::sseEventName)
                .containsExactly("incident.reported");
    }

    private Incident reported() {
        return service.report(
                "Lightning activity near western basin",
                IncidentType.WEATHER,
                IncidentSeverity.MAJOR,
                "Repeated strikes detected within the hold radius.",
                List.of("mangrove-run", "cypress-coil"),
                "Operator One"
        );
    }

    private Incident execute(
            IncidentId id,
            IncidentCommand command,
            long expectedVersion,
            String reason,
            String assignee,
            IncidentSeverity severity,
            String attractionId,
            String guestTitle
    ) {
        return service.execute(
                id,
                command,
                "Operator One",
                reason,
                expectedVersion,
                assignee,
                severity,
                attractionId,
                guestTitle,
                guestTitle == null ? null : "Paused."
        );
    }

    private static final class RecordingPublisher implements GuestAdvisoryUpdatePublisher {
        private final List<GuestAdvisoryOperationalUpdate> updates = new ArrayList<>();

        @Override
        public void publish(GuestAdvisoryOperationalUpdate update) {
            updates.add(update);
        }
    }

    private static final class RecordingIncidentPublisher implements IncidentUpdatePublisher {
        private final List<IncidentOperationalUpdate> updates = new ArrayList<>();

        @Override
        public void publish(IncidentOperationalUpdate update) {
            updates.add(update);
        }
    }
}
