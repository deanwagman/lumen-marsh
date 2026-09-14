package com.deanwagman.lumenmarsh.venueops.weather;

import com.deanwagman.lumenmarsh.venueops.security.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
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
@DirtiesContext
class WeatherRecommendationHandoffIntegrationTest {

    private static final String RECOMMENDATION_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

    @Autowired
    private MockMvc mockMvc;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void ingestRetryIncidentLinkHoldAndClearanceStayIndependent() throws Exception {
        mockMvc.perform(post("/api/v1/integrations/weather/recommendations")
                        .with(TestAuth.weatherService())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(activeBody(1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(RECOMMENDATION_ID))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.operatorStatus").value("PENDING"))
                .andExpect(jsonPath("$.sourceVersion").value(1))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.simulated").value(true));

        mockMvc.perform(post("/api/v1/integrations/weather/recommendations")
                        .with(TestAuth.weatherService())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(activeBody(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.sourceVersion").value(1));

        mockMvc.perform(get("/api/v1/operator/weather/recommendations").with(TestAuth.operator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(RECOMMENDATION_ID));

        MvcResult createdIncident = mockMvc.perform(post("/api/v1/operator/incidents")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Place Mangrove Run and Cypress Coil on weather hold",
                                  "type": "WEATHER",
                                  "severity": "MAJOR",
                                  "internalDescription": "Simulated lightning strike 1.2 miles from the western basin.\\n\\nRecommended action: Place Mangrove Run and Cypress Coil on weather hold",
                                  "attractionIds": ["mangrove-run", "cypress-coil"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String incidentId = jsonMapper.readTree(createdIncident.getResponse().getContentAsByteArray())
                .get("id")
                .asString();

        mockMvc.perform(post("/api/v1/operator/weather/recommendations/" + RECOMMENDATION_ID + "/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "LINK_INCIDENT",
                                  "incidentId": "%s",
                                  "expectedVersion": 1
                                }
                                """.formatted(incidentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operatorStatus").value("LINKED"))
                .andExpect(jsonPath("$.linkedIncidentId").value(incidentId))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.version").value(2));

        mockMvc.perform(get("/api/v1/attractions/mangrove-run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPERATING"));

        MvcResult mangrove = mockMvc.perform(get("/api/v1/operator/attractions/mangrove-run").with(TestAuth.operator()))
                .andExpect(status().isOk())
                .andReturn();
        long mangroveVersion = jsonMapper.readTree(mangrove.getResponse().getContentAsByteArray())
                .get("version")
                .asLong();

        mockMvc.perform(post("/api/v1/operator/attractions/mangrove-run/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "PLACE_WEATHER_HOLD",
                                  "reason": "Lightning detected within operating radius",
                                  "expectedVersion": %d
                                }
                                """.formatted(mangroveVersion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WEATHER_HOLD"));

        mockMvc.perform(post("/api/v1/integrations/weather/recommendations")
                        .with(TestAuth.weatherService())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clearedBody(2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLEARED"))
                .andExpect(jsonPath("$.sourceVersion").value(2))
                .andExpect(jsonPath("$.operatorStatus").value("LINKED"))
                .andExpect(jsonPath("$.version").value(3));

        mockMvc.perform(get("/api/v1/operator/weather/recommendations/" + RECOMMENDATION_ID).with(TestAuth.operator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLEARED"));

        mockMvc.perform(get("/api/v1/attractions/mangrove-run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WEATHER_HOLD"));

        JsonNode listed = jsonMapper.readTree(
                mockMvc.perform(get("/api/v1/operator/weather/recommendations").with(TestAuth.operator()))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsByteArray()
        );
        assertThat(listed.size()).isEqualTo(1);
    }

    private static String activeBody(long version) {
        return """
                {
                  "recommendationId": "%s",
                  "ruleId": "simulated-lightning-hold",
                  "status": "ACTIVE",
                  "severity": "WARNING",
                  "summary": "Place Mangrove Run and Cypress Coil on weather hold",
                  "evidence": "Simulated lightning strike 1.2 miles from the western basin; this is a demonstration signal, not an NWS observation.",
                  "recommendedAction": "Place Mangrove Run and Cypress Coil on weather hold",
                  "affectedAttractionIds": ["mangrove-run", "cypress-coil"],
                  "observedAt": "2026-09-01T16:00:00Z",
                  "version": %d
                }
                """.formatted(RECOMMENDATION_ID, version);
    }

    private static String clearedBody(long version) {
        return """
                {
                  "recommendationId": "%s",
                  "ruleId": "simulated-lightning-hold",
                  "status": "CLEARED",
                  "severity": "INFO",
                  "summary": "Lightning hold can be reviewed for clearance",
                  "evidence": "Simulated lightning is outside the hold radius.",
                  "recommendedAction": "Review weather holds for return-to-service",
                  "affectedAttractionIds": ["mangrove-run", "cypress-coil"],
                  "observedAt": "2026-09-01T16:30:00Z",
                  "version": %d
                }
                """.formatted(RECOMMENDATION_ID, version);
    }
}
