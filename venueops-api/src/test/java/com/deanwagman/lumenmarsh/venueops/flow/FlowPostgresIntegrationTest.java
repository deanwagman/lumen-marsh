package com.deanwagman.lumenmarsh.venueops.flow;

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

import java.time.Instant;
import java.util.UUID;

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
class FlowPostgresIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("venueops")
            .withUsername("venueops")
            .withPassword("venueops");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void flywayPersistsObservationProjectionAndRecommendation() throws Exception {
        UUID observationId = UUID.fromString("99999999-aaaa-4bbb-8ccc-dddddddddddd");
        UUID recommendationId = UUID.fromString("88888888-aaaa-4bbb-8ccc-dddddddddddd");
        String observedAt = Instant.now().minusSeconds(20).toString();
        mockMvc.perform(post("/api/v1/integrations/flow/observations")
                        .with(TestAuth.flowService())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "observationId": "%s",
                                  "attractionId": "mangrove-run",
                                  "observedAt": "%s",
                                  "windowSeconds": 60,
                                  "queueLength": 120,
                                  "arrivals": 14,
                                  "boarded": 10,
                                  "operatingUnits": 6,
                                  "configuredUnits": 8,
                                  "sourceType": "SIMULATOR",
                                  "simulated": true
                                }
                                """.formatted(observationId, observedAt)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/integrations/flow/forecasts")
                        .with(TestAuth.flowService())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "attractionId": "mangrove-run",
                                  "generatedAt": "%s",
                                  "basedOnObservationId": "%s",
                                  "simulated": true,
                                  "forecasts": [
                                    {"forecastId":"%s","horizonMinutes":15,"predictedQueueLength":130,"predictedWaitMinutes":22,"confidence":"HIGH","assumptions":["Fresh samples"],"explanation":"Stable throughput."},
                                    {"forecastId":"%s","horizonMinutes":30,"predictedQueueLength":150,"predictedWaitMinutes":28,"confidence":"MEDIUM","assumptions":["Fresh samples"],"explanation":"Slight rise."},
                                    {"forecastId":"%s","horizonMinutes":60,"predictedQueueLength":180,"predictedWaitMinutes":36,"confidence":"LOW","assumptions":["Fresh samples"],"explanation":"Longer horizon."}
                                  ],
                                  "recommendation": {
                                    "recommendationId": "%s",
                                    "type": "POSTED_WAIT_REVIEW",
                                    "severity": "INFO",
                                    "sourceAttractionId": "mangrove-run",
                                    "affectedAttractionIds": ["mangrove-run"],
                                    "recommendedDestinationIds": ["cypress-coil"],
                                    "summary": "Posted wait may lag",
                                    "explanation": "Calculated wait is rising ahead of posted wait."
                                  }
                                }
                                """.formatted(
                                Instant.now(),
                                observationId,
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                recommendationId
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recommendationAccepted").value(true));

        mockMvc.perform(get("/api/v1/operator/flow/recommendations/" + recommendationId).with(TestAuth.operator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendation.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.simulated").value(true));
    }
}
