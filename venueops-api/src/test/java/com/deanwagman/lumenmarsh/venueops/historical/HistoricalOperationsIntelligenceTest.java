package com.deanwagman.lumenmarsh.venueops.historical;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionEventType;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatus;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatusChanged;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.CapacityMode;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.Activity;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AttractionPlan;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AvailabilityClassification;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.Dataset;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.DatasetState;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.OperatingDay;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.ReplayBundle;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.FIXTURE_DATASET_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HistoricalOperationsIntelligenceTest {

    @Test
    void fixtureReplayIsIdempotentDeterministicAndRanksCypressCoilWorst() {
        InMemoryHistoricalStore store = new InMemoryHistoricalStore();
        HistoricalReplayService replay = new HistoricalReplayService(store);

        HistoricalReplayService.ReplayResult first = replay.replayFixture(FIXTURE_DATASET_ID, false);
        List<String> firstActivityIds = store.activities(FIXTURE_DATASET_ID, "cypress-coil").stream()
                .map(value -> value.value().id()).toList();
        HistoricalReplayService.ReplayResult second = replay.replayFixture(FIXTURE_DATASET_ID, false);

        assertThat(first.changed()).isTrue();
        assertThat(second.changed()).isFalse();
        assertThat(store.activities(FIXTURE_DATASET_ID, "cypress-coil").stream()
                .map(value -> value.value().id())).containsExactlyElementsOf(firstActivityIds);

        AvailabilityProjectionService projection = new AvailabilityProjectionService(store);
        HistoricalModels.AvailabilitySummary summary = projection.summary(
                FIXTURE_DATASET_ID, LocalDate.of(2026, 10, 13), LocalDate.of(2026, 10, 15));

        assertThat(summary.attractions().getFirst().attractionId()).isEqualTo("cypress-coil");
        assertThat(summary.attractions().getFirst().scheduledMinutes()).isEqualTo(1440);
        assertThat(summary.attractions().getFirst().operatingMinutes()).isEqualTo(1250);
        assertThat(summary.attractions().getFirst().uptimePercentage()).isEqualByComparingTo("86.81");
        assertThat(summary.attractions().getFirst().longestOutageMinutes()).isEqualTo(190);
        assertThat(summary.attractions()).filteredOn(value -> value.attractionId().equals("mangrove-run"))
                .singleElement().satisfies(value -> {
                    assertThat(value.operatingMinutes()).isEqualTo(1440);
                    assertThat(value.uptimePercentage()).isEqualByComparingTo("100.00");
                });
        assertThat(summary.attractions()).filteredOn(value -> value.attractionId().equals("stormglass-station"))
                .singleElement().satisfies(value -> {
                    assertThat(value.operatingMinutes()).isEqualTo(1360);
                    assertThat(value.uptimePercentage()).isEqualByComparingTo("94.44");
                });
        assertThat(summary.attractions()).filteredOn(value -> value.attractionId().equals("mangrove-run"))
                .singleElement().extracting(HistoricalModels.AttractionAvailability::degradedMinutes)
                .isEqualTo(60L);
        assertThat(summary.attractions()).filteredOn(value -> value.attractionId().equals("stormglass-station"))
                .singleElement().satisfies(value -> assertThat(value.downtimeMinutes())
                        .containsEntry(AvailabilityClassification.WEATHER_HOLD, 60L)
                        .containsEntry(AvailabilityClassification.TESTING_RETURN_TO_SERVICE, 20L));
        assertThat(summary.attractions().getFirst().citations()).allSatisfy(citation -> {
            assertThat(citation.id()).startsWith("segment-");
            assertThat(citation.controlTowerUrl()).contains(citation.id());
        });
    }

    @Test
    void checksumMismatchRequiresExplicitReplacementOfTheSelectedDataset() {
        InMemoryHistoricalStore store = new InMemoryHistoricalStore();
        LocalDate date = LocalDate.of(2026, 10, 13);
        store.replace(new ReplayBundle(new Dataset(FIXTURE_DATASET_ID, "old", 1, "old-checksum", date, date,
                DatasetState.COMPLETED, Instant.EPOCH, Instant.EPOCH),
                List.of(), List.of(), List.of(), List.of(), List.of()));
        store.replace(new ReplayBundle(new Dataset("other-dataset", "1", 2, "other", date, date,
                DatasetState.COMPLETED, Instant.EPOCH, Instant.EPOCH),
                List.of(new OperatingDay("other-dataset", date, "America/New_York",
                        LocalTime.of(9, 0), LocalTime.of(17, 0), 1)),
                List.of(), List.of(), List.of(), List.of()));
        HistoricalReplayService replay = new HistoricalReplayService(store);

        assertThatThrownBy(() -> replay.replayFixture(FIXTURE_DATASET_ID, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("explicit replacement");

        assertThat(replay.replayFixture(FIXTURE_DATASET_ID, true).changed()).isTrue();
        assertThat(store.findDataset(FIXTURE_DATASET_ID).orElseThrow().version()).isEqualTo("1");
        assertThat(store.days("other-dataset", date, date)).hasSize(1);
    }

    @Test
    void plannedClosureHasNoDenominator() {
        LocalDate date = LocalDate.of(2026, 11, 1);
        InMemoryHistoricalStore store = new InMemoryHistoricalStore();
        Dataset dataset = new Dataset("planned", "1", 1, "checksum", date, date,
                DatasetState.COMPLETED, Instant.EPOCH, Instant.EPOCH);
        store.replace(new ReplayBundle(dataset,
                List.of(new OperatingDay("planned", date, "America/New_York",
                        LocalTime.of(1, 30), LocalTime.of(3, 30), 1)),
                List.of(new AttractionPlan("planned", date, "cypress-coil",
                        LocalTime.of(1, 30), LocalTime.of(3, 30), true, "Annual inspection", 1)),
                List.of(), List.of(), List.of()));

        HistoricalModels.AvailabilityDetail detail =
                new AvailabilityProjectionService(store).detail("planned", "cypress-coil", date);

        assertThat(detail.plannedClosure()).isTrue();
        assertThat(detail.scheduledMinutes()).isZero();
        assertThat(detail.segments()).isEmpty();
        assertThat(detail.expectedStart()).isEqualTo(Instant.parse("2026-11-01T05:30:00Z"));
        assertThat(detail.expectedEnd()).isEqualTo(Instant.parse("2026-11-01T08:30:00Z"));
    }

    @Test
    void failureToOpenIsClassifiedForTheWholeExpectedInterval() {
        LocalDate date = LocalDate.of(2026, 10, 16);
        InMemoryHistoricalStore store = new InMemoryHistoricalStore();
        Dataset dataset = new Dataset("never-opened", "1", 1, "checksum", date, date,
                DatasetState.COMPLETED, Instant.EPOCH, Instant.EPOCH);
        store.replace(new ReplayBundle(dataset,
                List.of(new OperatingDay("never-opened", date, "America/New_York",
                        LocalTime.of(9, 0), LocalTime.of(17, 0), 1)),
                List.of(new AttractionPlan("never-opened", date, "cypress-coil",
                        LocalTime.of(9, 0), LocalTime.of(17, 0), false, null, 1)),
                List.of(), List.of(), List.of()));

        HistoricalModels.AvailabilityDetail detail =
                new AvailabilityProjectionService(store).detail("never-opened", "cypress-coil", date);

        assertThat(detail.scheduledMinutes()).isEqualTo(480);
        assertThat(detail.operatingMinutes()).isZero();
        assertThat(detail.longestOutageMinutes()).isEqualTo(480);
        assertThat(detail.segments()).singleElement()
                .extracting(HistoricalModels.AvailabilitySegment::classification)
                .isEqualTo(AvailabilityClassification.CLOSED_NOT_OPEN);
    }

    @Test
    void springForwardAndFallbackUseElapsedParkTime() {
        LocalDate spring = LocalDate.of(2026, 3, 8);
        HistoricalModels.AvailabilityDetail springDay = detailFor(spring, "America/New_York",
                LocalTime.of(1, 0), LocalTime.of(4, 0), false, null);
        assertThat(springDay.expectedStart()).isEqualTo(Instant.parse("2026-03-08T06:00:00Z"));
        assertThat(springDay.expectedEnd()).isEqualTo(Instant.parse("2026-03-08T08:00:00Z"));
        assertThat(springDay.scheduledMinutes()).isEqualTo(120);

        HistoricalModels.AvailabilityDetail gap = detailFor(spring, "America/New_York",
                LocalTime.of(2, 30), LocalTime.of(4, 0), false, null);
        assertThat(gap.expectedStart()).isEqualTo(Instant.parse("2026-03-08T07:30:00Z"));
        assertThat(gap.scheduledMinutes()).isEqualTo(30);

        LocalDate fallback = LocalDate.of(2026, 11, 1);
        HistoricalModels.AvailabilityDetail repeatedHour = detailFor(fallback, "America/New_York",
                LocalTime.of(1, 0), LocalTime.of(4, 0), false, null);
        assertThat(repeatedHour.expectedStart()).isEqualTo(Instant.parse("2026-11-01T05:00:00Z"));
        assertThat(repeatedHour.expectedEnd()).isEqualTo(Instant.parse("2026-11-01T09:00:00Z"));
        assertThat(repeatedHour.scheduledMinutes()).isEqualTo(240);
    }

    @Test
    void overnightExpectedCloseBelongsToTheNextParkDate() {
        HistoricalModels.AvailabilityDetail detail = detailFor(LocalDate.of(2026, 10, 14), "America/New_York",
                LocalTime.of(22, 0), LocalTime.of(2, 0), false, null);

        assertThat(detail.expectedStart()).isEqualTo(Instant.parse("2026-10-15T02:00:00Z"));
        assertThat(detail.expectedEnd()).isEqualTo(Instant.parse("2026-10-15T06:00:00Z"));
        assertThat(detail.scheduledMinutes()).isEqualTo(240);
    }

    @Test
    void earlyCloseAndLateOpenAreCommandedClosureInsideExpectedService() {
        LocalDate date = LocalDate.of(2026, 10, 16);
        ZoneId zone = ZoneId.of("America/New_York");
        Instant open = ZonedDateTime.of(date, LocalTime.of(9, 0), zone).toInstant();
        Instant earlyClose = ZonedDateTime.of(date, LocalTime.of(16, 0), zone).toInstant();
        InMemoryHistoricalStore early = dayStore("early-close", date, List.of(
                status("early-close", "open", open, AttractionStatus.CLOSED, AttractionStatus.OPERATING),
                status("early-close", "close", earlyClose, AttractionStatus.OPERATING, AttractionStatus.CLOSED)
        ));

        HistoricalModels.AvailabilityDetail earlyDetail =
                new AvailabilityProjectionService(early).detail("early-close", "cypress-coil", date);
        assertThat(earlyDetail.operatingMinutes()).isEqualTo(420);
        assertThat(earlyDetail.downtimeMinutes())
                .containsEntry(AvailabilityClassification.COMMANDED_CLOSURE, 60L);
        assertThat(earlyDetail.longestOutageMinutes()).isEqualTo(60);

        Instant lateOpen = ZonedDateTime.of(date, LocalTime.of(10, 0), zone).toInstant();
        InMemoryHistoricalStore late = dayStore("late-open", date, List.of(
                status("late-open", "open", lateOpen, AttractionStatus.CLOSED, AttractionStatus.OPERATING)
        ));
        HistoricalModels.AvailabilityDetail lateDetail =
                new AvailabilityProjectionService(late).detail("late-open", "cypress-coil", date);
        assertThat(lateDetail.operatingMinutes()).isEqualTo(420);
        assertThat(lateDetail.downtimeMinutes())
                .containsEntry(AvailabilityClassification.COMMANDED_CLOSURE, 60L);
    }

    @Test
    void closeExactlyAtExpectedEndDoesNotCreateDowntime() {
        LocalDate date = LocalDate.of(2026, 10, 16);
        ZoneId zone = ZoneId.of("America/New_York");
        InMemoryHistoricalStore store = dayStore("on-time-close", date, List.of(
                status("on-time-close", "open",
                        ZonedDateTime.of(date, LocalTime.of(9, 0), zone).toInstant(),
                        AttractionStatus.CLOSED, AttractionStatus.OPERATING),
                status("on-time-close", "close",
                        ZonedDateTime.of(date, LocalTime.of(17, 0), zone).toInstant(),
                        AttractionStatus.OPERATING, AttractionStatus.CLOSED)
        ));

        HistoricalModels.AvailabilityDetail detail =
                new AvailabilityProjectionService(store).detail("on-time-close", "cypress-coil", date);

        assertThat(detail.operatingMinutes()).isEqualTo(480);
        assertThat(detail.downtimeMinutes().values()).allMatch(minutes -> minutes == 0L);
    }

    @Test
    void reducedCapacityCountsAsUptimeAndDegradedTime() {
        LocalDate date = LocalDate.of(2026, 10, 16);
        ZoneId zone = ZoneId.of("America/New_York");
        InMemoryHistoricalStore store = dayStore("reduced", date, List.of(
                status("reduced", "open", ZonedDateTime.of(date, LocalTime.of(9, 0), zone).toInstant(),
                        AttractionStatus.CLOSED, AttractionStatus.OPERATING),
                new Activity("reduced", "source-reduce", "command-reduce",
                        new com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionCapacityChanged(
                                "activity-reduce", new AttractionId("cypress-coil"),
                                AttractionEventType.CAPACITY_REDUCED, "fixture", "Reduced dispatch",
                                ZonedDateTime.of(date, LocalTime.of(12, 0), zone).toInstant(),
                                1, 2, CapacityMode.NORMAL, CapacityMode.REDUCED))
        ));

        HistoricalModels.AvailabilityDetail detail =
                new AvailabilityProjectionService(store).detail("reduced", "cypress-coil", date);

        assertThat(detail.operatingMinutes()).isEqualTo(480);
        assertThat(detail.degradedMinutes()).isEqualTo(300);
        assertThat(detail.segments()).anySatisfy(segment -> {
            assertThat(segment.classification()).isEqualTo(AvailabilityClassification.REDUCED_CAPACITY);
            assertThat(segment.durationMinutes()).isEqualTo(300);
        });
    }

    @Test
    void plannedClosureIsOmittedFromTheRankingDenominator() {
        LocalDate closed = LocalDate.of(2026, 10, 16);
        LocalDate open = LocalDate.of(2026, 10, 17);
        InMemoryHistoricalStore store = new InMemoryHistoricalStore();
        store.replace(new ReplayBundle(new Dataset("mixed", "1", 1, "checksum", closed, open,
                DatasetState.COMPLETED, Instant.EPOCH, Instant.EPOCH),
                List.of(
                        new OperatingDay("mixed", closed, "America/New_York",
                                LocalTime.of(9, 0), LocalTime.of(17, 0), 1),
                        new OperatingDay("mixed", open, "America/New_York",
                                LocalTime.of(9, 0), LocalTime.of(17, 0), 1)),
                List.of(
                        new AttractionPlan("mixed", closed, "cypress-coil",
                                LocalTime.of(9, 0), LocalTime.of(17, 0), true, "Inspection", 1),
                        new AttractionPlan("mixed", open, "cypress-coil",
                                LocalTime.of(9, 0), LocalTime.of(17, 0), false, null, 1)),
                List.of(), List.of(), List.of()));

        HistoricalModels.AvailabilitySummary summary =
                new AvailabilityProjectionService(store).summary("mixed", closed, open);

        assertThat(summary.attractions()).singleElement().satisfies(value -> {
            assertThat(value.scheduledMinutes()).isEqualTo(480);
            assertThat(value.operatingMinutes()).isZero();
        });
    }

    @Test
    void deterministicProviderAnswersOnlyTheWorstUptimeQuestion() {
        InMemoryHistoricalStore store = new InMemoryHistoricalStore();
        new HistoricalReplayService(store).replayFixture(FIXTURE_DATASET_ID, false);
        HistoricalModels.AvailabilitySummary summary = new AvailabilityProjectionService(store).summary(
                FIXTURE_DATASET_ID, LocalDate.of(2026, 10, 13), LocalDate.of(2026, 10, 15));

        HistoricalModels.AnalyticsAnswer answer = new DeterministicAnswerProvider().answer(
                new HistoricalModels.AnalyticsQuestion(FIXTURE_DATASET_ID,
                        LocalDate.of(2026, 10, 13), LocalDate.of(2026, 10, 15),
                        "Which attraction had the worst uptime?"),
                summary);

        assertThat(answer.answer()).contains("Cypress Coil had the worst uptime at 86.81%");
        assertThat(answer.claims().getFirst().citations())
                .containsExactlyElementsOf(summary.attractions().getFirst().citations());
        assertThatThrownBy(() -> new DeterministicAnswerProvider().answer(
                new HistoricalModels.AnalyticsQuestion(FIXTURE_DATASET_ID,
                        LocalDate.of(2026, 10, 13), LocalDate.of(2026, 10, 15),
                        "What was the longest queue?"),
                summary)).isInstanceOf(IllegalArgumentException.class);
    }

    private static HistoricalModels.AvailabilityDetail detailFor(LocalDate date, String zone, LocalTime open,
                                                                 LocalTime close, boolean planned, String reason) {
        InMemoryHistoricalStore store = new InMemoryHistoricalStore();
        store.replace(new ReplayBundle(new Dataset("calendar", "1", 1, "checksum", date, date,
                DatasetState.COMPLETED, Instant.EPOCH, Instant.EPOCH),
                List.of(new OperatingDay("calendar", date, zone, open, close, 1)),
                List.of(new AttractionPlan("calendar", date, "cypress-coil", open, close, planned, reason, 1)),
                List.of(), List.of(), List.of()));
        return new AvailabilityProjectionService(store).detail("calendar", "cypress-coil", date);
    }

    private static InMemoryHistoricalStore dayStore(String datasetId, LocalDate date, List<Activity> activities) {
        InMemoryHistoricalStore store = new InMemoryHistoricalStore();
        store.replace(new ReplayBundle(new Dataset(datasetId, "1", 1, "checksum", date, date,
                DatasetState.COMPLETED, Instant.EPOCH, Instant.EPOCH),
                List.of(new OperatingDay(datasetId, date, "America/New_York",
                        LocalTime.of(9, 0), LocalTime.of(17, 0), 1)),
                List.of(new AttractionPlan(datasetId, date, "cypress-coil",
                        LocalTime.of(9, 0), LocalTime.of(17, 0), false, null, 1)),
                List.of(), activities, List.of()));
        return store;
    }

    private static Activity status(String datasetId, String id, Instant at, AttractionStatus previous,
                                   AttractionStatus next) {
        AttractionEventType type = next == AttractionStatus.OPERATING
                ? AttractionEventType.ATTRACTION_OPENED : AttractionEventType.ATTRACTION_CLOSED;
        return new Activity(datasetId, "source-" + id, "command-" + id, new AttractionStatusChanged(
                "activity-" + id, new AttractionId("cypress-coil"), type, "fixture", id, at,
                0, 1, previous, next));
    }
}
