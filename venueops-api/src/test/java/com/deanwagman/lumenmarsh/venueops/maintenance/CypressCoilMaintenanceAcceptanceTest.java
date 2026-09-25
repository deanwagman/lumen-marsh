package com.deanwagman.lumenmarsh.venueops.maintenance;

import com.deanwagman.lumenmarsh.venueops.maintenance.infrastructure.MaintenanceAssetSeedData;
import com.deanwagman.lumenmarsh.venueops.security.TestAuth;
import com.deanwagman.lumenmarsh.venueops.testsupport.CommandJson;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "venueops.attractions.seed=true")
@AutoConfigureMockMvc
@DirtiesContext
class CypressCoilMaintenanceAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void vibrationRecommendationBecomesReadyForTestingWithoutReopeningTheAttraction() throws Exception {
        mockMvc.perform(get("/api/v1/operator/maintenance/assets")
                        .with(TestAuth.operator())
                        .queryParam("attractionId", "cypress-coil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5));

        JsonNode ingested = json.readTree(mockMvc.perform(post("/api/v1/integrations/reliability/recommendations")
                        .with(TestAuth.reliabilityService())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vibrationBody("vibration-cc-train-01-20260914T182500Z")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.duplicate").value(false))
                .andReturn()
                .getResponse()
                .getContentAsByteArray());
        String recommendationId = ingested.get("recommendationId").asString();

        mockMvc.perform(post("/api/v1/integrations/reliability/recommendations")
                        .with(TestAuth.reliabilityService())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vibrationBody("vibration-cc-train-01-20260914T182500Z")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicate").value(true))
                .andExpect(jsonPath("$.recommendationId").value(recommendationId));

        UUID acceptCommandId = UUID.randomUUID();
        JsonNode accepted = json.readTree(mockMvc.perform(post("/api/v1/operator/maintenance/recommendations/"
                                + recommendationId + "/commands")
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
                .andReturn()
                .getResponse()
                .getContentAsByteArray());
        String workOrderId = accepted.get("workOrderId").asString();
        long recommendationVersion = accepted.get("version").asLong();

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
                .andExpect(jsonPath("$.workOrderId").value(workOrderId))
                .andExpect(jsonPath("$.version").value(recommendationVersion));

        mockMvc.perform(post("/api/v1/operator/maintenance/recommendations/" + recommendationId + "/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "commandId": "%s",
                                  "type": "ACCEPT",
                                  "expectedVersion": 1
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));

        JsonNode createdIncident = json.readTree(mockMvc.perform(post("/api/v1/operator/incidents")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Cypress Coil vibration investigation",
                                  "type": "TECHNICAL",
                                  "severity": "MODERATE",
                                  "internalDescription": "INTERNAL wheel vibration diagnosis — never guest facing",
                                  "attractionIds": ["cypress-coil"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsByteArray());
        String incidentId = createdIncident.get("id").asString();

        long version = json.readTree(mockMvc.perform(get("/api/v1/operator/maintenance/work-orders/" + workOrderId)
                        .with(TestAuth.operator()))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.asset.assetCode").value("CC-TRAIN-01-WHEEL-A"))
                .andReturn()
                .getResponse()
                .getContentAsByteArray()).get("version").asLong();

        mockMvc.perform(post("/api/v1/operator/maintenance/work-orders/" + workOrderId + "/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "commandId": "%s",
                                  "type": "START_WORK",
                                  "expectedVersion": %s
                                }
                                """.formatted(UUID.randomUUID(), version)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"))
                .andExpect(jsonPath("$.type").value("https://lumen-marsh.dev/problems/invalid-maintenance-transition"));

        version = command(workOrderId, "OPEN", version, null, null).get("workOrder").get("version").asLong();

        mockMvc.perform(post("/api/v1/operator/maintenance/work-orders/" + workOrderId + "/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "commandId": "%s",
                                  "type": "OPEN",
                                  "expectedVersion": 0
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STALE_VERSION"))
                .andExpect(jsonPath("$.type").value("https://lumen-marsh.dev/problems/stale-version"));

        version = command(workOrderId, "LINK_INCIDENT", version, "Linked to the operational incident.", """
                "incidentId": "%s"
                """.formatted(incidentId)).get("workOrder").get("version").asLong();
        version = command(workOrderId, "ASSIGN", version, null, """
                "teamId": "ride-maintenance-alpha"
                """).get("workOrder").get("version").asLong();
        version = command(workOrderId, "START_WORK", version, null, null).get("workOrder").get("version").asLong();
        version = command(workOrderId, "SET_ESTIMATED_RESTORE", version, "Replacement and inspection work are progressing normally.", """
                "estimatedRestoreAt": "2099-01-01T00:00:00Z"
                """).get("workOrder").get("version").asLong();

        JsonNode detail = json.readTree(mockMvc.perform(get("/api/v1/operator/maintenance/work-orders/" + workOrderId)
                        .with(TestAuth.operator()))
                .andReturn()
                .getResponse()
                .getContentAsByteArray());
        for (JsonNode item : detail.get("checklist")) {
            version = command(workOrderId, "RECORD_CHECKLIST_RESULT", version, "Physical inspection completed.", """
                    "checklistItemId": "%s",
                    "result": "PASSED",
                    "notes": "No visible damage; bearing replaced as a preventive measure."
                    """.formatted(item.get("id").asString()))
                    .get("workOrder").get("version").asLong();
        }

        mockMvc.perform(post("/api/v1/operator/maintenance/work-orders/" + workOrderId + "/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "commandId": "%s",
                                  "type": "APPROVE_INSPECTION",
                                  "expectedVersion": %s,
                                  "reason": "Operators cannot approve inspection."
                                }
                                """.formatted(UUID.randomUUID(), version)))
                .andExpect(status().isForbidden());

        version = command(workOrderId, "REQUEST_INSPECTION", version, null, null)
                .get("workOrder").get("version").asLong();
        JsonNode approved = command(
                workOrderId,
                "APPROVE_INSPECTION",
                version,
                "Required inspection steps passed. Attraction is ready for operational testing.",
                null,
                TestAuth.supervisor()
        );
        assertThat(approved.get("workOrder").get("status").asString()).isEqualTo("READY_FOR_TESTING");
        long readyVersion = approved.get("workOrder").get("version").asLong();

        mockMvc.perform(get("/api/v1/operator/incidents/" + incidentId).with(TestAuth.operator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relatedWorkOrders[0].workOrderNumber").exists());

        mockMvc.perform(get("/api/v1/attractions/cypress-coil"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("LM-"))))
                .andExpect(content().string(not(containsString("CC-TRAIN-01-WHEEL-A"))))
                .andExpect(content().string(not(containsString("wheel vibration"))))
                .andExpect(content().string(not(containsString("Lumen Marsh Fabrication"))));

        mockMvc.perform(get("/api/v1/operator/maintenance/work-orders/" + workOrderId)
                        .with(TestAuth.operator()))
                .andExpect(jsonPath("$.recommendedAttractionAction.command").value("START_TESTING"));

        mockMvc.perform(post("/api/v1/operator/attractions/cypress-coil/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CommandJson.envelope("""
                                {"type":"START_TESTING","expectedVersion":0}
                                """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TESTING"));

        mockMvc.perform(post("/api/v1/operator/maintenance/work-orders/" + workOrderId + "/commands")
                        .with(TestAuth.supervisor())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "commandId": "%s",
                                  "type": "COMPLETE",
                                  "expectedVersion": %s,
                                  "reason": "Attraction testing and supervisor approval are complete."
                                }
                                """.formatted(UUID.randomUUID(), readyVersion)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("MAINTENANCE_PREREQUISITE"));

        mockMvc.perform(post("/api/v1/operator/attractions/cypress-coil/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CommandJson.envelope("""
                                {"type":"COMPLETE_TESTING","expectedVersion":1}
                                """)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/operator/attractions/cypress-coil/commands")
                        .with(TestAuth.supervisor())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CommandJson.envelope("""
                                {"type":"APPROVE_RETURN_TO_SERVICE","expectedVersion":2}
                                """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPERATING"));

        command(
                workOrderId,
                "COMPLETE",
                readyVersion,
                "Attraction testing and supervisor approval are complete.",
                null,
                TestAuth.supervisor()
        );

        mockMvc.perform(get("/api/v1/operator/maintenance/work-orders/" + workOrderId)
                        .with(TestAuth.operator()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.recommendedAttractionAction").value(nullValue()));

        mockMvc.perform(get("/api/v1/operator/maintenance/work-orders/" + workOrderId + "/activity")
                        .with(TestAuth.operator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("WORK_ORDER_CREATED"))
                .andExpect(jsonPath("$[0].actorSubject").value("operator-sub-1"));

        mockMvc.perform(get("/api/v1/advisories"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("INTERNAL wheel vibration"))))
                .andExpect(content().string(not(containsString("CC-TRAIN-01-WHEEL-A"))));

        assertThat(MaintenanceAssetSeedData.wheelAId().toString()).isEqualTo("0fd7c7ce-f7af-4b65-8789-27679ca40303");
    }

    @Test
    void resolvingIncidentWithActiveP2WorkRequiresConfirmation() throws Exception {
        JsonNode createdIncident = json.readTree(mockMvc.perform(post("/api/v1/operator/incidents")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Cypress Coil technical pause",
                                  "type": "TECHNICAL",
                                  "severity": "MODERATE",
                                  "internalDescription": "Keep this internal."
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsByteArray());
        String incidentId = createdIncident.get("id").asString();

        mockMvc.perform(post("/api/v1/operator/maintenance/work-orders")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "commandId": "%s",
                                  "assetId": "%s",
                                  "incidentId": "%s",
                                  "sourceType": "INCIDENT",
                                  "classification": "CORRECTIVE",
                                  "priority": "P2",
                                  "summary": "Confirm-gated work order"
                                }
                                """.formatted(UUID.randomUUID(), MaintenanceAssetSeedData.wheelAId(), incidentId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/operator/incidents/" + incidentId + "/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CommandJson.envelope("""
                                {"type":"ACKNOWLEDGE","expectedVersion":2}
                                """)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/operator/incidents/" + incidentId + "/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CommandJson.envelope("""
                                {"type":"START_MITIGATION","expectedVersion":3}
                                """)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/operator/incidents/" + incidentId + "/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CommandJson.envelope("""
                                {
                                  "type": "RESOLVE",
                                  "reason": "Operational response complete",
                                  "expectedVersion": 4
                                }
                                """)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ACTIVE_WORK_ORDERS"));
        mockMvc.perform(post("/api/v1/operator/incidents/" + incidentId + "/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CommandJson.envelope("""
                                {
                                  "type": "RESOLVE",
                                  "reason": "Operational response complete",
                                  "expectedVersion": 4,
                                  "confirmActiveWorkOrders": true
                                }
                                """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    void machineTokenCannotCallOperatorMaintenanceCommands() throws Exception {
        mockMvc.perform(get("/api/v1/operator/maintenance/assets").with(TestAuth.reliabilityService()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/integrations/reliability/recommendations")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vibrationBody("operator-cannot-ingest")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/operator/maintenance/assets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clientSuppliedActorIsIgnored() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/operator/maintenance/work-orders")
                        .with(TestAuth.operator())
                        .header("X-Actor", "spoofed-attacker")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "commandId": "%s",
                                  "assetId": "%s",
                                  "sourceType": "MANUAL",
                                  "classification": "CORRECTIVE",
                                  "priority": "P3",
                                  "summary": "Preventive inspection"
                                }
                                """.formatted(UUID.randomUUID(), MaintenanceAssetSeedData.wheelAId())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/operator/maintenance/work-orders/")))
                .andReturn();
        String id = json.readTree(created.getResponse().getContentAsByteArray()).get("id").asString();
        mockMvc.perform(get("/api/v1/operator/maintenance/work-orders/" + id + "/activity")
                        .with(TestAuth.operator()))
                .andExpect(jsonPath("$[0].actorDisplayName").value("Operator One"))
                .andExpect(jsonPath("$[0].actorSubject").value("operator-sub-1"));
    }

    @Test
    void duplicateCommandIdIsIdempotent() throws Exception {
        UUID commandId = UUID.randomUUID();
        String body = """
                {
                  "commandId": "%s",
                  "assetId": "%s",
                  "classification": "PREVENTIVE",
                  "priority": "P4",
                  "summary": "Scheduled lubrication"
                }
                """.formatted(commandId, MaintenanceAssetSeedData.wheelAId());
        MvcResult first = mockMvc.perform(post("/api/v1/operator/maintenance/work-orders")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        MvcResult second = mockMvc.perform(post("/api/v1/operator/maintenance/work-orders")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(json.readTree(first.getResponse().getContentAsByteArray()).get("id").asString())
                .isEqualTo(json.readTree(second.getResponse().getContentAsByteArray()).get("id").asString());

        String workOrderId = json.readTree(first.getResponse().getContentAsByteArray()).get("id").asString();
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
                .andExpect(jsonPath("$.workOrder.status").value("OPEN"));

        mockMvc.perform(post("/api/v1/operator/maintenance/work-orders")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workOrderId))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void reusedCommandIdOnAnotherWorkOrderIsRejected() throws Exception {
        UUID openCommandId = UUID.randomUUID();
        String firstId = json.readTree(mockMvc.perform(post("/api/v1/operator/maintenance/work-orders")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "commandId": "%s",
                                  "assetId": "%s",
                                  "classification": "PREVENTIVE",
                                  "priority": "P4",
                                  "summary": "First work order"
                                }
                                """.formatted(UUID.randomUUID(), MaintenanceAssetSeedData.wheelAId())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsByteArray()).get("id").asString();
        String secondId = json.readTree(mockMvc.perform(post("/api/v1/operator/maintenance/work-orders")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "commandId": "%s",
                                  "assetId": "%s",
                                  "classification": "PREVENTIVE",
                                  "priority": "P4",
                                  "summary": "Second work order"
                                }
                                """.formatted(UUID.randomUUID(), MaintenanceAssetSeedData.wheelAId())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsByteArray()).get("id").asString();

        mockMvc.perform(post("/api/v1/operator/maintenance/work-orders/" + firstId + "/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "commandId": "%s",
                                  "type": "OPEN",
                                  "expectedVersion": 1
                                }
                                """.formatted(openCommandId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workOrder.status").value("OPEN"))
                .andExpect(jsonPath("$.workOrder.version").value(2));

        mockMvc.perform(post("/api/v1/operator/maintenance/work-orders/" + firstId + "/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "commandId": "%s",
                                  "type": "ADD_NOTE",
                                  "expectedVersion": 2,
                                  "data": { "note": "Later note after open" }
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workOrder.status").value("OPEN"))
                .andExpect(jsonPath("$.workOrder.version").value(3));

        mockMvc.perform(post("/api/v1/operator/maintenance/work-orders/" + firstId + "/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "commandId": "%s",
                                  "type": "OPEN",
                                  "expectedVersion": 99
                                }
                                """.formatted(openCommandId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replay").value(true))
                .andExpect(jsonPath("$.workOrder.status").value("OPEN"))
                .andExpect(jsonPath("$.workOrder.version").value(2));

        mockMvc.perform(post("/api/v1/operator/maintenance/work-orders/" + secondId + "/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "commandId": "%s",
                                  "type": "OPEN",
                                  "expectedVersion": 1
                                }
                                """.formatted(openCommandId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_COMMAND"));
    }

    private JsonNode command(String workOrderId, String type, long version, String reason, String data) throws Exception {
        return command(workOrderId, type, version, reason, data, TestAuth.operator());
    }

    private JsonNode command(
            String workOrderId,
            String type,
            long version,
            String reason,
            String data,
            RequestPostProcessor auth
    ) throws Exception {
        String reasonJson = reason == null ? "" : ", \"reason\": \"%s\"".formatted(reason);
        String dataJson = data == null ? "" : ", \"data\": { %s }".formatted(data);
        MvcResult result = mockMvc.perform(post("/api/v1/operator/maintenance/work-orders/" + workOrderId + "/commands")
                        .with(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "commandId": "%s",
                                  "type": "%s",
                                  "expectedVersion": %s
                                  %s
                                  %s
                                }
                                """.formatted(UUID.randomUUID(), type, version, reasonJson, dataJson)))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsByteArray());
    }

    private static String vibrationBody(String observationId) {
        return """
                {
                  "observationId": "%s",
                  "observedAt": "2026-09-14T18:25:00Z",
                  "assetCode": "CC-TRAIN-01-WHEEL-A",
                  "signalType": "VIBRATION",
                  "severity": "WARNING",
                  "value": 8.4,
                  "unit": "mm/s",
                  "evidence": "Fictional simulated vibration exceeded the demonstration threshold for three consecutive samples.",
                  "recommendedAction": "Inspect the wheel assembly and consider reduced-capacity operation."
                }
                """.formatted(observationId);
    }
}
