package com.deanwagman.lumenmarsh.venueops.historical;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionActivity;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionCapacityChanged;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatus;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatusChanged;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.CapacityMode;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.Activity;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AttractionAvailability;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AttractionPlan;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AvailabilityClassification;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AvailabilityDetail;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AvailabilitySegment;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AvailabilitySummary;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.Citation;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.OperatingDay;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.Snapshot;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AvailabilityProjectionService {

    private final HistoricalStore store;

    public AvailabilityProjectionService(HistoricalStore store) {
        this.store = store;
    }

    public AvailabilitySummary summary(String datasetId, LocalDate from, LocalDate to) {
        requireRange(from, to);
        store.findDataset(datasetId).orElseThrow(() -> new HistoricalDatasetNotFoundException(datasetId));
        List<Snapshot> snapshots = store.snapshots(datasetId);
        Map<String, List<AvailabilityDetail>> details = new LinkedHashMap<>();
        for (AttractionPlan plan : store.plans(datasetId, from, to)) {
            if (!plan.plannedClosure()) {
                details.computeIfAbsent(plan.attractionId(), ignored -> new ArrayList<>())
                        .add(detail(datasetId, plan.attractionId(), plan.parkDate()));
            }
        }
        Map<String, Snapshot> snapshotById = new LinkedHashMap<>();
        snapshots.forEach(snapshot -> snapshotById.put(snapshot.attractionId(), snapshot));
        List<AttractionAvailability> totals = details.entrySet().stream().map(entry -> {
            long scheduled = entry.getValue().stream().mapToLong(AvailabilityDetail::scheduledMinutes).sum();
            long operating = entry.getValue().stream().mapToLong(AvailabilityDetail::operatingMinutes).sum();
            long degraded = entry.getValue().stream().mapToLong(AvailabilityDetail::degradedMinutes).sum();
            long longest = entry.getValue().stream().mapToLong(AvailabilityDetail::longestOutageMinutes).max().orElse(0);
            Map<AvailabilityClassification, Long> downtime = emptyDowntime();
            entry.getValue().forEach(detail -> detail.downtimeMinutes()
                    .forEach((key, value) -> downtime.merge(key, value, Long::sum)));
            List<Citation> citations = entry.getValue().stream().flatMap(detail -> detail.segments().stream())
                    .map(this::citation).toList();
            Snapshot snapshot = snapshotById.get(entry.getKey());
            return new AttractionAvailability(entry.getKey(),
                    snapshot == null ? entry.getKey() : snapshot.name(), scheduled, operating,
                    percentage(operating, scheduled), degraded, Map.copyOf(downtime), longest, citations);
        }).sorted(Comparator.comparing(AttractionAvailability::uptimePercentage)
                .thenComparing(AttractionAvailability::attractionId)).toList();
        return new AvailabilitySummary(datasetId, from, to,
                store.days(datasetId, from, to).stream().findFirst().map(OperatingDay::zoneId)
                        .orElse(HistoricalModels.PARK_ZONE), totals);
    }

    public AvailabilityDetail detail(String datasetId, String attractionId, LocalDate date) {
        store.findDataset(datasetId).orElseThrow(() -> new HistoricalDatasetNotFoundException(datasetId));
        AttractionPlan plan = store.plans(datasetId, date, date).stream()
                .filter(value -> value.attractionId().equals(attractionId)).findFirst()
                .orElseThrow(() -> new HistoricalPlanNotFoundException(datasetId, attractionId, date));
        OperatingDay day = store.days(datasetId, date, date).stream().findFirst()
                .orElseThrow(() -> new HistoricalPlanNotFoundException(datasetId, attractionId, date));
        String name = store.snapshots(datasetId).stream()
                .filter(value -> value.attractionId().equals(attractionId)).map(Snapshot::name)
                .findFirst().orElse(attractionId);
        ZoneId zone = ZoneId.of(day.zoneId());
        Instant start = boundary(date, plan.expectedOpen(), zone);
        Instant end = boundary(date, plan.expectedClose(), zone);
        if (!end.isAfter(start)) {
            end = boundary(date.plusDays(1), plan.expectedClose(), zone);
        }
        Instant expectedEnd = end;
        if (plan.plannedClosure()) {
            return new AvailabilityDetail(datasetId, attractionId, name, date, day.zoneId(), start, end,
                    true, plan.closureReason(), 0, 0, BigDecimal.ZERO.setScale(2), 0,
                    Map.copyOf(emptyDowntime()), 0, List.of());
        }
        List<Activity> all = store.activities(datasetId, attractionId);
        State state = stateAt(all, start);
        List<Activity> window = all.stream()
                .filter(value -> !value.value().occurredAt().isBefore(start)
                        && value.value().occurredAt().isBefore(expectedEnd))
                .toList();
        boolean opened = state.status() == AttractionStatus.OPERATING || window.stream()
                .anyMatch(value -> value.value() instanceof AttractionStatusChanged changed
                        && changed.newStatus() == AttractionStatus.OPERATING);
        List<AvailabilitySegment> segments;
        if (!opened) {
            segments = List.of(segment(datasetId, attractionId, date, start, end, state.status(),
                    AvailabilityClassification.CLOSED_NOT_OPEN, "Attraction did not open", null, null));
        } else {
            segments = fold(datasetId, attractionId, date, start, end, state, window);
        }
        long scheduled = Duration.between(start, end).toMinutes();
        long operating = segments.stream().filter(this::isOperating)
                .mapToLong(AvailabilitySegment::durationMinutes).sum();
        long degraded = segments.stream()
                .filter(value -> value.classification() == AvailabilityClassification.REDUCED_CAPACITY)
                .mapToLong(AvailabilitySegment::durationMinutes).sum();
        Map<AvailabilityClassification, Long> downtime = emptyDowntime();
        segments.stream().filter(value -> !isOperating(value))
                .forEach(value -> downtime.merge(value.classification(), value.durationMinutes(), Long::sum));
        return new AvailabilityDetail(datasetId, attractionId, name, date, day.zoneId(), start, end,
                false, null, scheduled, operating, percentage(operating, scheduled), degraded,
                Map.copyOf(downtime), longestOutage(segments), segments);
    }

    private List<AvailabilitySegment> fold(String datasetId, String attractionId, LocalDate date,
                                           Instant start, Instant end, State initial, List<Activity> activities) {
        List<AvailabilitySegment> result = new ArrayList<>();
        State state = initial;
        Instant cursor = start;
        for (Activity activity : activities) {
            Instant eventTime = activity.value().occurredAt();
            if (eventTime.isAfter(cursor)) {
                result.add(segment(datasetId, attractionId, date, cursor, eventTime, state.status(),
                        classification(state.status(), state.capacityMode()), state.reason(),
                        state.commandId(), state.activityId()));
            }
            state = apply(state, activity);
            cursor = eventTime;
        }
        if (cursor.isBefore(end)) {
            result.add(segment(datasetId, attractionId, date, cursor, end, state.status(),
                    classification(state.status(), state.capacityMode()), state.reason(),
                    state.commandId(), state.activityId()));
        }
        return result;
    }

    private static State stateAt(List<Activity> activities, Instant instant) {
        State state = new State(AttractionStatus.CLOSED, CapacityMode.NOT_APPLICABLE, null, null, null);
        for (Activity activity : activities) {
            if (activity.value().occurredAt().isAfter(instant)) {
                break;
            }
            state = apply(state, activity);
        }
        return state;
    }

    private static State apply(State state, Activity stored) {
        AttractionActivity activity = stored.value();
        AttractionStatus status = state.status();
        CapacityMode mode = state.capacityMode();
        if (activity instanceof AttractionStatusChanged changed) {
            status = changed.newStatus();
            mode = status == AttractionStatus.OPERATING ? CapacityMode.NORMAL : CapacityMode.NOT_APPLICABLE;
        } else if (activity instanceof AttractionCapacityChanged changed) {
            mode = changed.newMode();
        }
        return new State(status, mode, activity.reason(), stored.commandId(), activity.id());
    }

    private AvailabilitySegment segment(String datasetId, String attractionId, LocalDate date,
                                        Instant start, Instant end, AttractionStatus status,
                                        AvailabilityClassification classification, String reason,
                                        String commandId, String activityId) {
        String citationId = "segment-" + shortHash(datasetId + "|" + attractionId + "|" + date + "|"
                + start + "|" + end + "|" + classification + "|" + activityId);
        String url = "/history?datasetId=" + datasetId + "&date=" + date + "&attractionId="
                + attractionId + "&citation=" + citationId;
        return new AvailabilitySegment(citationId, status, classification, start, end,
                Duration.between(start, end).toMinutes(), reason, commandId, activityId, datasetId, url);
    }

    private Citation citation(AvailabilitySegment segment) {
        String[] parts = segment.controlTowerUrl().split("date=");
        LocalDate date = LocalDate.parse(parts[1].substring(0, 10));
        return new Citation(segment.citationId(), segment.datasetId(),
                segment.controlTowerUrl().replaceAll(".*attractionId=([^&]+).*", "$1"), date,
                segment.activityId(), segment.commandId(), segment.controlTowerUrl());
    }

    private static AvailabilityClassification classification(AttractionStatus status, CapacityMode mode) {
        return switch (status) {
            case OPERATING -> mode == CapacityMode.REDUCED
                    ? AvailabilityClassification.REDUCED_CAPACITY : AvailabilityClassification.OPERATING;
            case WEATHER_HOLD -> AvailabilityClassification.WEATHER_HOLD;
            case TECHNICAL_DELAY -> AvailabilityClassification.TECHNICAL_DELAY;
            case TESTING, RETURNING_TO_SERVICE -> AvailabilityClassification.TESTING_RETURN_TO_SERVICE;
            case CLOSED -> AvailabilityClassification.COMMANDED_CLOSURE;
        };
    }

    private boolean isOperating(AvailabilitySegment segment) {
        return segment.classification() == AvailabilityClassification.OPERATING
                || segment.classification() == AvailabilityClassification.REDUCED_CAPACITY;
    }

    private long longestOutage(List<AvailabilitySegment> segments) {
        long longest = 0;
        long current = 0;
        for (AvailabilitySegment segment : segments) {
            if (isOperating(segment)) {
                current = 0;
            } else {
                current += segment.durationMinutes();
                longest = Math.max(longest, current);
            }
        }
        return longest;
    }

    private static Map<AvailabilityClassification, Long> emptyDowntime() {
        Map<AvailabilityClassification, Long> result = new EnumMap<>(AvailabilityClassification.class);
        for (AvailabilityClassification value : AvailabilityClassification.values()) {
            if (value != AvailabilityClassification.OPERATING
                    && value != AvailabilityClassification.REDUCED_CAPACITY) {
                result.put(value, 0L);
            }
        }
        return result;
    }

    private static BigDecimal percentage(long numerator, long denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private static Instant boundary(LocalDate date, LocalTime time, ZoneId zone) {
        return ZonedDateTime.of(date, time, zone).toInstant();
    }

    private static void requireRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new IllegalArgumentException("from and to must define an inclusive date range");
        }
    }

    private static String shortHash(String value) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest, 0, 10);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private record State(
            AttractionStatus status,
            CapacityMode capacityMode,
            String reason,
            String commandId,
            String activityId
    ) {
    }
}
