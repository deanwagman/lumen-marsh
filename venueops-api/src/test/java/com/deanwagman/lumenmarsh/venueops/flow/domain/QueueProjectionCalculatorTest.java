package com.deanwagman.lumenmarsh.venueops.flow.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueueProjectionCalculatorTest {

    @Test
    void ewmaWeightsNewestSampleMost() {
        assertThat(QueueProjectionCalculator.ewma(10, 8.0, 6.0)).isEqualTo(8.6, org.assertj.core.data.Offset.offset(0.0001));
        assertThat(QueueProjectionCalculator.ewma(10, 8.0, null)).isEqualTo(9.25, org.assertj.core.data.Offset.offset(0.0001));
        assertThat(QueueProjectionCalculator.ewma(10, null, null)).isEqualTo(10);
    }

    @Test
    void predictedQueueNeverGoesNegative() {
        int predicted = QueueProjectionCalculator.predictedQueueLength(10, 5, 20, 15, 0);
        assertThat(predicted).isZero();
    }

    @Test
    void waitUsesMinimumThroughput() {
        assertThat(QueueProjectionCalculator.calculatedWaitMinutes(10, 0)).isEqualTo(20);
    }

    @Test
    void freshnessBands() {
        java.time.Instant now = java.time.Instant.parse("2026-09-15T18:00:00Z");
        assertThat(QueueFreshness.of(now.minusSeconds(60), now)).isEqualTo(QueueFreshness.FRESH);
        assertThat(QueueFreshness.of(now.minusSeconds(180), now)).isEqualTo(QueueFreshness.DELAYED);
        assertThat(QueueFreshness.of(now.minusSeconds(400), now)).isEqualTo(QueueFreshness.STALE);
    }

    @Test
    void observationRejectsNegativeCounts() {
        assertThatThrownBy(() -> QueueObservation.accept(
                java.util.UUID.randomUUID(),
                new com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId("mangrove-run"),
                java.time.Instant.parse("2026-09-15T18:00:00Z"),
                60,
                -1,
                0,
                0,
                1,
                1,
                QueueObservationSourceType.SIMULATOR,
                true,
                java.time.Clock.fixed(java.time.Instant.parse("2026-09-15T18:00:00Z"), java.time.ZoneOffset.UTC)
        )).isInstanceOf(InvalidFlowObservationException.class);
    }
}
