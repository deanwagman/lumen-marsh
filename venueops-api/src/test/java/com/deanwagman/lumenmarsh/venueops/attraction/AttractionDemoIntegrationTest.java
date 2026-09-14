package com.deanwagman.lumenmarsh.venueops.attraction;

import com.deanwagman.lumenmarsh.venueops.security.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "venueops.attractions.seed=true",
        "springdoc.api-docs.enabled=true",
        "venueops.security.expose-api-docs=true"
})
@AutoConfigureMockMvc
class AttractionDemoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void weatherHoldInterruptionAndRecovery() throws Exception {
        mockMvc.perform(get("/api/v1/attractions/mangrove-run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPERATING"))
                .andExpect(jsonPath("$.capacityMode").value("NORMAL"))
                .andExpect(jsonPath("$.waitMinutes").value(25))
                .andExpectAll(MangroveRunExperienceExpectations.detailExperienceFields());

        mockMvc.perform(get("/api/v1/attractions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].experience").doesNotExist())
                .andExpect(jsonPath("$[?(@.id == 'mangrove-run')].thumbnailUrl")
                        .value("/media/attractions/mangrove-run/thumbnail.webp"))
                .andExpect(jsonPath("$[?(@.id == 'mangrove-run')].thumbnailAltText")
                        .value("An expedition boat moving through a glowing mangrove forest."));

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("VenueOps API"))
                .andExpect(jsonPath("$.components.schemas.AttractionDetail").exists())
                .andExpect(jsonPath("$.components.schemas.AttractionExperience").exists());

        command("PLACE_WEATHER_HOLD", "Lightning detected within operating radius", 0, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WEATHER_HOLD"))
                .andExpect(jsonPath("$.waitMinutes").isEmpty())
                .andExpect(jsonPath("$.capacityMode").value("NOT_APPLICABLE"));

        mockMvc.perform(get("/api/v1/attractions/mangrove-run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusMessage").value("Temporarily unavailable due to nearby weather."))
                .andExpect(jsonPath("$.waitMinutes").isEmpty());

        command("APPROVE_RETURN_TO_SERVICE", "Skip testing", 1, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));

        command("CLEAR_WEATHER_HOLD", "Storm cell moved out of radius", 1, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TESTING"));

        command("COMPLETE_TESTING", null, 2, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNING_TO_SERVICE"));

        command("APPROVE_RETURN_TO_SERVICE", "Return to service approved", 3, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPERATING"))
                .andExpect(jsonPath("$.capacityMode").value("NORMAL"))
                .andExpect(jsonPath("$.waitMinutes").value(0));

        MvcResult activity = mockMvc.perform(get("/api/v1/operator/attractions/mangrove-run/activity")
                .with(TestAuth.operator()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode events = jsonMapper.readTree(activity.getResponse().getContentAsByteArray());
        assertThat(events).hasSize(4);
        assertThat(events.get(0).get("type").asString()).isEqualTo("WEATHER_HOLD_PLACED");
        assertThat(events.get(1).get("type").asString()).isEqualTo("WEATHER_HOLD_CLEARED");
        assertThat(events.get(2).get("type").asString()).isEqualTo("ATTRACTION_TESTING_COMPLETED");
        assertThat(events.get(3).get("type").asString()).isEqualTo("ATTRACTION_OPENED");
        assertThat(events.get(0).get("actor").asString()).isEqualTo("Operator One");
        assertThat(events.get(0).get("reason").asString()).contains("Lightning");
    }

    private org.springframework.test.web.servlet.ResultActions command(
            String type,
            String reason,
            long expectedVersion,
            Integer waitMinutes
    ) throws Exception {
        StringBuilder body = new StringBuilder("{");
        body.append("\"type\":\"").append(type).append("\",");
        if (reason != null) {
            body.append("\"reason\":\"").append(reason).append("\",");
        }
        if (waitMinutes != null) {
            body.append("\"waitMinutes\":").append(waitMinutes).append(",");
        }
        body.append("\"expectedVersion\":").append(expectedVersion).append("}");

        return mockMvc.perform(post("/api/v1/operator/attractions/mangrove-run/commands")
                .with(TestAuth.operator())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()));
    }
}
