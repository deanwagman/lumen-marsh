package com.deanwagman.lumenmarsh.venueops.historical;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionActivity;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatus;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionType;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.CapacityMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public final class HistoricalModels {

    public static final String FIXTURE_DATASET_ID = "lumen-marsh-availability-demo-v1";
    public static final String FIXTURE_VERSION = "1";
    public static final String PARK_ZONE = "America/New_York";

    private HistoricalModels() {
    }

    public enum DatasetState {
        LOADING, COMPLETED, FAILED
    }

    public record Dataset(
            String id,
            String version,
            long seed,
            String manifestChecksum,
            LocalDate fromDate,
            LocalDate toDate,
            DatasetState state,
            Instant createdAt,
            Instant completedAt
    ) {
    }

    public record OperatingDay(
            String datasetId,
            LocalDate parkDate,
            String zoneId,
            LocalTime scheduledOpen,
            LocalTime scheduledClose,
            int version
    ) {
    }

    public record AttractionPlan(
            String datasetId,
            LocalDate parkDate,
            String attractionId,
            LocalTime expectedOpen,
            LocalTime expectedClose,
            boolean plannedClosure,
            String closureReason,
            int version
    ) {
    }

    public record Snapshot(
            String datasetId,
            String attractionId,
            String name,
            String area,
            AttractionType type,
            AttractionStatus status,
            CapacityMode capacityMode,
            Integer waitMinutes,
            Instant updatedAt,
            long version
    ) {
    }

    public record Activity(
            String datasetId,
            String sourceId,
            String commandId,
            AttractionActivity value
    ) {
    }

    public record ProcessedCommand(
            String datasetId,
            String commandId,
            String attractionId,
            String eventType,
            long resultingVersion,
            Instant occurredAt,
            String resultJson
    ) {
    }

    public record ReplayBundle(
            Dataset dataset,
            List<OperatingDay> days,
            List<AttractionPlan> plans,
            List<Snapshot> snapshots,
            List<Activity> activities,
            List<ProcessedCommand> commands
    ) {
    }

    public enum AvailabilityClassification {
        OPERATING,
        REDUCED_CAPACITY,
        WEATHER_HOLD,
        TECHNICAL_DELAY,
        TESTING_RETURN_TO_SERVICE,
        COMMANDED_CLOSURE,
        CLOSED_NOT_OPEN
    }

    public record Citation(
            String id,
            String datasetId,
            String attractionId,
            LocalDate parkDate,
            String activityId,
            String commandId,
            String controlTowerUrl
    ) {
    }

    public record AvailabilitySegment(
            String citationId,
            AttractionStatus status,
            AvailabilityClassification classification,
            Instant start,
            Instant end,
            long durationMinutes,
            String reason,
            String commandId,
            String activityId,
            String datasetId,
            String controlTowerUrl
    ) {
    }

    public record AttractionAvailability(
            String attractionId,
            String attractionName,
            long scheduledMinutes,
            long operatingMinutes,
            BigDecimal uptimePercentage,
            long degradedMinutes,
            Map<AvailabilityClassification, Long> downtimeMinutes,
            long longestOutageMinutes,
            List<Citation> citations
    ) {
    }

    public record AvailabilitySummary(
            String datasetId,
            LocalDate from,
            LocalDate to,
            String zoneId,
            List<AttractionAvailability> attractions
    ) {
    }

    public record AvailabilityDetail(
            String datasetId,
            String attractionId,
            String attractionName,
            LocalDate parkDate,
            String zoneId,
            Instant expectedStart,
            Instant expectedEnd,
            boolean plannedClosure,
            String plannedClosureReason,
            long scheduledMinutes,
            long operatingMinutes,
            BigDecimal uptimePercentage,
            long degradedMinutes,
            Map<AvailabilityClassification, Long> downtimeMinutes,
            long longestOutageMinutes,
            List<AvailabilitySegment> segments
    ) {
    }

    public record AnalyticsQuestion(
            String datasetId,
            LocalDate from,
            LocalDate to,
            String question
    ) {
    }

    public record AnswerClaim(String text, List<Citation> citations) {
    }

    public record AnalyticsAnswer(
            String datasetId,
            String answer,
            List<AnswerClaim> claims,
            String provider
    ) {
    }
}
