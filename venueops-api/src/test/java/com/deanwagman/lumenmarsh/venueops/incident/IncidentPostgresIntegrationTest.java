package com.deanwagman.lumenmarsh.venueops.incident;

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
class IncidentPostgresIntegrationTest {

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
    void persistsIncidentLifecycleAndRejectsStaleCommands() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/operator/incidents")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Lightning activity near western basin",
                                  "type": "WEATHER",
                                  "severity": "MAJOR",
                                  "internalDescription": "Repeated strikes detected within the hold radius.",
                                  "attractionIds": ["mangrove-run"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REPORTED"))
                .andExpect(jsonPath("$.version").value(1))
                .andReturn();
        String incidentId = jsonMapper.readTree(created.getResponse().getContentAsByteArray()).get("id").asString();

        command(incidentId, "{\"type\":\"ACKNOWLEDGE\",\"expectedVersion\":99}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STALE_VERSION"));

        command(incidentId, "{\"type\":\"ACKNOWLEDGE\",\"expectedVersion\":1}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.version").value(2));

        command(incidentId, """
                {"type":"LINK_ATTRACTION","attractionId":"cypress-coil","expectedVersion":2}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attractionIds[1]").value("cypress-coil"));

        command(incidentId, "{\"type\":\"START_MITIGATION\",\"expectedVersion\":3}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MITIGATING"));

        command(incidentId, "{\"type\":\"RESOLVE\",\"reason\":\"Storm cell moved out of radius\",\"expectedVersion\":4}", TestAuth.supervisor())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        mockMvc.perform(get("/api/v1/operator/incidents/" + incidentId).with(TestAuth.operator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.version").value(5));

        MvcResult activity = mockMvc.perform(get("/api/v1/operator/incidents/" + incidentId + "/activity").with(TestAuth.operator()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode events = jsonMapper.readTree(activity.getResponse().getContentAsByteArray());
        assertThat(events).hasSize(5);
        assertThat(events.get(0).get("type").asString()).isEqualTo("INCIDENT_REPORTED");
        assertThat(events.get(4).get("type").asString()).isEqualTo("INCIDENT_RESOLVED");
        assertThat(events.get(4).get("previousStatus").asString()).isEqualTo("MITIGATING");
        assertThat(events.get(4).get("newStatus").asString()).isEqualTo("RESOLVED");
    }

    private org.springframework.test.web.servlet.ResultActions command(String incidentId, String body) throws Exception {
        return command(incidentId, body, TestAuth.operator());
    }

    private org.springframework.test.web.servlet.ResultActions command(
            String incidentId,
            String body,
            org.springframework.test.web.servlet.request.RequestPostProcessor auth
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/operator/incidents/" + incidentId + "/commands")
                .with(auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(CommandJson.envelope(body)));
    }
}

