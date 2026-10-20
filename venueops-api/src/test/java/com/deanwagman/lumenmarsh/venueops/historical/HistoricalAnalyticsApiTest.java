package com.deanwagman.lumenmarsh.venueops.historical;

import com.deanwagman.lumenmarsh.venueops.flow.application.FlowRecommendationRepository;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentRepository;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceRecommendationRepository;
import com.deanwagman.lumenmarsh.venueops.security.TestAuth;
import com.deanwagman.lumenmarsh.venueops.weather.application.WeatherRecommendationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.FIXTURE_DATASET_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HistoricalAnalyticsApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WeatherRecommendationRepository weather;

    @Autowired
    private MaintenanceRecommendationRepository maintenance;

    @Autowired
    private IncidentRepository incidents;

    @Autowired
    private FlowRecommendationRepository flow;

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
                        org.hamcrest.Matchers.startsWith("segment-")))
                .andExpect(jsonPath("$.answers.length()").value(3))
                .andExpect(jsonPath("$.answers[0].question").value("Which attraction had the worst uptime?"))
                .andExpect(jsonPath("$.answers[1].question").value("What caused it?"))
                .andExpect(jsonPath("$.answers[1].answer").value(
                        org.hamcrest.Matchers.containsString("maintenance episode")))
                .andExpect(jsonPath("$.answers[1].claims[0].citations[0].id").value(
                        org.hamcrest.Matchers.startsWith("segment-")))
                .andExpect(jsonPath("$.answers[2].question").value("Which day?"))
                .andExpect(jsonPath("$.answers[2].answer").value(
                        org.hamcrest.Matchers.containsString("2026-10-15")));

        mockMvc.perform(get("/api/v1/operator/analytics/availability/attractions/cypress-coil")
                        .with(TestAuth.operator())
                        .param("datasetId", FIXTURE_DATASET_ID)
                        .param("date", "2026-10-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uptimePercentage").value(60.42))
                .andExpect(jsonPath("$.downtimeMinutes.TECHNICAL_DELAY").value(150))
                .andExpect(jsonPath("$.segments[1].controlTowerUrl").value(
                        org.hamcrest.Matchers.containsString("citation=segment-")))
                .andExpect(jsonPath("$.causes[0].episodeId").value("episode-cypress-maintenance-2026-10-15"))
                .andExpect(jsonPath("$.causes[0].segmentActivityIds").isNotEmpty())
                .andExpect(jsonPath("$.causes[0].citations[0].id").value(
                        org.hamcrest.Matchers.startsWith("segment-")));

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
                        org.hamcrest.Matchers.containsString("Cypress Coil had the worst uptime at 86.81%")))
                .andExpect(jsonPath("$.claims[0].citations[0].id").value(
                        org.hamcrest.Matchers.startsWith("segment-")));

        for (String question : List.of("What caused it?", "Which day?")) {
            mockMvc.perform(post("/api/v1/operator/analytics/questions")
                            .with(TestAuth.operator())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "datasetId": "%s",
                                      "from": "2026-10-13",
                                      "to": "2026-10-15",
                                      "question": "%s"
                                    }
                                    """.formatted(FIXTURE_DATASET_ID, question)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.question").value(question))
                    .andExpect(jsonPath("$.claims[0].citations[0].controlTowerUrl").value(
                            org.hamcrest.Matchers.containsString("date=2026-10-15")));
        }
    }

    @Test
    void analyticsRequiresOperatorAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/operator/analytics/availability")
                        .param("datasetId", FIXTURE_DATASET_ID)
                        .param("from", "2026-10-13")
                        .param("to", "2026-10-15"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void replayRequiresSupervisorAndPreservesGuestAttractions() throws Exception {
        int weatherBefore = weather.findAll().size();
        int maintenanceBefore = maintenance.findAll().size();
        int incidentsBefore = incidents.findAll().size();
        int flowBefore = flow.findAll().size();
        MvcResult guestBefore = mockMvc.perform(get("/api/v1/attractions"))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult activityBefore = mockMvc.perform(get("/api/v1/operator/attractions/cypress-coil/activity")
                        .with(TestAuth.operator()))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(get("/api/v1/operator/analytics/datasets").with(TestAuth.operator()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/operator/analytics/datasets/" + FIXTURE_DATASET_ID + "/replay")
                        .with(TestAuth.operator()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/operator/analytics/datasets").with(TestAuth.weatherService()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/operator/analytics/datasets/" + FIXTURE_DATASET_ID + "/replay")
                        .with(TestAuth.jwt("supervisor-no-read", "Supervisor No Read", List.of("supervisors"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/operator/analytics/datasets/" + FIXTURE_DATASET_ID + "/replay")
                        .with(TestAuth.supervisor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changed").value(false));

        MvcResult guestAfter = mockMvc.perform(get("/api/v1/attractions")).andExpect(status().isOk()).andReturn();
        MvcResult activityAfter = mockMvc.perform(get("/api/v1/operator/attractions/cypress-coil/activity")
                        .with(TestAuth.operator()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(guestAfter.getResponse().getContentAsString())
                .isEqualTo(guestBefore.getResponse().getContentAsString());
        assertThat(activityAfter.getResponse().getContentAsString())
                .isEqualTo(activityBefore.getResponse().getContentAsString());
        assertThat(weather.findAll()).hasSize(weatherBefore);
        assertThat(maintenance.findAll()).hasSize(maintenanceBefore);
        assertThat(incidents.findAll()).hasSize(incidentsBefore);
        assertThat(flow.findAll()).hasSize(flowBefore);
    }

    @Test
    void unknownDatasetRangeAndQuestionAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/operator/analytics/availability")
                        .with(TestAuth.operator())
                        .param("datasetId", "missing-dataset")
                        .param("from", "2026-10-13")
                        .param("to", "2026-10-15"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/operator/analytics/availability")
                        .with(TestAuth.operator())
                        .param("datasetId", FIXTURE_DATASET_ID)
                        .param("from", "2026-10-15")
                        .param("to", "2026-10-13"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/operator/analytics/questions")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "datasetId": "%s",
                                  "from": "2026-10-13",
                                  "to": "2026-10-15",
                                  "question": "What was the longest queue?"
                                }
                                """.formatted(FIXTURE_DATASET_ID)))
                .andExpect(status().isBadRequest());
    }
}
