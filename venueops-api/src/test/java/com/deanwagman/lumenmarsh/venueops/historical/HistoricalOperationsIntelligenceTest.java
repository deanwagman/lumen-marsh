package com.deanwagman.lumenmarsh.venueops.historical;

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
        assertThat(summary.attractions().getFirst().longestOutageMinutes()).isEqualTo(190);
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
        HistoricalReplayService replay = new HistoricalReplayService(store);

        assertThatThrownBy(() -> replay.replayFixture(FIXTURE_DATASET_ID, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("explicit replacement");

        assertThat(replay.replayFixture(FIXTURE_DATASET_ID, true).changed()).isTrue();
        assertThat(store.findDataset(FIXTURE_DATASET_ID).orElseThrow().version()).isEqualTo("1");
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
}
