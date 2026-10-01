package com.deanwagman.lumenmarsh.venueops.attraction;

import com.deanwagman.lumenmarsh.venueops.security.TestAuth;
import com.deanwagman.lumenmarsh.venueops.testsupport.CommandJson;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
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
class AttractionPostgresIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("venueops")
            .withUsername("venueops")
            .withPassword("venueops");

    @Autowired
    private MockMvc mockMvc;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void persistsWeatherHoldRecoveryAndRejectsStaleCommands() throws Exception {
        mockMvc.perform(get("/api/v1/attractions/mangrove-run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPERATING"))
                .andExpect(jsonPath("$.waitMinutes").value(25))
                .andExpect(jsonPath("$.version").value(0))
                .andExpectAll(MangroveRunExperienceExpectations.detailExperienceFields());

        command("PLACE_WEATHER_HOLD", "Lightning nearby", 99, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STALE_VERSION"));

        command("PLACE_WEATHER_HOLD", "Lightning detected within operating radius", 0, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WEATHER_HOLD"))
                .andExpect(jsonPath("$.waitMinutes").isEmpty())
                .andExpect(jsonPath("$.capacityMode").value("NOT_APPLICABLE"));

        mockMvc.perform(get("/api/v1/attractions/mangrove-run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusMessage").value("Temporarily unavailable due to nearby weather."));

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
                .andExpect(jsonPath("$.capacityMode").value("NORMAL"));

        MvcResult activity = mockMvc.perform(get("/api/v1/operator/attractions/mangrove-run/activity").with(TestAuth.operator()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode events = jsonMapper.readTree(activity.getResponse().getContentAsByteArray());
        assertThat(events).hasSize(4);
        assertThat(events.get(0).get("type").asString()).isEqualTo("WEATHER_HOLD_PLACED");
        assertThat(events.get(3).get("type").asString()).isEqualTo("ATTRACTION_OPENED");
        assertThat(events.get(0).get("previousStatus").asString()).isEqualTo("OPERATING");
        assertThat(events.get(0).get("newStatus").asString()).isEqualTo("WEATHER_HOLD");
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
                .content(CommandJson.envelope(body.toString())));
    }
}
