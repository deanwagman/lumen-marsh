package com.deanwagman.lumenmarsh.venueops.attraction.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionActivity;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionCapacityChanged;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionEventType;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatus;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatusChanged;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionWaitTimeChanged;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.CapacityMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AttractionActivityMapperTest {

    private static final Instant NOW = Instant.parse("2026-08-26T18:42:00Z");
    private static final AttractionId MANGROVE_RUN = new AttractionId("mangrove-run");

    @ParameterizedTest
    @EnumSource(AttractionEventType.class)
    void roundTripsEveryEventType(AttractionEventType type) {
        AttractionActivity original = sample(type);
        AttractionActivity restored = AttractionActivityMapper.toDomain(AttractionActivityMapper.toEntity(original));
        assertThat(restored).isEqualTo(original);
    }

    private static AttractionActivity sample(AttractionEventType type) {
        return switch (type) {
            case ATTRACTION_TESTING_STARTED,
                 ATTRACTION_TESTING_COMPLETED,
                 ATTRACTION_OPENED,
                 WEATHER_HOLD_PLACED,
                 WEATHER_HOLD_CLEARED,
                 TECHNICAL_FAULT_REPORTED,
                 REPAIR_COMPLETED,
                 ATTRACTION_CLOSED -> new AttractionStatusChanged(
                    "evt-" + type.name(),
                    MANGROVE_RUN,
                    type,
                    "Operator One",
                    "Operational change",
                    NOW,
                    0L,
                    1L,
                    AttractionStatus.OPERATING,
                    AttractionStatus.WEATHER_HOLD
            );
            case CAPACITY_REDUCED, CAPACITY_RESTORED -> new AttractionCapacityChanged(
                    "evt-" + type.name(),
                    MANGROVE_RUN,
                    type,
                    "Operator One",
                    "Capacity change",
                    NOW,
                    0L,
                    1L,
                    CapacityMode.NORMAL,
                    CapacityMode.REDUCED
            );
            case WAIT_TIME_UPDATED -> new AttractionWaitTimeChanged(
                    "evt-" + type.name(),
                    MANGROVE_RUN,
                    type,
                    "Operator One",
                    null,
                    NOW,
                    1L,
                    2L,
                    25,
                    0
            );
        };
    }
}
