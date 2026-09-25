package com.deanwagman.lumenmarsh.venueops.attraction.application;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.Attraction;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionCommand;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionEventType;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatus;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionType;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.CapacityMode;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.InvalidAttractionTransitionException;
import com.deanwagman.lumenmarsh.venueops.attraction.infrastructure.InMemoryAttractionProcessedCommandRepository;
import com.deanwagman.lumenmarsh.venueops.attraction.infrastructure.InMemoryAttractionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AttractionServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-26T18:42:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final AttractionId MANGROVE_RUN = new AttractionId("mangrove-run");

    private InMemoryAttractionRepository repository;
    private InMemoryAttractionProcessedCommandRepository processedCommands;
    private RecordingPublisher publisher;
    private AttractionService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAttractionRepository();
        processedCommands = new InMemoryAttractionProcessedCommandRepository();
        publisher = new RecordingPublisher();
        service = new AttractionService(repository, processedCommands, CLOCK, publisher);
        repository.save(operatingMangroveRun());
    }

    @Test
    void executeRecordsCommandAndIncrementsVersion() {
        Attraction updated = service.execute(
                MANGROVE_RUN,
                UUID.randomUUID(),
                AttractionCommand.PLACE_WEATHER_HOLD,
                "Operator One",
                "Lightning detected within operating radius",
                null,
                0L
        );

        assertThat(updated.status()).isEqualTo(AttractionStatus.WEATHER_HOLD);
        assertThat(updated.version()).isEqualTo(1);
        assertThat(updated.activity()).hasSize(1);
        assertThat(updated.activity().getFirst().type()).isEqualTo(AttractionEventType.WEATHER_HOLD_PLACED);
        assertThat(service.get(MANGROVE_RUN).status()).isEqualTo(AttractionStatus.WEATHER_HOLD);
    }

    @Test
    void successfulCommandPublishesOneGuestSafeUpdate() {
        Attraction updated = service.execute(
                MANGROVE_RUN,
                UUID.randomUUID(),
                AttractionCommand.UPDATE_WAIT_TIME,
                "Operator One",
                null,
                35,
                0L
        );

        assertThat(publisher.updates).hasSize(1);
        AttractionOperationalUpdate update = publisher.updates.getFirst();
        assertThat(update.eventId()).isEqualTo(updated.activity().getFirst().id());
        assertThat(update.eventType()).isEqualTo(AttractionUpdateEventType.WAIT_TIME_CHANGED);
        assertThat(update.occurredAt()).isEqualTo(NOW);
        assertThat(update.attraction().id()).isEqualTo("mangrove-run");
        assertThat(update.attraction().status()).isEqualTo(AttractionStatus.OPERATING);
        assertThat(update.attraction().capacityMode()).isEqualTo(CapacityMode.NORMAL);
        assertThat(update.attraction().waitMinutes()).isEqualTo(35);
        assertThat(update.attraction().version()).isEqualTo(1);
        assertThat(update.attraction().version()).isEqualTo(updated.version());
    }

    @Test
    void staleExpectedVersionIsRejectedWithoutChangingState() {
        assertThatThrownBy(() -> service.execute(
                MANGROVE_RUN,
                UUID.randomUUID(),
                AttractionCommand.PLACE_WEATHER_HOLD,
                "Operator One",
                "Lightning nearby",
                null,
                9L
        )).isInstanceOf(StaleAttractionVersionException.class);

        Attraction current = service.get(MANGROVE_RUN);
        assertThat(current.status()).isEqualTo(AttractionStatus.OPERATING);
        assertThat(current.version()).isZero();
        assertThat(current.activity()).isEmpty();
        assertThat(publisher.updates).isEmpty();
    }

    @Test
    void unknownAttractionIsRejected() {
        assertThatThrownBy(() -> service.get(new AttractionId("missing-ride")))
                .isInstanceOf(AttractionNotFoundException.class);
    }

    @Test
    void invalidTransitionIsRejectedWithoutPublishing() {
        assertThatThrownBy(() -> service.execute(
                MANGROVE_RUN,
                UUID.randomUUID(),
                AttractionCommand.START_TESTING,
                "Operator One",
                null,
                null,
                0L
        )).isInstanceOf(InvalidAttractionTransitionException.class);

        assertThat(publisher.updates).isEmpty();
        assertThat(service.get(MANGROVE_RUN).version()).isZero();
    }

    @Test
    void repositoryFailuresPublishNothing() {
        AttractionRepository failingRepository = mock(AttractionRepository.class);
        when(failingRepository.findById(MANGROVE_RUN)).thenReturn(Optional.of(operatingMangroveRun()));
        doThrow(new RuntimeException("write failed")).when(failingRepository).save(any());
        AttractionService failingService = new AttractionService(
                failingRepository,
                processedCommands,
                CLOCK,
                publisher
        );

        assertThatThrownBy(() -> failingService.execute(
                MANGROVE_RUN,
                UUID.randomUUID(),
                AttractionCommand.PLACE_WEATHER_HOLD,
                "Operator One",
                "Lightning nearby",
                null,
                0L
        )).hasMessage("write failed");

        assertThat(publisher.updates).isEmpty();
    }

    @Test
    void duplicateCommandIdReplaysWithoutPublishingAgain() {
        UUID commandId = UUID.fromString("11111111-1111-4111-8111-111111111111");
        Attraction first = service.execute(
                MANGROVE_RUN,
                commandId,
                AttractionCommand.PLACE_WEATHER_HOLD,
                "Operator One",
                "Lightning detected within operating radius",
                null,
                0L
        );
        Attraction replayed = service.execute(
                MANGROVE_RUN,
                commandId,
                AttractionCommand.PLACE_WEATHER_HOLD,
                "Operator One",
                "Lightning detected within operating radius",
                null,
                0L
        );

        assertThat(replayed.status()).isEqualTo(first.status());
        assertThat(replayed.version()).isEqualTo(first.version());
        assertThat(publisher.updates).hasSize(1);
        assertThat(service.get(MANGROVE_RUN).activity()).hasSize(1);
    }

    private static Attraction operatingMangroveRun() {
        return Attraction.rehydrate(
                MANGROVE_RUN,
                "Mangrove Run",
                "Luminous Wetlands",
                AttractionType.BOAT_EXPEDITION,
                AttractionStatus.OPERATING,
                CapacityMode.NORMAL,
                25,
                NOW,
                0L,
                List.of()
        );
    }

    private static final class RecordingPublisher implements AttractionUpdatePublisher {
        private final List<AttractionOperationalUpdate> updates = new ArrayList<>();

        @Override
        public void publish(AttractionOperationalUpdate update) {
            updates.add(update);
        }
    }
}
