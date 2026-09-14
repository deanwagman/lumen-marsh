package com.deanwagman.lumenmarsh.venueops.attraction.infrastructure;

import com.deanwagman.lumenmarsh.venueops.attraction.application.StaleAttractionVersionException;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.Attraction;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatus;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionType;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.CapacityMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryAttractionRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-08-26T18:42:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final AttractionId MANGROVE_RUN = new AttractionId("mangrove-run");

    private InMemoryAttractionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAttractionRepository();
        repository.save(Attraction.rehydrate(
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
        ));
    }

    @Test
    void saveRejectsAStaleCopyWithoutOverwriting() {
        Attraction first = repository.findById(MANGROVE_RUN).orElseThrow();
        Attraction second = repository.findById(MANGROVE_RUN).orElseThrow();

        first.placeWeatherHold("Operator One", "Lightning nearby", CLOCK);
        repository.save(first);

        second.reportTechnicalFault("Operator One", "Sensor fault", CLOCK);
        assertThatThrownBy(() -> repository.save(second))
                .isInstanceOf(StaleAttractionVersionException.class);

        Attraction stored = repository.findById(MANGROVE_RUN).orElseThrow();
        assertThat(stored.status()).isEqualTo(AttractionStatus.WEATHER_HOLD);
        assertThat(stored.version()).isEqualTo(1);
        assertThat(stored.activity()).hasSize(1);
    }
}
