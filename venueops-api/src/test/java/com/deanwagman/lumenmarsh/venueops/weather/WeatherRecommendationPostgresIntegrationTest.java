package com.deanwagman.lumenmarsh.venueops.weather;

import com.deanwagman.lumenmarsh.venueops.security.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "venueops.attractions.persistence=jpa",
        "venueops.attractions.seed=true",
        "spring.autoconfigure.exclude="
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class WeatherRecommendationPostgresIntegrationTest {

    private static final String RECOMMENDATION_ID = "cccccccc-dddd-eeee-ffff-000000000000";

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("venueops")
            .withUsername("venueops")
            .withPassword("venueops");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void persistsIdempotentIngestAndOperatorCommands() throws Exception {
        mockMvc.perform(post("/api/v1/integrations/weather/recommendations")
                        .with(TestAuth.weatherService())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("ACTIVE", "WARNING", 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(post("/api/v1/integrations/weather/recommendations")
                        .with(TestAuth.weatherService())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("ACTIVE", "WARNING", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(post("/api/v1/operator/weather/recommendations/" + RECOMMENDATION_ID + "/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"ACKNOWLEDGE\",\"expectedVersion\":99}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STALE_VERSION"));

        mockMvc.perform(post("/api/v1/operator/weather/recommendations/" + RECOMMENDATION_ID + "/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"ACKNOWLEDGE\",\"expectedVersion\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operatorStatus").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.version").value(2));

        mockMvc.perform(post("/api/v1/integrations/weather/recommendations")
                        .with(TestAuth.weatherService())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("CLEARED", "INFO", 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLEARED"))
                .andExpect(jsonPath("$.operatorStatus").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.sourceVersion").value(2));

        mockMvc.perform(get("/api/v1/operator/weather/recommendations/" + RECOMMENDATION_ID).with(TestAuth.operator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLEARED"))
                .andExpect(jsonPath("$.affectedAttractionIds").value(org.hamcrest.Matchers.containsInAnyOrder("mangrove-run", "cypress-coil")));
    }

    private static String body(String status, String severity, long version) {
        return """
                {
                  "recommendationId": "%s",
                  "ruleId": "simulated-lightning-hold",
                  "status": "%s",
                  "severity": "%s",
                  "summary": "Place Mangrove Run and Cypress Coil on weather hold",
                  "evidence": "Simulated lightning strike 1.2 miles from the western basin.",
                  "recommendedAction": "Place Mangrove Run and Cypress Coil on weather hold",
                  "affectedAttractionIds": ["mangrove-run", "cypress-coil"],
                  "observedAt": "2026-09-01T16:00:00Z",
                  "version": %d
                }
                """.formatted(RECOMMENDATION_ID, status, severity, version);
    }
}
