package com.deanwagman.lumenmarsh.venueops.flow;

import com.deanwagman.lumenmarsh.venueops.security.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "venueops.attractions.seed=true")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FlowParkAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void observationProducesProjectionAndDuplicateReplays() throws Exception {
        UUID observationId = UUID.fromString("183cc7a4-69fd-43e9-94b3-acde2c196481");
        String observedAt = Instant.now().minusSeconds(30).toString();
        String body = observation(observationId, observedAt, 225, 18, 12);

        mockMvc.perform(post("/api/v1/integrations/flow/observations")
                        .with(TestAuth.flowService())
                        .header("X-Correlation-Id", "flow-obs-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accepted").value(true))
                .andExpect(jsonPath("$.replay").value(false))
                .andExpect(jsonPath("$.projectionVersion").value(1));

        mockMvc.perform(post("/api/v1/integrations/flow/observations")
                        .with(TestAuth.flowService())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replay").value(true))
                .andExpect(jsonPath("$.projectionVersion").value(1));

        mockMvc.perform(get("/api/v1/operator/flow/attractions/mangrove-run").with(TestAuth.operator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projection.queueLength").value(225))
                .andExpect(jsonPath("$.projection.calculatedWaitMinutes").isNumber())
                .andExpect(jsonPath("$.projection.simulated").value(true))
                .andExpect(jsonPath("$.projection.freshness").value("FRESH"));
    }

    @Test
    void outOfOrderObservationDoesNotReplaceNewerProjection() throws Exception {
        UUID newest = UUID.fromString("aaaaaaaa-1111-4111-8111-aaaaaaaaaaaa");
        UUID older = UUID.fromString("bbbbbbbb-2222-4222-8222-bbbbbbbbbbbb");
        Instant now = Instant.now();
        mockMvc.perform(post("/api/v1/integrations/flow/observations")
                        .with(TestAuth.flowService())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(observation(newest, "cypress-coil", now.minusSeconds(20).toString(), 200, 16, 12)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/integrations/flow/observations")
                        .with(TestAuth.flowService())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(observation(older, "cypress-coil", now.minusSeconds(90).toString(), 40, 4, 10)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.replay").value(false));

        mockMvc.perform(get("/api/v1/operator/flow/attractions/cypress-coil").with(TestAuth.operator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projection.observationId").value(newest.toString()))
                .andExpect(jsonPath("$.projection.queueLength").value(200));
    }

    @Test
    void forecastRecommendationRequiresHumanPublishBeforeGuestSeesGuidance() throws Exception {
        UUID observationId = UUID.fromString("cccccccc-3333-4333-8333-cccccccccccc");
        UUID recommendationId = UUID.fromString("dddddddd-4444-4444-8444-dddddddddddd");
        Instant observedAt = Instant.now().minusSeconds(15);
        mockMvc.perform(post("/api/v1/integrations/flow/observations")
                        .with(TestAuth.flowService())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(observation(observationId, "stormglass-station", observedAt.toString(), 80, 10, 8)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/integrations/flow/forecasts")
                        .with(TestAuth.flowService())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(forecasts(observationId, recommendationId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recommendationAccepted").value(true));

        mockMvc.perform(get("/api/v1/flow/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(post("/api/v1/operator/flow/recommendations/" + recommendationId + "/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "commandId": "fe33d625-213a-4b0c-a769-f00ddeef764c",
                                  "type": "APPROVE",
                                  "expectedVersion": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendation.status").value("APPROVED"));

        mockMvc.perform(post("/api/v1/operator/flow/recommendations/" + recommendationId + "/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "commandId": "aaaaaaaa-213a-4b0c-a769-f00ddeef764c",
                                  "type": "PUBLISH",
                                  "expectedVersion": 2,
                                  "data": { "guestMessage": "Try Cypress Coil next." }
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/operator/flow/recommendations/" + recommendationId + "/commands")
                        .with(TestAuth.supervisor())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "commandId": "bbbbbbbb-213a-4b0c-a769-f00ddeef764c",
                                  "type": "PUBLISH",
                                  "expectedVersion": 2,
                                  "data": { "guestMessage": "Stormglass Station is a good next choice." }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendation.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/v1/flow/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishedGuidance[0].guestMessage").value("Stormglass Station is a good next choice."))
                .andExpect(jsonPath("$.publishedGuidance[0].simulated").value(true))
                .andExpect(jsonPath("$[*].explanation").doesNotExist())
                .andExpect(jsonPath("$[*].confidence").doesNotExist())
                .andExpect(jsonPath("$[*].actor").doesNotExist())
                .andExpect(jsonPath("$[*].relatedIncidentId").doesNotExist())
                .andExpect(contentSafe());

        mockMvc.perform(get("/api/v1/attractions/stormglass-station/wait-forecast"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attractionId").value("stormglass-station"))
                .andExpect(jsonPath("$.forecast30Minutes").isString())
                .andExpect(jsonPath("$.confidence").doesNotExist())
                .andExpect(jsonPath("$.queueLength").doesNotExist());
    }

    @Test
    void machineTokenCannotCallOperatorFlowAndOperatorCannotIngest() throws Exception {
        mockMvc.perform(get("/api/v1/operator/flow/overview").with(TestAuth.flowService()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/integrations/flow/observations")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(observation(UUID.randomUUID(), Instant.now().toString(), 10, 1, 1)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/operator/flow/overview").with(TestAuth.operatorWithoutFlow()))
                .andExpect(status().isForbidden());
    }

    @Test
    void staleCommandIsConflictAndDuplicateCommandReplays() throws Exception {
        UUID observationId = UUID.fromString("eeeeeeee-5555-4555-8555-eeeeeeeeeeee");
        UUID recommendationId = UUID.fromString("ffffffff-6666-4666-8666-ffffffffffff");
        mockMvc.perform(post("/api/v1/integrations/flow/observations")
                        .with(TestAuth.flowService())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(observation(observationId, "cypress-coil", Instant.now().minusSeconds(10).toString(), 90, 12, 9)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/integrations/flow/forecasts")
                        .with(TestAuth.flowService())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(forecasts(observationId, recommendationId, "cypress-coil")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/operator/flow/recommendations/" + recommendationId + "/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "commandId": "12121212-213a-4b0c-a769-f00ddeef764c",
                                  "type": "DISMISS",
                                  "expectedVersion": 99,
                                  "reason": "stale"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STALE_VERSION"));

        String dismiss = """
                {
                  "commandId": "13131313-213a-4b0c-a769-f00ddeef764c",
                  "type": "DISMISS",
                  "expectedVersion": 1,
                  "reason": "Noise in the telemetry window."
                }
                """;
        mockMvc.perform(post("/api/v1/operator/flow/recommendations/" + recommendationId + "/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dismiss))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendation.status").value("DISMISSED"))
                .andExpect(jsonPath("$.recommendation.version").value(2));

        mockMvc.perform(post("/api/v1/operator/flow/recommendations/" + recommendationId + "/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dismiss))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendation.status").value("DISMISSED"))
                .andExpect(jsonPath("$.recommendation.version").value(2));
    }

    private static org.springframework.test.web.servlet.ResultMatcher contentSafe() {
        return result -> {
            String body = result.getResponse().getContentAsString();
            org.hamcrest.MatcherAssert.assertThat(body, not(containsString("confidence")));
            org.hamcrest.MatcherAssert.assertThat(body, not(containsString("Operator One")));
            org.hamcrest.MatcherAssert.assertThat(body, not(containsString("queueLength")));
        };
    }

    private static String observation(UUID observationId, String observedAt, int queue, int arrivals, int boarded) {
        return observation(observationId, "mangrove-run", observedAt, queue, arrivals, boarded);
    }

    private static String observation(
            UUID observationId,
            String attractionId,
            String observedAt,
            int queue,
            int arrivals,
            int boarded
    ) {
        return """
                {
                  "observationId": "%s",
                  "attractionId": "%s",
                  "observedAt": "%s",
                  "windowSeconds": 60,
                  "queueLength": %d,
                  "arrivals": %d,
                  "boarded": %d,
                  "operatingUnits": 6,
                  "configuredUnits": 8,
                  "sourceType": "SIMULATOR",
                  "simulated": true
                }
                """.formatted(observationId, attractionId, observedAt, queue, arrivals, boarded);
    }

    private static String forecasts(UUID observationId, UUID recommendationId) {
        return forecasts(observationId, recommendationId, "stormglass-station");
    }

    private static String forecasts(UUID observationId, UUID recommendationId, String attractionId) {
        return """
                {
                  "attractionId": "%s",
                  "generatedAt": "%s",
                  "basedOnObservationId": "%s",
                  "simulated": true,
                  "forecasts": [
                    {
                      "forecastId": "%s",
                      "horizonMinutes": 15,
                      "predictedQueueLength": 90,
                      "predictedWaitMinutes": 20,
                      "confidence": "MEDIUM",
                      "assumptions": ["Smoothed arrival rate from the last three windows."],
                      "explanation": "Arrivals slightly exceed throughput."
                    },
                    {
                      "forecastId": "%s",
                      "horizonMinutes": 30,
                      "predictedQueueLength": 110,
                      "predictedWaitMinutes": 25,
                      "confidence": "MEDIUM",
                      "assumptions": ["No further capacity loss."],
                      "explanation": "Wait is likely to rise over the next half hour."
                    },
                    {
                      "forecastId": "%s",
                      "horizonMinutes": 60,
                      "predictedQueueLength": 140,
                      "predictedWaitMinutes": 35,
                      "confidence": "LOW",
                      "assumptions": ["Demand redistribution continues."],
                      "explanation": "Longer horizon with delayed data remains conservative."
                    }
                  ],
                  "recommendation": {
                    "recommendationId": "%s",
                    "type": "CONGESTION_EXPECTED",
                    "severity": "WARNING",
                    "sourceAttractionId": "%s",
                    "affectedAttractionIds": ["%s"],
                    "recommendedDestinationIds": ["cypress-coil"],
                    "summary": "Congestion expected",
                    "explanation": "Predicted wait is rising before posted wait changes.",
                    "guestMessage": "Consider Cypress Coil while this queue builds."
                  }
                }
                """.formatted(
                attractionId,
                Instant.now().toString(),
                observationId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                recommendationId,
                attractionId,
                attractionId
        );
    }
}
