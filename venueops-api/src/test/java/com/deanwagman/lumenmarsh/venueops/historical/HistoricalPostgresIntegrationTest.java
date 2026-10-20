package com.deanwagman.lumenmarsh.venueops.historical;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.FIXTURE_DATASET_ID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "venueops.attractions.persistence=jpa",
        "venueops.attractions.seed=true",
        "spring.autoconfigure.exclude="
})
@Testcontainers(disabledWithoutDocker = true)
class HistoricalPostgresIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("venueops")
            .withUsername("venueops")
            .withPassword("venueops");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private HistoricalReplayService replay;

    @Test
    void flywayStorageSeedsIdempotentlyWithoutChangingLiveOperations() {
        List<String> liveTables = List.of(
                "attraction",
                "attraction_activity",
                "attraction_processed_commands",
                "attraction_experience",
                "weather_recommendation",
                "weather_recommendation_activity",
                "weather_recommendation_attraction",
                "maintenance_recommendations",
                "maintenance_work_orders",
                "maintenance_activity",
                "maintenance_assets",
                "incident",
                "incident_activity",
                "flow_observation",
                "flow_recommendation",
                "flow_activity");
        List<Integer> liveCounts = liveTables.stream()
                .map(table -> jdbc.queryForObject("select count(*) from " + table, Integer.class))
                .toList();
        Integer liveAttractions = liveCounts.getFirst();
        Integer liveActivity = liveCounts.get(1);
        Integer historicalActivity = jdbc.queryForObject(
                "select count(*) from historical_attraction_activity where dataset_id = ?",
                Integer.class, FIXTURE_DATASET_ID);
        List<String> ids = jdbc.queryForList("""
                select activity_id from historical_attraction_activity
                where dataset_id = ? order by activity_id
                """, String.class, FIXTURE_DATASET_ID);

        HistoricalReplayService.ReplayResult result = replay.replayFixture(FIXTURE_DATASET_ID, false);

        assertThat(result.changed()).isFalse();
        assertThat(liveTables.stream()
                .map(table -> jdbc.queryForObject("select count(*) from " + table, Integer.class))
                .toList()).containsExactlyElementsOf(liveCounts);
        assertThat(jdbc.queryForObject("select count(*) from attraction", Integer.class))
                .isEqualTo(liveAttractions);
        assertThat(jdbc.queryForObject("select count(*) from attraction_activity", Integer.class))
                .isEqualTo(liveActivity);
        assertThat(jdbc.queryForObject("""
                select count(distinct episode_id) from historical_operating_evidence
                where dataset_id = ?
                """, Integer.class, FIXTURE_DATASET_ID)).isEqualTo(2);
        assertThat(jdbc.queryForObject("""
                select count(*) from historical_operating_evidence
                where dataset_id = ? and episode_id = 'episode-stormglass-weather-2026-10-14'
                """, Integer.class, FIXTURE_DATASET_ID)).isEqualTo(5);
        assertThat(jdbc.queryForObject("""
                select count(*) from historical_operating_evidence
                where dataset_id = ? and episode_id = 'episode-cypress-maintenance-2026-10-15'
                """, Integer.class, FIXTURE_DATASET_ID)).isEqualTo(6);
        assertThat(jdbc.queryForObject(
                "select count(*) from historical_attraction_activity where dataset_id = ?",
                Integer.class, FIXTURE_DATASET_ID)).isEqualTo(historicalActivity);
        assertThat(jdbc.queryForList("""
                select activity_id from historical_attraction_activity
                where dataset_id = ? order by activity_id
                """, String.class, FIXTURE_DATASET_ID)).containsExactlyElementsOf(ids);
        assertThat(historicalActivity).isPositive();
    }
}
