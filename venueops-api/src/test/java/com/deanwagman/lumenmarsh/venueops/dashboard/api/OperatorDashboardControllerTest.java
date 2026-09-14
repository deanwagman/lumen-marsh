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
