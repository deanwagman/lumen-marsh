package com.deanwagman.lumenmarsh.venueops.demo;

import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionExperienceRepository;
import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionRepository;
import com.deanwagman.lumenmarsh.venueops.attraction.infrastructure.AttractionSeedData;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceAssetRepository;
import com.deanwagman.lumenmarsh.venueops.maintenance.infrastructure.MaintenanceAssetSeedData;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

final class DemoResetService {
    enum Outcome { COMPLETED, REPLAY }

    private final DemoSandboxProperties properties;
    private final RestClient rest;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final EntityManager entityManager;
    private final AttractionRepository attractions;
    private final AttractionExperienceRepository experiences;
    private final MaintenanceAssetRepository assets;
    private final Clock clock;

    DemoResetService(
            DemoSandboxProperties properties,
            RestClient rest,
            JdbcTemplate jdbc,
            TransactionTemplate transactions,
            EntityManager entityManager,
            AttractionRepository attractions,
            AttractionExperienceRepository experiences,
            MaintenanceAssetRepository assets,
            Clock clock
    ) {
        this.properties = properties;
        this.rest = rest;
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.entityManager = entityManager;
        this.attractions = attractions;
        this.experiences = experiences;
        this.assets = assets;
        this.clock = clock;
    }

    Map<String, String> resetAll(UUID resetId) {
        Map<String, String> outcomes = new LinkedHashMap<>();
        outcomes.put("environmental", resetRemote(properties.environmentalUrl(), resetId));
        outcomes.put("flow", resetRemote(properties.flowUrl(), resetId));
        outcomes.put("reliability", resetRemote(properties.reliabilityUrl(), resetId));
        outcomes.put("venueops", resetVenueOps(resetId).name());
        return outcomes;
    }

    Outcome resetVenueOps(UUID resetId) {
        return transactions.execute(status -> resetVenueOpsInTransaction(resetId));
    }

    private Outcome resetVenueOpsInTransaction(UUID resetId) {
        String existing = jdbc.query(
                "SELECT status FROM demo_reset_execution WHERE reset_id=? AND component='venueops'",
                result -> result.next() ? result.getString(1) : null,
                resetId.toString()
        );
        if ("COMPLETED".equals(existing)) {
            return Outcome.REPLAY;
        }
        jdbc.update("""
                INSERT INTO demo_reset_execution(reset_id, component, status)
                VALUES (?, 'venueops', 'STARTED')
                ON CONFLICT (reset_id, component) DO UPDATE SET status='STARTED', completed_at=NULL
                """, resetId.toString());

        entityManager.clear();
        jdbc.execute("""
                TRUNCATE TABLE
                  flow_activity, flow_recommendation_destination, flow_processed_commands,
                  flow_recommendation, queue_forecast, queue_projection, flow_observation,
                  maintenance_work_order_evidence, maintenance_checklist_items, maintenance_activity,
                  maintenance_processed_commands, maintenance_recommendations, maintenance_work_orders,
                  maintenance_work_order_sequence, maintenance_assets,
                  weather_recommendation_activity, weather_recommendation_attraction, weather_recommendation,
                  incident_activity, incident_attraction, incident_processed_commands, incident,
                  attraction_activity, attraction_processed_commands, attraction_experience, attraction
                RESTART IDENTITY CASCADE
                """);
        entityManager.clear();
        AttractionSeedData.attractions(clock).forEach(attractions::save);
        AttractionSeedData.experienceProfiles().forEach(experiences::save);
        MaintenanceAssetSeedData.assets(clock).forEach(assets::save);
        entityManager.flush();
        jdbc.update("""
                UPDATE demo_reset_execution SET status='COMPLETED', completed_at=?
                WHERE reset_id=? AND component='venueops'
                """, Instant.now(clock), resetId.toString());
        return Outcome.COMPLETED;
    }

    private String resetRemote(String baseUrl, UUID resetId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> body = rest.post()
                .uri(baseUrl + "/internal/demo/reset")
                .header("X-Demo-Reset-Secret", properties.internalSecret())
                .header(HttpHeaders.ACCEPT, "application/json")
                .body(Map.of("resetId", resetId.toString()))
                .retrieve()
                .body(Map.class);
        String status = body == null ? null : String.valueOf(body.get("status"));
        if (!"COMPLETED".equals(status) && !"REPLAY".equals(status)) {
            throw new IllegalStateException("Reset returned an invalid status.");
        }
        return status;
    }
}
