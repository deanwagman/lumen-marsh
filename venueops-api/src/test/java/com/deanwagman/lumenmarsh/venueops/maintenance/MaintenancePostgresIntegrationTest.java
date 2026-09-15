package com.deanwagman.lumenmarsh.venueops.maintenance;

import com.deanwagman.lumenmarsh.venueops.maintenance.infrastructure.MaintenanceAssetSeedData;
import com.deanwagman.lumenmarsh.venueops.security.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
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
class MaintenancePostgresIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("venueops")
            .withUsername("venueops")
            .withPassword("venueops");

    @Autowired
    private MockMvc mockMvc;

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void persistsAssetsWorkOrdersAndIdempotentCommands() throws Exception {
        mockMvc.perform(get("/api/v1/operator/maintenance/assets")
                        .with(TestAuth.operator())
                        .queryParam("attractionId", "cypress-coil")
                        .queryParam("assetType", "COMPONENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].assetCode").value("CC-TRAIN-01-WHEEL-A"));

        UUID commandId = UUID.randomUUID();
        String createBody = """
                {
                  "commandId": "%s",
                  "assetId": "%s",
                  "classification": "CORRECTIVE",
                  "priority": "P3",
                  "summary": "Inspect wheel assembly"
                }
                """.formatted(commandId, MaintenanceAssetSeedData.wheelAId());
        MvcResult created = mockMvc.perform(post("/api/v1/operator/maintenance/work-orders")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workOrderNumber").value(startsWith("LM-")))
                .andExpect(jsonPath("$.version").value(1))
                .andReturn();
        String workOrderId = json.readTree(created.getResponse().getContentAsByteArray()).get("id").asString();

        mockMvc.perform(post("/api/v1/operator/maintenance/work-orders")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workOrderId));

        mockMvc.perform(post("/api/v1/operator/maintenance/work-orders/" + workOrderId + "/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "commandId": "%s",
                                  "type": "OPEN",
                                  "expectedVersion": 99
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STALE_VERSION"));

        mockMvc.perform(post("/api/v1/operator/maintenance/work-orders/" + workOrderId + "/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "commandId": "%s",
                                  "type": "OPEN",
                                  "expectedVersion": 1
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workOrder.status").value("OPEN"))
                .andExpect(jsonPath("$.workOrder.version").value(2));

        mockMvc.perform(post("/api/v1/operator/maintenance/work-orders")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workOrderId))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(get("/api/v1/operator/maintenance/work-orders/" + workOrderId + "/activity")
                        .with(TestAuth.operator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("WORK_ORDER_CREATED"))
                .andExpect(jsonPath("$[1].eventType").value("WORK_ORDER_OPENED"))
                .andExpect(jsonPath("$[1].sequence").value(2));

        mockMvc.perform(get("/api/v1/operator/maintenance/work-orders")
                        .with(TestAuth.operator())
                        .queryParam("attractionId", "cypress-coil")
                        .queryParam("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));

        String observationId = "vibration-pg-" + UUID.randomUUID();
        String ingestBody = """
                {
                  "observationId": "%s",
                  "observedAt": "2026-09-14T18:25:00Z",
                  "assetCode": "CC-TRAIN-01-WHEEL-A",
                  "signalType": "VIBRATION",
                  "severity": "WARNING",
                  "evidence": "Fictional simulated vibration.",
                  "recommendedAction": "Inspect the wheel assembly."
                }
                """.formatted(observationId);
        String recommendationId = json.readTree(mockMvc.perform(post("/api/v1/integrations/reliability/recommendations")
                        .with(TestAuth.reliabilityService())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ingestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsByteArray()).get("recommendationId").asString();
        mockMvc.perform(post("/api/v1/integrations/reliability/recommendations")
                        .with(TestAuth.reliabilityService())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ingestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicate").value(true));

        UUID acceptCommandId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/operator/maintenance/recommendations/" + recommendationId + "/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "commandId": "%s",
                                  "type": "ACCEPT",
                                  "expectedVersion": 99
                                }
                                """.formatted(acceptCommandId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STALE_VERSION"));
        mockMvc.perform(post("/api/v1/operator/maintenance/recommendations/" + recommendationId + "/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "commandId": "%s",
                                  "type": "ACCEPT",
                                  "expectedVersion": 1
                                }
                                """.formatted(acceptCommandId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WORK_ORDER_CREATED"))
                .andExpect(jsonPath("$.workOrderId").isNotEmpty());

        assertThat(workOrderId).isNotBlank();
    }
}
