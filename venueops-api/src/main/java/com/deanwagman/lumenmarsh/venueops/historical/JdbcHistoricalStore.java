package com.deanwagman.lumenmarsh.venueops.historical;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionActivity;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionCapacityChanged;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionEventType;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatus;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatusChanged;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionType;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionWaitTimeChanged;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.CapacityMode;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.Activity;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AttractionPlan;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.Dataset;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.DatasetState;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.OperatingDay;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.ProcessedCommand;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.ReplayBundle;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.Snapshot;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "venueops.attractions.persistence", havingValue = "jpa")
public class JdbcHistoricalStore implements HistoricalStore {

    private final JdbcTemplate jdbc;

    public JdbcHistoricalStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Dataset> findDataset(String datasetId) {
        return jdbc.query("select * from historical_dataset where id = ?", this::dataset, datasetId)
                .stream().findFirst();
    }

    @Override
    public List<Dataset> listDatasets() {
        return jdbc.query("select * from historical_dataset order by created_at, id", this::dataset);
    }

    @Override
    @Transactional
    public void replace(ReplayBundle bundle) {
        String id = bundle.dataset().id();
        jdbc.update("delete from historical_dataset where id = ?", id);
        Dataset dataset = bundle.dataset();
        jdbc.update("""
                insert into historical_dataset
                (id, dataset_version, seed, manifest_checksum, from_date, to_date, state, created_at, completed_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, dataset.version(), dataset.seed(), dataset.manifestChecksum(), dataset.fromDate(),
                dataset.toDate(), dataset.state().name(), timestamptz(dataset.createdAt()),
                timestamptz(dataset.completedAt()));
        for (OperatingDay day : bundle.days()) {
            jdbc.update("""
                    insert into historical_operating_calendar
                    (dataset_id, park_date, zone_id, scheduled_open, scheduled_close, calendar_version)
                    values (?, ?, ?, ?, ?, ?)
                    """, id, day.parkDate(), day.zoneId(), day.scheduledOpen(), day.scheduledClose(), day.version());
        }
        for (AttractionPlan plan : bundle.plans()) {
            jdbc.update("""
                    insert into historical_attraction_plan
                    (dataset_id, park_date, attraction_id, expected_open, expected_close,
                     planned_closure, closure_reason, plan_version)
                    values (?, ?, ?, ?, ?, ?, ?, ?)
                    """, id, plan.parkDate(), plan.attractionId(), plan.expectedOpen(), plan.expectedClose(),
                    plan.plannedClosure(), plan.closureReason(), plan.version());
        }
        for (Snapshot snapshot : bundle.snapshots()) {
            jdbc.update("""
                    insert into historical_attraction_snapshot
                    (dataset_id, attraction_id, name, area, attraction_type, status, capacity_mode,
                     wait_minutes, updated_at, aggregate_version)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, id, snapshot.attractionId(), snapshot.name(), snapshot.area(), snapshot.type().name(),
                    snapshot.status().name(), snapshot.capacityMode().name(), snapshot.waitMinutes(),
                    timestamptz(snapshot.updatedAt()), snapshot.version());
        }
        for (Activity activity : bundle.activities()) {
            insertActivity(activity);
        }
        for (ProcessedCommand command : bundle.commands()) {
            jdbc.update("""
                    insert into historical_processed_command
                    (dataset_id, command_id, attraction_id, event_type, resulting_version, occurred_at, result_json)
                    values (?, ?, ?, ?, ?, ?, cast(? as jsonb))
                    """, id, command.commandId(), command.attractionId(), command.eventType(),
                    command.resultingVersion(), timestamptz(command.occurredAt()), command.resultJson());
        }
    }

    @Override
    public List<OperatingDay> days(String datasetId, LocalDate from, LocalDate to) {
        return jdbc.query("""
                select * from historical_operating_calendar
                where dataset_id = ? and park_date between ? and ? order by park_date
                """, (rs, row) -> new OperatingDay(rs.getString("dataset_id"),
                rs.getObject("park_date", LocalDate.class), rs.getString("zone_id"),
                rs.getObject("scheduled_open", java.time.LocalTime.class),
                rs.getObject("scheduled_close", java.time.LocalTime.class),
                rs.getInt("calendar_version")), datasetId, from, to);
    }

    @Override
    public List<AttractionPlan> plans(String datasetId, LocalDate from, LocalDate to) {
        return jdbc.query("""
                select * from historical_attraction_plan
                where dataset_id = ? and park_date between ? and ?
                order by park_date, attraction_id
                """, (rs, row) -> new AttractionPlan(rs.getString("dataset_id"),
                rs.getObject("park_date", LocalDate.class), rs.getString("attraction_id"),
                rs.getObject("expected_open", java.time.LocalTime.class),
                rs.getObject("expected_close", java.time.LocalTime.class),
                rs.getBoolean("planned_closure"), rs.getString("closure_reason"),
                rs.getInt("plan_version")), datasetId, from, to);
    }

    @Override
    public List<Snapshot> snapshots(String datasetId) {
        return jdbc.query("""
                select * from historical_attraction_snapshot where dataset_id = ? order by attraction_id
                """, (rs, row) -> new Snapshot(rs.getString("dataset_id"), rs.getString("attraction_id"),
                rs.getString("name"), rs.getString("area"), AttractionType.valueOf(rs.getString("attraction_type")),
                AttractionStatus.valueOf(rs.getString("status")),
                CapacityMode.valueOf(rs.getString("capacity_mode")), (Integer) rs.getObject("wait_minutes"),
                instant(rs, "updated_at"), rs.getLong("aggregate_version")), datasetId);
    }

    @Override
    public List<Activity> activities(String datasetId, String attractionId) {
        return jdbc.query("""
                select * from historical_attraction_activity
                where dataset_id = ? and attraction_id = ? order by occurred_at, resulting_version
                """, this::activity, datasetId, attractionId);
    }

    private Dataset dataset(ResultSet rs, int row) throws SQLException {
        return new Dataset(rs.getString("id"), rs.getString("dataset_version"), rs.getLong("seed"),
                rs.getString("manifest_checksum"), rs.getObject("from_date", LocalDate.class),
                rs.getObject("to_date", LocalDate.class), DatasetState.valueOf(rs.getString("state")),
                instant(rs, "created_at"), instant(rs, "completed_at"));
    }

    private void insertActivity(Activity stored) {
        AttractionActivity value = stored.value();
        String previousStatus = null;
        String newStatus = null;
        String previousMode = null;
        String newMode = null;
        Integer previousMinutes = null;
        Integer newMinutes = null;
        if (value instanceof AttractionStatusChanged changed) {
            previousStatus = changed.previousStatus().name();
            newStatus = changed.newStatus().name();
        } else if (value instanceof AttractionCapacityChanged changed) {
            previousMode = changed.previousMode().name();
            newMode = changed.newMode().name();
        } else if (value instanceof AttractionWaitTimeChanged changed) {
            previousMinutes = changed.previousMinutes();
            newMinutes = changed.newMinutes();
        }
        jdbc.update("""
                insert into historical_attraction_activity
                (dataset_id, activity_id, source_id, command_id, attraction_id, event_type, actor, reason,
                 occurred_at, previous_version, resulting_version, previous_status, new_status,
                 previous_capacity_mode, new_capacity_mode, previous_wait_minutes, new_wait_minutes)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, stored.datasetId(), value.id(), stored.sourceId(), stored.commandId(),
                value.attractionId().value(), value.type().name(), value.actor(), value.reason(),
                timestamptz(value.occurredAt()), value.previousVersion(), value.resultingVersion(),
                previousStatus, newStatus, previousMode, newMode, previousMinutes, newMinutes);
    }

    private Activity activity(ResultSet rs, int row) throws SQLException {
        AttractionEventType type = AttractionEventType.valueOf(rs.getString("event_type"));
        AttractionId id = new AttractionId(rs.getString("attraction_id"));
        String activityId = rs.getString("activity_id");
        AttractionActivity value;
        if (rs.getString("new_status") != null) {
            value = new AttractionStatusChanged(activityId, id, type, rs.getString("actor"),
                    rs.getString("reason"), instant(rs, "occurred_at"),
                    rs.getLong("previous_version"), rs.getLong("resulting_version"),
                    AttractionStatus.valueOf(rs.getString("previous_status")),
                    AttractionStatus.valueOf(rs.getString("new_status")));
        } else if (rs.getString("new_capacity_mode") != null) {
            value = new AttractionCapacityChanged(activityId, id, type, rs.getString("actor"),
                    rs.getString("reason"), instant(rs, "occurred_at"),
                    rs.getLong("previous_version"), rs.getLong("resulting_version"),
                    CapacityMode.valueOf(rs.getString("previous_capacity_mode")),
                    CapacityMode.valueOf(rs.getString("new_capacity_mode")));
        } else {
            value = new AttractionWaitTimeChanged(activityId, id, type, rs.getString("actor"),
                    rs.getString("reason"), instant(rs, "occurred_at"),
                    rs.getLong("previous_version"), rs.getLong("resulting_version"),
                    (Integer) rs.getObject("previous_wait_minutes"), (Integer) rs.getObject("new_wait_minutes"));
        }
        return new Activity(rs.getString("dataset_id"), rs.getString("source_id"),
                rs.getString("command_id"), value);
    }

    private static OffsetDateTime timestamptz(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
