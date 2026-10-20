package com.deanwagman.lumenmarsh.venueops.historical;

import com.deanwagman.lumenmarsh.venueops.security.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.FIXTURE_DATASET_ID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HistoricalAnalyticsApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void summaryDetailAndQuestionUseStableCuratedProjection() throws Exception {
        mockMvc.perform(get("/api/v1/operator/analytics/availability")
                        .with(TestAuth.operator())
                        .param("datasetId", FIXTURE_DATASET_ID)
                        .param("from", "2026-10-13")
                        .param("to", "2026-10-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zoneId").value("America/New_York"))
                .andExpect(jsonPath("$.attractions[0].attractionId").value("cypress-coil"))
                .andExpect(jsonPath("$.attractions[0].longestOutageMinutes").value(190))
                .andExpect(jsonPath("$.attractions[0].citations[0].id").value(
                        org.hamcrest.Matchers.startsWith("segment-")));

        mockMvc.perform(get("/api/v1/operator/analytics/availability/attractions/cypress-coil")
                        .with(TestAuth.operator())
                        .param("datasetId", FIXTURE_DATASET_ID)
                        .param("date", "2026-10-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uptimePercentage").value(60.42))
                .andExpect(jsonPath("$.downtimeMinutes.TECHNICAL_DELAY").value(150))
                .andExpect(jsonPath("$.segments[1].controlTowerUrl").value(
                        org.hamcrest.Matchers.containsString("citation=segment-")));

        mockMvc.perform(post("/api/v1/operator/analytics/questions")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "datasetId": "%s",
                                  "from": "2026-10-13",
                                  "to": "2026-10-15",
                                  "question": "Which attraction had the worst uptime?"
                                }
                                """.formatted(FIXTURE_DATASET_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(
                        org.hamcrest.Matchers.containsString("Cypress Coil had the worst uptime")))
                .andExpect(jsonPath("$.claims[0].citations[0].id").value(
                        org.hamcrest.Matchers.startsWith("segment-")));
    }

    @Test
    void analyticsRequiresOperatorAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/operator/analytics/availability")
                        .param("datasetId", FIXTURE_DATASET_ID)
                        .param("from", "2026-10-13")
                        .param("to", "2026-10-15"))
                .andExpect(status().isUnauthorized());
    }
}
