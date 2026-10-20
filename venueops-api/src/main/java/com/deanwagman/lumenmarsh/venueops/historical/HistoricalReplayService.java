package com.deanwagman.lumenmarsh.venueops.historical;

import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionProcessedCommandRepository;
import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionRepository;
import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionService;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.Attraction;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionActivity;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionCommand;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionType;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.Activity;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.Evidence;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.EvidenceClassification;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AttractionPlan;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.Dataset;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.DatasetState;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.OperatingDay;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.ReplayBundle;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.Snapshot;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.FIXTURE_DATASET_ID;
import static com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.FIXTURE_VERSION;
import static com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.PARK_ZONE;

@Service
public class HistoricalReplayService {

    static final long FIXTURE_SEED = 20261020L;
    static final LocalDate FIXTURE_START = LocalDate.of(2026, 10, 13);
    static final String WEATHER_EPISODE_ID = "episode-stormglass-weather-2026-10-14";
    static final String MAINTENANCE_EPISODE_ID = "episode-cypress-maintenance-2026-10-15";
    static final String MANIFEST = """
            fixture=historical-operations-intelligence
            version=2
            timezone=America/New_York
            dates=2026-10-13..2026-10-15
            scenarios=normal,reduced-capacity,weather-recovery,cypress-technical-recovery
            evidence=stormglass-weather-2026-10-14,cypress-maintenance-2026-10-15
            """;

    private final HistoricalStore store;

    public HistoricalReplayService(HistoricalStore store) {
        this.store = store;
    }

    public ReplayResult replayBundledFixture() {
        return replayFixture(FIXTURE_DATASET_ID, true);
    }

    public ReplayResult replayFixture(String selectedDatasetId, boolean replaceSelectedDataset) {
        if (!FIXTURE_DATASET_ID.equals(selectedDatasetId)) {
            throw new IllegalArgumentException("The bundled fixture may only replace " + FIXTURE_DATASET_ID);
        }
        String checksum = sha256(MANIFEST);
        Optional<Dataset> existing = store.findDataset(selectedDatasetId);
        if (existing.isPresent() && existing.get().state() == DatasetState.COMPLETED
                && existing.get().manifestChecksum().equals(checksum)) {
            return new ReplayResult(selectedDatasetId, checksum, false);
        }
        if (existing.isPresent() && !replaceSelectedDataset) {
            throw new IllegalStateException("Dataset exists with a different manifest; explicit replacement is required");
        }
        ReplayBundle bundle = build(checksum);
        store.replace(bundle);
        return new ReplayResult(selectedDatasetId, checksum, true);
    }

    private ReplayBundle build(String checksum) {
        ZoneId zone = ZoneId.of(PARK_ZONE);
        Instant created = FIXTURE_START.atStartOfDay(zone).toInstant();
        SteppingClock clock = new SteppingClock(created);
        DeterministicAttractionRepository attractions =
                new DeterministicAttractionRepository(FIXTURE_DATASET_ID);
        CapturingProcessedCommands commands = new CapturingProcessedCommands();
        AttractionService service = new AttractionService(attractions, commands, clock, update -> {
        });

        attractions.add(Attraction.create(new AttractionId("mangrove-run"), "Mangrove Run",
                "Luminous Wetlands", AttractionType.BOAT_EXPEDITION, clock));
        attractions.add(Attraction.create(new AttractionId("stormglass-station"), "Stormglass Station",
                "Research Quarter", AttractionType.INDOOR_DARK_RIDE, clock));
        attractions.add(Attraction.create(new AttractionId("cypress-coil"), "Cypress Coil",
                "Cypress Basin", AttractionType.LAUNCH_COASTER, clock));

        int step = 0;
        for (int dayOffset = 0; dayOffset < 3; dayOffset++) {
            LocalDate date = FIXTURE_START.plusDays(dayOffset);
            for (String attractionId : List.of("mangrove-run", "stormglass-station", "cypress-coil")) {
                step = open(service, attractions, clock, zone, date, attractionId, step);
            }
            if (dayOffset == 0) {
                step = command(service, attractions, clock, zone, date, LocalTime.of(12, 0),
                        "mangrove-run", AttractionCommand.REDUCE_CAPACITY, "Reduced dispatch capacity", step);
                step = command(service, attractions, clock, zone, date, LocalTime.of(13, 0),
                        "mangrove-run", AttractionCommand.RESTORE_CAPACITY, "Normal dispatch restored", step);
            } else if (dayOffset == 1) {
                step = command(service, attractions, clock, zone, date, LocalTime.of(11, 0),
                        "stormglass-station", AttractionCommand.PLACE_WEATHER_HOLD,
                        "Lightning detected within operating radius", step);
                step = command(service, attractions, clock, zone, date, LocalTime.of(12, 0),
                        "stormglass-station", AttractionCommand.CLEAR_WEATHER_HOLD, "Weather all-clear", step);
                step = command(service, attractions, clock, zone, date, LocalTime.of(12, 15),
                        "stormglass-station", AttractionCommand.COMPLETE_TESTING, "Safety cycle completed", step);
                step = command(service, attractions, clock, zone, date, LocalTime.of(12, 20),
                        "stormglass-station", AttractionCommand.APPROVE_RETURN_TO_SERVICE,
                        "Operations approval", step);
            } else {
                step = command(service, attractions, clock, zone, date, LocalTime.of(10, 0),
                        "cypress-coil", AttractionCommand.REPORT_TECHNICAL_FAULT,
                        "Launch control feedback fault", step);
                step = command(service, attractions, clock, zone, date, LocalTime.of(12, 30),
                        "cypress-coil", AttractionCommand.COMPLETE_REPAIR, "Sensor replaced", step);
                step = command(service, attractions, clock, zone, date, LocalTime.of(13, 0),
                        "cypress-coil", AttractionCommand.COMPLETE_TESTING, "Verification cycles passed", step);
                step = command(service, attractions, clock, zone, date, LocalTime.of(13, 10),
                        "cypress-coil", AttractionCommand.APPROVE_RETURN_TO_SERVICE,
                        "Operations approval", step);
            }
            for (String attractionId : List.of("mangrove-run", "stormglass-station", "cypress-coil")) {
                step = command(service, attractions, clock, zone, date, LocalTime.of(17, 0),
                        attractionId, AttractionCommand.CLOSE_FOR_DAY, "Scheduled close", step);
            }
        }

        List<OperatingDay> days = new ArrayList<>();
        List<AttractionPlan> plans = new ArrayList<>();
        for (int offset = 0; offset < 3; offset++) {
            LocalDate date = FIXTURE_START.plusDays(offset);
            days.add(new OperatingDay(FIXTURE_DATASET_ID, date, PARK_ZONE,
                    LocalTime.of(9, 0), LocalTime.of(17, 0), 1));
            for (String id : List.of("mangrove-run", "stormglass-station", "cypress-coil")) {
                plans.add(new AttractionPlan(FIXTURE_DATASET_ID, date, id,
                        LocalTime.of(9, 0), LocalTime.of(17, 0), false, null, 1));
            }
        }
        Instant completed = ZonedDateTime.of(FIXTURE_START.plusDays(2), LocalTime.of(17, 1), zone).toInstant();
        Dataset dataset = new Dataset(FIXTURE_DATASET_ID, FIXTURE_VERSION, FIXTURE_SEED, checksum,
                FIXTURE_START, FIXTURE_START.plusDays(2), DatasetState.COMPLETED, created, completed);
        List<Activity> storedActivities = attractions.storedActivities();
        return new ReplayBundle(dataset, days, plans, attractions.snapshots(), storedActivities,
                commands.values().stream().map(value -> new HistoricalModels.ProcessedCommand(FIXTURE_DATASET_ID,
                        value.commandId().toString(), value.attractionId().value(), value.eventType().name(),
                        value.resultingVersion(), value.occurredAt(), value.resultJson())).toList(),
                evidence(storedActivities));
    }

    private static List<Evidence> evidence(List<Activity> activities) {
        LocalDate weatherDay = LocalDate.of(2026, 10, 14);
        LocalDate maintenanceDay = LocalDate.of(2026, 10, 15);
        Activity lightningHold = at(activities, "stormglass-station", weatherDay, LocalTime.of(11, 0));
        Activity allClear = at(activities, "stormglass-station", weatherDay, LocalTime.of(12, 0));
        Activity weatherTesting = at(activities, "stormglass-station", weatherDay, LocalTime.of(12, 15));
        Activity weatherReturn = at(activities, "stormglass-station", weatherDay, LocalTime.of(12, 20));
        Activity technicalDelay = at(activities, "cypress-coil", maintenanceDay, LocalTime.of(10, 0));
        Activity repair = at(activities, "cypress-coil", maintenanceDay, LocalTime.of(12, 30));
        Activity maintenanceTesting = at(activities, "cypress-coil", maintenanceDay, LocalTime.of(13, 0));
        Activity maintenanceReturn = at(activities, "cypress-coil", maintenanceDay, LocalTime.of(13, 10));
        return List.of(
                evidence("evidence-stormglass-01-lightning", WEATHER_EPISODE_ID, "stormglass-station", weatherDay,
                        EvidenceClassification.LIGHTNING_DETECTED,
                        "Lightning detected within the operating radius of Stormglass Station.",
                        lightningHold),
                evidence("evidence-stormglass-02-hold", WEATHER_EPISODE_ID, "stormglass-station", weatherDay,
                        EvidenceClassification.WEATHER_HOLD,
                        "Stormglass Station was placed on weather hold.",
                        lightningHold),
                evidence("evidence-stormglass-03-all-clear", WEATHER_EPISODE_ID, "stormglass-station", weatherDay,
                        EvidenceClassification.WEATHER_ALL_CLEAR,
                        "Weather all-clear. The hold was cleared.",
                        allClear),
                evidence("evidence-stormglass-04-testing", WEATHER_EPISODE_ID, "stormglass-station", weatherDay,
                        EvidenceClassification.TESTING,
                        "Safety cycle completed during return-to-service testing.",
                        weatherTesting),
                evidence("evidence-stormglass-05-return", WEATHER_EPISODE_ID, "stormglass-station", weatherDay,
                        EvidenceClassification.RETURN_TO_SERVICE,
                        "Operations approved return to service.",
                        weatherReturn),
                evidence("evidence-cypress-01-vibration", MAINTENANCE_EPISODE_ID, "cypress-coil", maintenanceDay,
                        EvidenceClassification.VIBRATION_RECOMMENDATION,
                        "A vibration recommendation was recorded for Cypress Coil.",
                        technicalDelay),
                evidence("evidence-cypress-02-accepted", MAINTENANCE_EPISODE_ID, "cypress-coil", maintenanceDay,
                        EvidenceClassification.ACCEPTED_WORK,
                        "Maintenance accepted the vibration recommendation.",
                        technicalDelay),
                evidence("evidence-cypress-03-technical-delay", MAINTENANCE_EPISODE_ID, "cypress-coil", maintenanceDay,
                        EvidenceClassification.TECHNICAL_DELAY,
                        "Launch control feedback fault placed Cypress Coil in a technical delay.",
                        technicalDelay),
                evidence("evidence-cypress-04-repair", MAINTENANCE_EPISODE_ID, "cypress-coil", maintenanceDay,
                        EvidenceClassification.REPAIR,
                        "The sensor was replaced and the repair was completed.",
                        repair),
                evidence("evidence-cypress-05-testing", MAINTENANCE_EPISODE_ID, "cypress-coil", maintenanceDay,
                        EvidenceClassification.TESTING,
                        "Verification cycles passed.",
                        maintenanceTesting),
                evidence("evidence-cypress-06-return", MAINTENANCE_EPISODE_ID, "cypress-coil", maintenanceDay,
                        EvidenceClassification.RETURN_TO_SERVICE,
                        "Operations approved return to service.",
                        maintenanceReturn)
        );
    }

    private static Evidence evidence(String id, String episodeId, String attractionId, LocalDate date,
                                     EvidenceClassification classification, String summary, Activity segment) {
        return new Evidence(id, FIXTURE_DATASET_ID, episodeId, attractionId, date, classification, summary,
                segment.value().occurredAt(), List.of(segment.value().id()));
    }

    private static Activity at(List<Activity> activities, String attractionId, LocalDate date, LocalTime time) {
        Instant expected = ZonedDateTime.of(date, time, ZoneId.of(PARK_ZONE)).toInstant();
        return activities.stream()
                .filter(activity -> activity.value().attractionId().value().equals(attractionId))
                .filter(activity -> activity.value().occurredAt().equals(expected))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing historical activity for " + attractionId + " at " + date + " " + time));
    }

    private static int open(AttractionService service, DeterministicAttractionRepository repository,
                            SteppingClock clock, ZoneId zone, LocalDate date, String attractionId, int step) {
        step = command(service, repository, clock, zone, date, LocalTime.of(8, 45),
                attractionId, AttractionCommand.START_TESTING, "Opening checks", step);
        step = command(service, repository, clock, zone, date, LocalTime.of(8, 55),
                attractionId, AttractionCommand.COMPLETE_TESTING, "Opening checks passed", step);
        return command(service, repository, clock, zone, date, LocalTime.of(9, 0),
                attractionId, AttractionCommand.APPROVE_RETURN_TO_SERVICE, "Open for guests", step);
    }

    private static int command(AttractionService service, DeterministicAttractionRepository repository,
                               SteppingClock clock, ZoneId zone, LocalDate date, LocalTime time,
                               String attractionId, AttractionCommand command, String reason, int step) {
        int nextStep = step + 1;
        clock.set(ZonedDateTime.of(date, time, zone).toInstant());
        UUID commandId = deterministicUuid("command", nextStep);
        repository.prepare(commandId.toString(), deterministicUuid("activity", nextStep).toString(),
                deterministicUuid("source", nextStep).toString());
        Attraction current = service.get(new AttractionId(attractionId));
        service.execute(current.id(), commandId, command, "Historical fixture", reason, null, current.version());
        return nextStep;
    }

    private static UUID deterministicUuid(String kind, int step) {
        return UUID.nameUUIDFromBytes((FIXTURE_SEED + ":" + kind + ":" + step)
                .getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public record ReplayResult(String datasetId, String manifestChecksum, boolean changed) {
    }

    private static final class CapturingProcessedCommands implements AttractionProcessedCommandRepository {
        private final Map<UUID, ProcessedCommandRepositoryValue> values = new LinkedHashMap<>();

        @Override
        public Optional<AttractionProcessedCommandRepository.ProcessedCommand> findByCommandId(UUID commandId) {
            ProcessedCommandRepositoryValue value = values.get(commandId);
            return value == null ? Optional.empty() : Optional.of(value.command());
        }

        @Override
        public void save(AttractionProcessedCommandRepository.ProcessedCommand command) {
            values.put(command.commandId(), new ProcessedCommandRepositoryValue(command));
        }

        List<AttractionProcessedCommandRepository.ProcessedCommand> values() {
            return values.values().stream().map(ProcessedCommandRepositoryValue::command).toList();
        }

        private record ProcessedCommandRepositoryValue(
                AttractionProcessedCommandRepository.ProcessedCommand command
        ) {
        }
    }

    private static final class DeterministicAttractionRepository implements AttractionRepository {
        private final String datasetId;
        private final Map<AttractionId, Attraction> values = new LinkedHashMap<>();
        private final List<Activity> activities = new ArrayList<>();
        private String nextCommandId;
        private String nextActivityId;
        private String nextSourceId;

        private DeterministicAttractionRepository(String datasetId) {
            this.datasetId = datasetId;
        }

        void add(Attraction attraction) {
            values.put(attraction.id(), copy(attraction));
        }

        void prepare(String commandId, String activityId, String sourceId) {
            nextCommandId = commandId;
            nextActivityId = activityId;
            nextSourceId = sourceId;
        }

        @Override
        public Optional<Attraction> findById(AttractionId id) {
            return Optional.ofNullable(values.get(id)).map(this::copy);
        }

        @Override
        public List<Attraction> findAll() {
            return values.values().stream().map(this::copy)
                    .sorted(Comparator.comparing(value -> value.id().value())).toList();
        }

        @Override
        public void save(Attraction attraction) {
            for (AttractionActivity activity : attraction.uncommittedActivity()) {
                activities.add(new Activity(datasetId, nextSourceId, nextCommandId, activity));
            }
            values.put(attraction.id(), copy(attraction));
            attraction.markActivityCommitted();
        }

        List<Activity> storedActivities() {
            return List.copyOf(activities);
        }

        List<Snapshot> snapshots() {
            return values.values().stream().map(value -> new Snapshot(datasetId, value.id().value(),
                    value.name(), value.area(), value.type(), value.status(), value.capacityMode(),
                    value.waitMinutes(), value.updatedAt(), value.version()))
                    .sorted(Comparator.comparing(Snapshot::attractionId)).toList();
        }

        private Attraction copy(Attraction value) {
            Supplier<String> idSupplier = () -> {
                if (nextActivityId == null) {
                    throw new IllegalStateException("Historical activity ID was not prepared");
                }
                String id = nextActivityId;
                nextActivityId = null;
                return id;
            };
            return Attraction.rehydrate(value.id(), value.name(), value.area(), value.type(), value.status(),
                    value.capacityMode(), value.waitMinutes(), value.updatedAt(), value.version(),
                    value.activity(), idSupplier);
        }
    }
}
