package com.deanwagman.lumenmarsh.venueops.dashboard.api;

import com.deanwagman.lumenmarsh.venueops.security.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "venueops.attractions.seed=true")
@AutoConfigureMockMvc
class OperatorDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void anonymousRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/operator/dashboard")).andExpect(status().isUnauthorized());
    }

    @Test
    void weatherServiceCannotReadDashboard() throws Exception {
        mockMvc.perform(get("/api/v1/operator/dashboard").with(TestAuth.weatherService()))
                .andExpect(status().isForbidden());
    }

    @Test
    void operatorDashboardProjectsTypedWeatherActionWithoutRuleIdentity() throws Exception {
        mockMvc.perform(post("/api/v1/integrations/weather/recommendations")
                        .with(TestAuth.weatherService())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recommendationId": "rec-dashboard-clearance",
                                  "ruleId": "simulated-lightning-clearance",
                                  "status": "ACTIVE",
                                  "severity": "INFO",
                                  "summary": "Review held attractions",
                                  "evidence": "Simulated clearance period completed.",
                                  "recommendedAction": "Review held attractions for return to service",
                                  "affectedAttractionIds": ["mangrove-run", "cypress-coil"],
                                  "observedAt": "2026-09-01T16:30:00Z",
                                  "version": 1
                                }
                                """))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/api/v1/operator/dashboard").with(TestAuth.operator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.pendingWeatherRecommendations[?(@.id=='rec-dashboard-clearance')].action"
                ).value(org.hamcrest.Matchers.hasItem("RETURN_TO_SERVICE")))
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("simulated-lightning-clearance");
    }

    @Test
    void operatorCanReadSnapshotWithoutWritingActivity() throws Exception {
        mockMvc.perform(get("/api/v1/operator/dashboard").with(TestAuth.operator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedAt").exists())
                .andExpect(jsonPath("$.summary.operatingAttractions").isNumber())
                .andExpect(jsonPath("$.freshness.venueOps.status").exists())
                .andExpect(jsonPath("$.openIncidents").isArray())
                .andExpect(jsonPath("$.publishedGuestAdvisories").isArray());
    }

    @Test
    void dashboardReadDoesNotCreateIncidentsOrChangeVersions() throws Exception {
        mockMvc.perform(get("/api/v1/operator/dashboard").with(TestAuth.operator()))
                .andExpect(status().isOk());

        MvcResult list = mockMvc.perform(get("/api/v1/operator/incidents").with(TestAuth.operator()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode before = jsonMapper.readTree(list.getResponse().getContentAsByteArray());
        int count = before.size();

        mockMvc.perform(get("/api/v1/operator/dashboard").with(TestAuth.operator()))
                .andExpect(status().isOk());

        MvcResult after = mockMvc.perform(get("/api/v1/operator/incidents").with(TestAuth.operator()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(jsonMapper.readTree(after.getResponse().getContentAsByteArray()).size()).isEqualTo(count);
    }

    @Test
    void seededPendingReliabilityRecommendationIsVisibleAndGuestJsonStaysClean() throws Exception {
        MvcResult dashboard = mockMvc.perform(get("/api/v1/operator/dashboard").with(TestAuth.operator()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = jsonMapper.readTree(dashboard.getResponse().getContentAsByteArray());
        boolean found = false;
        for (JsonNode item : body.get("needsAttention")) {
            if ("PENDING_RELIABILITY_RECOMMENDATION".equals(item.path("kind").asText())
                    && "b7e2c1a0-4c11-4c11-8c11-27679ca40303".equals(item.path("subjectId").asText())) {
                found = true;
                assertThat(item.path("href").asText()).isEqualTo("/docs/shift#pending-reliability");
                assertThat(item.path("subjectLabel").asText()).isEqualTo("Vibration");
                assertThat(item.toString()).doesNotContain("CC-TRAIN-01-WHEEL-A");
                assertThat(item.toString()).doesNotContain("workOrderNumber");
            }
        }
        assertThat(found).isTrue();

        String guest = mockMvc.perform(get("/api/v1/attractions/cypress-coil"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(guest).doesNotContain("CC-TRAIN-01-WHEEL-A");
        assertThat(guest).doesNotContain("workOrderNumber");
        assertThat(guest).doesNotContain("assetCode");
        assertThat(guest).doesNotContain("internalDescription");
        assertThat(guest).doesNotContain("INTERNAL ONLY");
    }

    @Test
    void dashboardReflectsReportedIncidentAndDoesNotLeakInternalDescriptionOnAdvisories() throws Exception {
        mockMvc.perform(post("/api/v1/operator/incidents")
                        .with(TestAuth.operator())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Dashboard weather incident",
                                  "type": "WEATHER",
                                  "severity": "MAJOR",
                                  "internalDescription": "INTERNAL ONLY — dashboard must not copy this to advisories",
                                  "attractionIds": ["mangrove-run"]
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/operator/dashboard").with(TestAuth.operator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.openIncidents").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.openIncidents[?(@.title=='Dashboard weather incident')].severity")
                        .value(org.hamcrest.Matchers.hasItem("MAJOR")))
                .andExpect(jsonPath("$.publishedGuestAdvisories[*].message")
                        .value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(
                                org.hamcrest.Matchers.containsString("INTERNAL ONLY")))));
    }
}
