package com.deanwagman.lumenmarsh.venueops.attraction.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttractionTest {

    private static final Instant NOW = Instant.parse("2026-08-26T18:42:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final String ACTOR = "Operator One";

    @Test
    void createStartsClosedWithNoWaitAndNotApplicableCapacity() {
        Attraction attraction = closedAttraction();

        assertThat(attraction.status()).isEqualTo(AttractionStatus.CLOSED);
        assertThat(attraction.capacityMode()).isEqualTo(CapacityMode.NOT_APPLICABLE);
        assertThat(attraction.waitMinutes()).isNull();
        assertThat(attraction.version()).isZero();
        assertThat(attraction.activity()).isEmpty();
        assertThat(attraction.guestStatusMessage()).isEqualTo("Currently closed.");
        assertThat(attraction.updatedAt().getNano()).isZero();
    }

    @Test
    void validLifecycleIncrementsVersionAndRecordsStatusHistory() {
        Attraction attraction = closedAttraction();

        attraction.startTesting(ACTOR, null, CLOCK);
        assertThat(attraction.status()).isEqualTo(AttractionStatus.TESTING);
        assertThat(attraction.version()).isEqualTo(1);
        assertThat(attraction.guestStatusMessage()).isEqualTo("Preparing to welcome explorers.");

        attraction.completeTesting(ACTOR, null, CLOCK);
        assertThat(attraction.status()).isEqualTo(AttractionStatus.RETURNING_TO_SERVICE);
        assertThat(attraction.guestStatusMessage()).isEqualTo("Expected to reopen soon.");

        attraction.approveReturnToService(ACTOR, "Morning inspection complete", CLOCK);
        assertThat(attraction.status()).isEqualTo(AttractionStatus.OPERATING);
        assertThat(attraction.capacityMode()).isEqualTo(CapacityMode.NORMAL);
        assertThat(attraction.waitMinutes()).isZero();
        assertThat(attraction.guestStatusMessage()).isNull();
        assertThat(attraction.version()).isEqualTo(3);
        assertThat(attraction.updatedAt()).isEqualTo(NOW);

        assertThat(attraction.activity()).hasSize(3);
        AttractionStatusChanged opened = (AttractionStatusChanged) attraction.activity().get(2);
        assertThat(opened.type()).isEqualTo(AttractionEventType.ATTRACTION_OPENED);
        assertThat(opened.previousStatus()).isEqualTo(AttractionStatus.RETURNING_TO_SERVICE);
        assertThat(opened.newStatus()).isEqualTo(AttractionStatus.OPERATING);
        assertThat(opened.previousVersion()).isEqualTo(2);
        assertThat(opened.resultingVersion()).isEqualTo(3);
        assertThat(opened.actor()).isEqualTo(ACTOR);
        assertThat(opened.occurredAt()).isEqualTo(NOW);
    }

    @Test
    void weatherHoldClearsWaitAndCapacityThenRequiresTestingBeforeReopening() {
        Attraction attraction = operatingAttraction();
        attraction.updateWaitTime(25, ACTOR, null, CLOCK);

        attraction.placeWeatherHold(ACTOR, "Lightning detected within operating radius", CLOCK);

        assertThat(attraction.status()).isEqualTo(AttractionStatus.WEATHER_HOLD);
        assertThat(attraction.waitMinutes()).isNull();
        assertThat(attraction.capacityMode()).isEqualTo(CapacityMode.NOT_APPLICABLE);
        assertThat(attraction.guestStatusMessage()).isEqualTo("Temporarily unavailable due to nearby weather.");

        Snapshot snapshot = Snapshot.of(attraction);
        assertThatThrownBy(() -> attraction.approveReturnToService(ACTOR, "Skip testing", CLOCK))
                .isInstanceOf(InvalidAttractionTransitionException.class);
        snapshot.assertUnchanged(attraction);

        attraction.clearWeatherHold(ACTOR, "Storm cell moved out of radius", CLOCK);
        assertThat(attraction.status()).isEqualTo(AttractionStatus.TESTING);

        attraction.completeTesting(ACTOR, null, CLOCK);
        attraction.approveReturnToService(ACTOR, "Return to service approved", CLOCK);

        assertThat(attraction.status()).isEqualTo(AttractionStatus.OPERATING);
        assertThat(attraction.capacityMode()).isEqualTo(CapacityMode.NORMAL);
        assertThat(attraction.waitMinutes()).isZero();
    }

    @Test
    void technicalDelayClearsWaitAndReturnsThroughTesting() {
        Attraction attraction = operatingAttraction();
        attraction.updateWaitTime(12, ACTOR, null, CLOCK);

        attraction.reportTechnicalFault(ACTOR, "Sensor array fault on dispatch", CLOCK);
        assertThat(attraction.status()).isEqualTo(AttractionStatus.TECHNICAL_DELAY);
        assertThat(attraction.waitMinutes()).isNull();
        assertThat(attraction.capacityMode()).isEqualTo(CapacityMode.NOT_APPLICABLE);
        assertThat(attraction.guestStatusMessage()).isEqualTo("Temporarily unavailable.");

        attraction.completeRepair(ACTOR, "Sensor replaced and verified", CLOCK);
        assertThat(attraction.status()).isEqualTo(AttractionStatus.TESTING);
    }

    @ParameterizedTest
    @EnumSource(value = AttractionStatus.class, names = {"TESTING", "RETURNING_TO_SERVICE", "OPERATING", "WEATHER_HOLD", "TECHNICAL_DELAY"})
    void closeForDayIsAllowedFromEveryNonClosedStatus(AttractionStatus status) {
        Attraction attraction = attractionIn(status);
        attraction.closeForDay(ACTOR, "Park closing", CLOCK);

        assertThat(attraction.status()).isEqualTo(AttractionStatus.CLOSED);
        assertThat(attraction.waitMinutes()).isNull();
        assertThat(attraction.capacityMode()).isEqualTo(CapacityMode.NOT_APPLICABLE);
        assertThat(lastEvent(attraction).type()).isEqualTo(AttractionEventType.ATTRACTION_CLOSED);
    }

    @Test
    void rejectedCommandDoesNotChangeStateOrHistory() {
        Attraction attraction = closedAttraction();
        Snapshot snapshot = Snapshot.of(attraction);

        assertThatThrownBy(() -> attraction.placeWeatherHold(ACTOR, "Lightning nearby", CLOCK))
                .isInstanceOf(InvalidAttractionTransitionException.class);

        snapshot.assertUnchanged(attraction);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void exceptionalCommandsRequireAMeaningfulReason(String reason) {
        Attraction attraction = operatingAttraction();
        Snapshot snapshot = Snapshot.of(attraction);

        assertThatThrownBy(() -> attraction.placeWeatherHold(ACTOR, reason, CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reason");
        snapshot.assertUnchanged(attraction);
    }

    @Test
    void waitTimeCanBeZeroButNotNegativeOrAboveCap() {
        Attraction attraction = operatingAttraction();

        attraction.updateWaitTime(0, ACTOR, null, CLOCK);
        assertThat(attraction.waitMinutes()).isZero();

        Snapshot snapshot = Snapshot.of(attraction);
        assertThatThrownBy(() -> attraction.updateWaitTime(-1, ACTOR, null, CLOCK))
                .isInstanceOf(IllegalArgumentException.class);
        snapshot.assertUnchanged(attraction);

        assertThatThrownBy(() -> attraction.updateWaitTime(301, ACTOR, null, CLOCK))
                .isInstanceOf(IllegalArgumentException.class);
        snapshot.assertUnchanged(attraction);
    }

    @Test
    void waitTimeCannotBeUpdatedWhenNotOperating() {
        Attraction attraction = closedAttraction();
        Snapshot snapshot = Snapshot.of(attraction);

        assertThatThrownBy(() -> attraction.updateWaitTime(10, ACTOR, null, CLOCK))
                .isInstanceOf(InvalidAttractionTransitionException.class);

        snapshot.assertUnchanged(attraction);
    }

    @Test
    void capacityCanOnlyChangeWhileOperating() {
        Attraction attraction = closedAttraction();
        Snapshot snapshot = Snapshot.of(attraction);

        assertThatThrownBy(() -> attraction.reduceCapacity(ACTOR, "Single boat operation", CLOCK))
                .isInstanceOf(InvalidAttractionTransitionException.class);
        snapshot.assertUnchanged(attraction);

        Attraction operating = operatingAttraction();
        operating.reduceCapacity(ACTOR, "Single boat operation", CLOCK);
        assertThat(operating.capacityMode()).isEqualTo(CapacityMode.REDUCED);

        AttractionCapacityChanged reduced = (AttractionCapacityChanged) lastEvent(operating);
        assertThat(reduced.previousMode()).isEqualTo(CapacityMode.NORMAL);
        assertThat(reduced.newMode()).isEqualTo(CapacityMode.REDUCED);

        operating.restoreCapacity(ACTOR, null, CLOCK);
        assertThat(operating.capacityMode()).isEqualTo(CapacityMode.NORMAL);
    }

    @Test
    void capacityRestoreIsRejectedWhenAlreadyNormal() {
        Attraction attraction = operatingAttraction();
        Snapshot snapshot = Snapshot.of(attraction);

        assertThatThrownBy(() -> attraction.restoreCapacity(ACTOR, null, CLOCK))
                .isInstanceOf(InvalidAttractionTransitionException.class);
        snapshot.assertUnchanged(attraction);
    }

    @Test
    void rehydrateRestoresOperatingSeedState() {
        Attraction attraction = Attraction.rehydrate(
                new AttractionId("mangrove-run"),
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

        assertThat(attraction.status()).isEqualTo(AttractionStatus.OPERATING);
        assertThat(attraction.waitMinutes()).isEqualTo(25);
        assertThat(attraction.capacityMode()).isEqualTo(CapacityMode.NORMAL);
        assertThat(attraction.uncommittedActivity()).isEmpty();
    }

    @Test
    void timestampsAreTakenFromUtcClock() {
        Attraction attraction = closedAttraction();
        attraction.startTesting(ACTOR, null, CLOCK);

        assertThat(attraction.updatedAt()).isEqualTo(NOW);
        assertThat(lastEvent(attraction).occurredAt()).isEqualTo(NOW);
        assertThat(lastEvent(attraction).occurredAt().toString()).endsWith("Z");
    }

    private static Attraction closedAttraction() {
        return Attraction.create(
                new AttractionId("cypress-coil"),
                "Cypress Coil",
                "Cypress Basin",
                AttractionType.LAUNCH_COASTER,
                CLOCK
        );
    }

    private static Attraction operatingAttraction() {
        Attraction attraction = closedAttraction();
        attraction.startTesting(ACTOR, null, CLOCK);
        attraction.completeTesting(ACTOR, null, CLOCK);
        attraction.approveReturnToService(ACTOR, "Cleared", CLOCK);
        attraction.markActivityCommitted();
        return attraction;
    }

    private static Attraction attractionIn(AttractionStatus status) {
        Attraction attraction = closedAttraction();
        switch (status) {
            case CLOSED -> {
            }
            case TESTING -> attraction.startTesting(ACTOR, null, CLOCK);
            case RETURNING_TO_SERVICE -> {
                attraction.startTesting(ACTOR, null, CLOCK);
                attraction.completeTesting(ACTOR, null, CLOCK);
            }
            case OPERATING -> {
                attraction.startTesting(ACTOR, null, CLOCK);
                attraction.completeTesting(ACTOR, null, CLOCK);
                attraction.approveReturnToService(ACTOR, "Cleared", CLOCK);
            }
            case WEATHER_HOLD -> {
                attraction.startTesting(ACTOR, null, CLOCK);
                attraction.completeTesting(ACTOR, null, CLOCK);
                attraction.approveReturnToService(ACTOR, "Cleared", CLOCK);
                attraction.placeWeatherHold(ACTOR, "Lightning nearby", CLOCK);
            }
            case TECHNICAL_DELAY -> {
                attraction.startTesting(ACTOR, null, CLOCK);
                attraction.completeTesting(ACTOR, null, CLOCK);
                attraction.approveReturnToService(ACTOR, "Cleared", CLOCK);
                attraction.reportTechnicalFault(ACTOR, "Sensor fault", CLOCK);
            }
        }
        return attraction;
    }

    private static AttractionActivity lastEvent(Attraction attraction) {
        List<AttractionActivity> activity = attraction.activity();
        return activity.get(activity.size() - 1);
    }

    private record Snapshot(AttractionStatus status, CapacityMode capacityMode, Integer waitMinutes, long version, int historySize) {
        static Snapshot of(Attraction attraction) {
            return new Snapshot(
                    attraction.status(),
                    attraction.capacityMode(),
                    attraction.waitMinutes(),
                    attraction.version(),
                    attraction.activity().size()
            );
        }

        void assertUnchanged(Attraction attraction) {
            assertThat(attraction.status()).isEqualTo(status);
            assertThat(attraction.capacityMode()).isEqualTo(capacityMode);
            assertThat(attraction.waitMinutes()).isEqualTo(waitMinutes);
            assertThat(attraction.version()).isEqualTo(version);
            assertThat(attraction.activity()).hasSize(historySize);
        }
    }
}
