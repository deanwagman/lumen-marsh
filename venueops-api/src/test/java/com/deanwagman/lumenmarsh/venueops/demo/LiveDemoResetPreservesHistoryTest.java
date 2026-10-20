package com.deanwagman.lumenmarsh.venueops.demo;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "venueops.attractions.persistence=jpa",
        "venueops.attractions.seed=true",
        "venueops.historical.seed=true",
        "venueops.demo-sandbox.enabled=true",
        "venueops.demo-sandbox.internal-secret=0123456789abcdef0123456789abcdef",
        "venueops.demo-sandbox.secure-cookie=false",
        "venueops.security.issuer-uri=https://demo.localhost:8443",
        "spring.autoconfigure.exclude="
})
@AutoConfigureMockMvc
@EnabledIf("databaseAvailable")
class LiveDemoResetPreservesHistoryTest {
    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final String MARKER_DATASET = "preserved-marker";
    private static final PostgreSQLContainer POSTGRES = dockerAvailable()
            ? new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("venueops")
            .withUsername("venueops")
            .withPassword("venueops")
            : null;

    static {
        if (POSTGRES != null) {
            POSTGRES.start();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    @Qualifier("historicalFixtureSeedRunner")
    private ApplicationRunner historicalFixtureSeedRunner;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        if (POSTGRES != null) {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
            return;
        }
        registry.add("spring.datasource.url", () -> requiredEnv("VENUEOPS_RESET_TEST_JDBC_URL"));
        registry.add("spring.datasource.username", () ->
                envOrDefault("VENUEOPS_RESET_TEST_JDBC_USERNAME", "venueops"));
        registry.add("spring.datasource.password", () ->
                envOrDefault("VENUEOPS_RESET_TEST_JDBC_PASSWORD", "venueops"));
    }

    @BeforeAll
    static void databaseIsReachable() {
        assertThat(databaseAvailable()).isTrue();
    }

    @Test
    void liveResetLeavesHistoricalRowsAndPlayableAttractionStateIntact() throws Exception {
        assertThat(DemoResetService.truncateLiveTables())
                .doesNotContain("CASCADE")
                .doesNotContain("historical_");
        assertThat(DemoResetService.LIVE_TABLES).doesNotContainAnyElementsOf(DemoResetService.PRESERVED_TABLES);

        jdbc.update("DELETE FROM historical_dataset WHERE id = ?", MARKER_DATASET);
        insertPreservedMarker();
        List<String> historicalRows = historicalFingerprint();
        assertThat(historicalRows).anyMatch(row -> row.contains(MARKER_DATASET + "|marker-checksum|COMPLETED"));
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM historical_attraction_activity WHERE dataset_id <> ?",
                Integer.class, MARKER_DATASET)).isPositive();

        jdbc.update("""
                UPDATE attraction
                SET status = 'WEATHER_HOLD', capacity_mode = 'NORMAL', wait_minutes = 99, version = 4
                WHERE id = 'mangrove-run'
                """);
        jdbc.update("""
                INSERT INTO attraction_activity (
                    id, attraction_id, event_type, actor, actor_subject, actor_display_name,
                    actor_type, actor_issuer, occurred_at, previous_version, resulting_version, payload
                ) VALUES ('live-reset-marker', 'mangrove-run', 'WAIT_TIME_CHANGED', 'test',
                          'test', 'test', 'LEGACY', 'venueops', CURRENT_TIMESTAMP, 0, 1, '{}'::jsonb)
                """);

        String resetId = UUID.randomUUID().toString();
        mockMvc.perform(post("/internal/demo/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resetId\":\"" + resetId + "\"}"))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject(
                "SELECT wait_minutes FROM attraction WHERE id = 'mangrove-run'", Integer.class))
                .isEqualTo(99);

        mockMvc.perform(post("/internal/demo/reset")
                        .header("X-Demo-Reset-Secret", SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resetId\":\"" + resetId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.component").value("venueops"));

        assertThat(historicalFingerprint()).containsExactlyElementsOf(historicalRows);
        assertPlayableOpening();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM attraction_activity", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM maintenance_assets", Integer.class)).isEqualTo(5);

        mockMvc.perform(post("/internal/demo/reset")
                        .header("X-Demo-Reset-Secret", SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resetId\":\"" + resetId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REPLAY"));
        assertThat(historicalFingerprint()).containsExactlyElementsOf(historicalRows);
        assertPlayableOpening();
    }

    @Test
    void missingChecksumReloadsBundledFixtureWithoutChangingPlayableAttractions() throws Exception {
        List<String> playable = playableAttractionRows();
        Integer markerDatasets = jdbc.queryForObject(
                "SELECT count(*) FROM historical_dataset WHERE id = ?", Integer.class, MARKER_DATASET);

        jdbc.update(
                "DELETE FROM historical_dataset WHERE id = 'lumen-marsh-availability-demo-v1'");
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM historical_dataset WHERE id = 'lumen-marsh-availability-demo-v1'",
                Integer.class)).isZero();

        historicalFixtureSeedRunner.run(null);

        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM historical_dataset WHERE id = 'lumen-marsh-availability-demo-v1'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM historical_operating_calendar WHERE dataset_id = 'lumen-marsh-availability-demo-v1'",
                Integer.class)).isPositive();
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM historical_attraction_activity WHERE dataset_id = 'lumen-marsh-availability-demo-v1'",
                Integer.class)).isPositive();
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM historical_processed_command WHERE dataset_id = 'lumen-marsh-availability-demo-v1'",
                Integer.class)).isPositive();
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM historical_operating_evidence WHERE dataset_id = 'lumen-marsh-availability-demo-v1'",
                Integer.class)).isPositive();
        assertThat(DemoResetService.PRESERVED_TABLES).contains("historical_operating_evidence");
        assertThat(DemoResetService.LIVE_TABLES).doesNotContain("historical_operating_evidence");
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM historical_dataset WHERE id = ?", Integer.class, MARKER_DATASET))
                .isEqualTo(markerDatasets);
        assertThat(playableAttractionRows()).containsExactlyElementsOf(playable);
    }

    private void insertPreservedMarker() {
        jdbc.update("""
                INSERT INTO historical_dataset (
                    id, dataset_version, seed, manifest_checksum, from_date, to_date, state, created_at
                ) VALUES (?, '1', 1, 'marker-checksum', DATE '2026-01-01', DATE '2026-01-01',
                          'COMPLETED', CURRENT_TIMESTAMP)
                """, MARKER_DATASET);
        jdbc.update("""
                INSERT INTO historical_operating_calendar (
                    dataset_id, park_date, zone_id, scheduled_open, scheduled_close, calendar_version
                ) VALUES (?, DATE '2026-01-01', 'America/New_York', TIME '09:00', TIME '17:00', 1)
                """, MARKER_DATASET);
        jdbc.update("""
                INSERT INTO historical_attraction_plan (
                    dataset_id, park_date, attraction_id, expected_open, expected_close,
                    planned_closure, closure_reason, plan_version
                ) VALUES (?, DATE '2026-01-01', 'mangrove-run', TIME '09:00', TIME '17:00', false, null, 1)
                """, MARKER_DATASET);
        jdbc.update("""
                INSERT INTO historical_attraction_snapshot (
                    dataset_id, attraction_id, name, area, attraction_type, status, capacity_mode,
                    wait_minutes, updated_at, aggregate_version
                ) VALUES (?, 'mangrove-run', 'Mangrove Run', 'Luminous Wetlands', 'BOAT_EXPEDITION',
                          'OPERATING', 'NORMAL', 25, CURRENT_TIMESTAMP, 0)
                """, MARKER_DATASET);
        jdbc.update("""
                INSERT INTO historical_attraction_activity (
                    dataset_id, activity_id, source_id, command_id, attraction_id, event_type, actor,
                    occurred_at, previous_version, resulting_version
                ) VALUES (?, 'marker-activity', 'marker-source', 'marker-command', 'mangrove-run',
                          'ATTRACTION_OPENED', 'fixture', CURRENT_TIMESTAMP, 0, 1)
                """, MARKER_DATASET);
        jdbc.update("""
                INSERT INTO historical_processed_command (
                    dataset_id, command_id, attraction_id, event_type, resulting_version, occurred_at, result_json
                ) VALUES (?, 'marker-command', 'mangrove-run', 'OPEN', 1, CURRENT_TIMESTAMP, '{}'::jsonb)
                """, MARKER_DATASET);
    }

    private List<String> historicalFingerprint() {
        return List.of(
                "dataset|" + joined("""
                        SELECT id || '|' || manifest_checksum || '|' || state
                        FROM historical_dataset ORDER BY id
                        """),
                "calendar|" + joined("""
                        SELECT dataset_id || '|' || park_date::text
                        FROM historical_operating_calendar ORDER BY dataset_id, park_date
                        """),
                "plan|" + joined("""
                        SELECT dataset_id || '|' || park_date::text || '|' || attraction_id || '|' || plan_version
                        FROM historical_attraction_plan ORDER BY dataset_id, park_date, attraction_id
                        """),
                "snapshot|" + joined("""
                        SELECT dataset_id || '|' || attraction_id || '|' || status || '|' || aggregate_version
                        FROM historical_attraction_snapshot ORDER BY dataset_id, attraction_id
                        """),
                "activity|" + joined("""
                        SELECT dataset_id || '|' || activity_id || '|' || resulting_version
                        FROM historical_attraction_activity ORDER BY dataset_id, activity_id
                        """),
                "command|" + joined("""
                        SELECT dataset_id || '|' || command_id || '|' || resulting_version
                        FROM historical_processed_command ORDER BY dataset_id, command_id
                        """),
                "evidence|" + joined("""
                        SELECT dataset_id || '|' || evidence_id || '|' || episode_id || '|' || segment_activity_ids::text
                        FROM historical_operating_evidence ORDER BY dataset_id, evidence_id
                        """)
        );
    }

    private void assertPlayableOpening() {
        assertThat(playableAttractionRows()).containsExactly(
                "cypress-coil|CLOSED|NOT_APPLICABLE|null|0",
                "mangrove-run|OPERATING|NORMAL|25|0",
                "stormglass-station|CLOSED|NOT_APPLICABLE|null|0"
        );
        assertThat(jdbc.queryForObject("SELECT count(*) FROM attraction_experience", Integer.class)).isEqualTo(3);
    }

    private List<String> playableAttractionRows() {
        return jdbc.queryForList("""
                SELECT id || '|' || status || '|' || capacity_mode || '|' ||
                       coalesce(wait_minutes::text, 'null') || '|' || version
                FROM attraction ORDER BY id
                """, String.class);
    }

    private String joined(String sql) {
        return String.join(",", jdbc.queryForList(sql, String.class));
    }

    static boolean databaseAvailable() {
        return POSTGRES != null || System.getenv("VENUEOPS_RESET_TEST_JDBC_URL") != null;
    }

    private static boolean dockerAvailable() {
        try {
            Process process = new ProcessBuilder("docker", "info")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception unavailable) {
            return false;
        }
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required when Docker is unavailable");
        }
        return value;
    }

    private static String envOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
