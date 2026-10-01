package com.deanwagman.lumenmarsh.venueops.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.deanwagman.lumenmarsh.venueops.testsupport.CommandJson;
import tools.jackson.databind.json.JsonMapper;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "venueops.attractions.seed=true")
@AutoConfigureMockMvc
class SecurityAuthorizationMatrixTest {

    @Autowired
    private MockMvc mockMvc;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void guestCanReadAttractionsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/attractions")).andExpect(status().isOk());
    }

    @Test
    void guestCannotCallAttractionCommand() throws Exception {
        mockMvc.perform(post("/api/v1/operator/attractions/mangrove-run/commands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CommandJson.envelope("""
                                {"type":"PLACE_WEATHER_HOLD","reason":"x","expectedVersion":0}
                                """)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void forgedXActorDoesNotOverrideVerifiedIdentity() throws Exception {
        mockMvc.perform(post("/api/v1/operator/attractions/mangrove-run/commands")
                        .with(TestAuth.operator())
                        .header("X-Actor", "spoofed-attacker")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CommandJson.envelope("""
                                {"type":"UPDATE_WAIT_TIME","waitMinutes":12,"expectedVersion":0}
                                """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waitMinutes").value(12));

        mockMvc.perform(get("/api/v1/operator/attractions/mangrove-run/activity").with(TestAuth.operator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actor").value("Operator One"));
    }

    @Test
    void weatherServiceCannotCallOperatorEndpoints() throws Exception {
        mockMvc.perform(post("/api/v1/operator/attractions/mangrove-run/commands")
                        .with(TestAuth.weatherService())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CommandJson.envelope("""
                                {"type":"PLACE_WEATHER_HOLD","reason":"x","expectedVersion":0}
                                """)))
                .andExpect(status().isForbidden());
    }

    @Test
    void operatorCannotCallWeatherIngest() throws Exception {
        mockMvc.perform(post("/api/v1/integrations/weather/recommendations")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recommendationId":"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
                                  "ruleId":"r",
                                  "status":"ACTIVE",
                                  "severity":"INFO",
                                  "summary":"s",
                                  "evidence":"e",
                                  "recommendedAction":"a",
                                  "affectedAttractionIds":[],
                                  "observedAt":"2026-09-01T16:00:00Z",
                                  "version":1
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void reliabilityServiceCannotCallOperatorEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/operator/maintenance/work-orders").with(TestAuth.reliabilityService()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/operator/maintenance/work-orders")
                        .with(TestAuth.reliabilityService())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "commandId": "ce8d795d-cc8c-4f10-a1ac-4eb331e727ab",
                                  "assetId": "0fd7c7ce-f7af-4b65-8789-27679ca40303",
                                  "classification": "CORRECTIVE",
                                  "priority": "P2",
                                  "summary": "Machine tokens cannot create work"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void operatorCannotCallReliabilityIngest() throws Exception {
        mockMvc.perform(post("/api/v1/integrations/reliability/recommendations")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "observationId": "operator-denied",
                                  "observedAt": "2026-09-14T18:25:00Z",
                                  "assetCode": "CC-TRAIN-01-WHEEL-A",
                                  "signalType": "VIBRATION",
                                  "severity": "INFO",
                                  "evidence": "e",
                                  "recommendedAction": "a"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void flowServiceCannotCallOperatorEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/operator/flow/overview").with(TestAuth.flowService()))
                .andExpect(status().isForbidden());
    }

    @Test
    void operatorCannotCallFlowIngest() throws Exception {
        mockMvc.perform(post("/api/v1/integrations/flow/observations")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "observationId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
                                  "attractionId": "mangrove-run",
                                  "observedAt": "2026-09-15T18:00:00Z",
                                  "windowSeconds": 60,
                                  "queueLength": 10,
                                  "arrivals": 1,
                                  "boarded": 1,
                                  "operatingUnits": 1,
                                  "configuredUnits": 8,
                                  "sourceType": "SIMULATOR",
                                  "simulated": true
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void guestCanReadFlowWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/flow/overview")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/attractions/mangrove-run/wait-forecast")).andExpect(status().isOk());
    }

    @Test
    void helloIsDenied() throws Exception {
        mockMvc.perform(get("/api/hello")).andExpect(status().isUnauthorized());
    }

    @Test
    void operatorSseRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/operator/events")).andExpect(status().isUnauthorized());
    }

    @Test
    void operatorWithoutMaintenanceReadCanOpenOperatorStream() throws Exception {
        mockMvc.perform(get("/api/v1/operator/events").with(TestAuth.operatorWithoutMaintenance()))
                .andExpect(status().isOk());
    }

    @Test
    void operatorCannotPublishGuestAdvisory() throws Exception {
        String incidentId = reportWeatherIncident(TestAuth.operator(), "Operator publish deny");

        mockMvc.perform(post("/api/v1/operator/incidents/" + incidentId + "/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CommandJson.envelope("""
                                {
                                  "type":"PUBLISH_GUEST_ADVISORY",
                                  "guestTitle":"Should not publish",
                                  "guestMessage":"Operators cannot publish guest advisories.",
                                  "expectedVersion":1
                                }
                                """)))
                .andExpect(status().isForbidden());
    }

    @Test
    void supervisorCanPublishGuestAdvisoryAndGuestPayloadStaysAllowlisted() throws Exception {
        String incidentId = reportWeatherIncident(
                TestAuth.supervisor(),
                "Supervisor publish allowlist"
        );

        mockMvc.perform(post("/api/v1/operator/incidents/" + incidentId + "/commands")
                        .with(TestAuth.supervisor())
                        .header("X-Actor", "spoofed-supervisor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CommandJson.envelope("""
                                {
                                  "type":"PUBLISH_GUEST_ADVISORY",
                                  "guestTitle":"Outdoor weather pause",
                                  "guestMessage":"Some outdoor attractions are temporarily paused.",
                                  "expectedVersion":1
                                }
                                """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guestAdvisoryPublished").value(true));

        mockMvc.perform(get("/api/v1/advisories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + incidentId + "')].title", hasItem("Outdoor weather pause")))
                .andExpect(jsonPath("$[*].internalDescription").doesNotExist())
                .andExpect(jsonPath("$[*].assignedTo").doesNotExist())
                .andExpect(jsonPath("$[*].actor").doesNotExist())
                .andExpect(jsonPath("$[*].activity").doesNotExist())
                .andExpect(content().string(not(containsString("INTERNAL ONLY — must never reach guests"))))
                .andExpect(content().string(not(containsString("\"internalDescription\""))));

        mockMvc.perform(get("/api/v1/operator/incidents/" + incidentId + "/activity")
                        .with(TestAuth.supervisor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type=='GUEST_ADVISORY_PUBLISHED')].actor", hasItem("Supervisor One")));

        mockMvc.perform(post("/api/v1/operator/incidents/" + incidentId + "/commands")
                        .with(TestAuth.supervisor())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CommandJson.envelope("""
                                {
                                  "type":"WITHDRAW_GUEST_ADVISORY",
                                  "reason":"Clear published advisory after leak check",
                                  "expectedVersion":2
                                }
                                """)))
                .andExpect(status().isOk());
    }

    private String reportWeatherIncident(
            org.springframework.test.web.servlet.request.RequestPostProcessor auth,
            String title
    ) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/operator/incidents")
                        .with(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "type": "WEATHER",
                                  "severity": "MAJOR",
                                  "internalDescription": "INTERNAL ONLY — must never reach guests",
                                  "attractionIds": ["mangrove-run"]
                                }
                                """.formatted(title)))
                .andExpect(status().isCreated())
                .andReturn();
        return jsonMapper.readTree(created.getResponse().getContentAsByteArray()).get("id").asString();
    }
}
