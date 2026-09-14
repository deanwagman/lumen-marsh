package com.deanwagman.lumenmarsh.venueops.incident;

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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "venueops.attractions.seed=true",
        "venueops.test.fixture=incident-demo"
})
@AutoConfigureMockMvc
@DirtiesContext
class IncidentDemoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void weatherIncidentAdvisoryAndAttractionHoldsStayIndependent() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/operator/incidents")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Lightning activity near western basin",
                                  "type": "WEATHER",
                                  "severity": "MAJOR",
                                  "internalDescription": "Repeated strikes detected within the hold radius."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REPORTED"))
                .andReturn();
        String incidentId = jsonMapper.readTree(created.getResponse().getContentAsByteArray()).get("id").asString();

        incidentCommand(incidentId, "{\"type\":\"LINK_ATTRACTION\",\"attractionId\":\"mangrove-run\",\"expectedVersion\":1}")
                .andExpect(status().isOk());
        incidentCommand(incidentId, "{\"type\":\"LINK_ATTRACTION\",\"attractionId\":\"cypress-coil\",\"expectedVersion\":2}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attractionIds[0]").value("mangrove-run"))
                .andExpect(jsonPath("$.attractionIds[1]").value("cypress-coil"));

        mockMvc.perform(get("/api/v1/attractions/mangrove-run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPERATING"));

        incidentCommand(incidentId, "{\"type\":\"ACKNOWLEDGE\",\"expectedVersion\":3}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));
        incidentCommand(incidentId, "{\"type\":\"ASSIGN\",\"assignee\":\"Control Tower\",\"expectedVersion\":4}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedTo").value("Control Tower"));
        incidentCommand(incidentId, """
                {
                  "type":"PUBLISH_GUEST_ADVISORY",
                  "guestTitle":"Weather advisory",
                  "guestMessage":"Some outdoor attractions are temporarily paused.",
                  "expectedVersion":5
                }
                """, TestAuth.supervisor())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guestAdvisoryPublished").value(true));
        incidentCommand(incidentId, "{\"type\":\"START_MITIGATION\",\"expectedVersion\":6}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MITIGATING"));

        mockMvc.perform(get("/api/v1/advisories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Weather advisory"))
                .andExpect(jsonPath("$[0].message").value("Some outdoor attractions are temporarily paused."))
                .andExpect(jsonPath("$[0].internalDescription").doesNotExist())
                .andExpect(jsonPath("$[0].assignedTo").doesNotExist())
                .andExpect(contentWithout("Repeated strikes"));

        attractionCommand("mangrove-run", """
                {"type":"PLACE_WEATHER_HOLD","reason":"Lightning detected within operating radius","expectedVersion":0}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WEATHER_HOLD"));

        attractionCommand("cypress-coil", "{\"type\":\"START_TESTING\",\"expectedVersion\":0}").andExpect(status().isOk());
        attractionCommand("cypress-coil", "{\"type\":\"COMPLETE_TESTING\",\"expectedVersion\":1}").andExpect(status().isOk());
        attractionCommand("cypress-coil", """
                {"type":"APPROVE_RETURN_TO_SERVICE","reason":"Opened to apply weather hold","expectedVersion":2}
                """).andExpect(status().isOk());
        attractionCommand("cypress-coil", """
                {"type":"PLACE_WEATHER_HOLD","reason":"Lightning detected within operating radius","expectedVersion":3}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WEATHER_HOLD"));

        MvcResult incidentActivity = mockMvc.perform(get("/api/v1/operator/incidents/" + incidentId + "/activity")
                .with(TestAuth.operator()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode incidentEvents = jsonMapper.readTree(incidentActivity.getResponse().getContentAsByteArray());
        assertThat(incidentEvents.get(0).get("type").asString()).isEqualTo("INCIDENT_REPORTED");
        assertThat(incidentEvents.get(6).get("type").asString()).isEqualTo("MITIGATION_STARTED");

        mockMvc.perform(get("/api/v1/operator/attractions/mangrove-run/activity")
                .with(TestAuth.operator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("WEATHER_HOLD_PLACED"));

        incidentCommand(incidentId, "{\"type\":\"RESOLVE\",\"reason\":\"Storm cell moved out of radius\",\"expectedVersion\":7}", TestAuth.supervisor())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.guestAdvisoryPublished").value(false));

        mockMvc.perform(get("/api/v1/advisories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(get("/api/v1/attractions/mangrove-run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WEATHER_HOLD"));

        attractionCommand("mangrove-run", """
                {"type":"CLEAR_WEATHER_HOLD","reason":"Storm cell moved out of radius","expectedVersion":1}
                """).andExpect(status().isOk());
        attractionCommand("mangrove-run", "{\"type\":\"COMPLETE_TESTING\",\"expectedVersion\":2}").andExpect(status().isOk());
        attractionCommand("mangrove-run", """
                {"type":"APPROVE_RETURN_TO_SERVICE","reason":"Return to service approved","expectedVersion":3}
                """).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPERATING"));

        attractionCommand("cypress-coil", """
                {"type":"CLEAR_WEATHER_HOLD","reason":"Storm cell moved out of radius","expectedVersion":4}
                """).andExpect(status().isOk());
        attractionCommand("cypress-coil", "{\"type\":\"COMPLETE_TESTING\",\"expectedVersion\":5}").andExpect(status().isOk());
        attractionCommand("cypress-coil", """
                {"type":"APPROVE_RETURN_TO_SERVICE","reason":"Return to service approved","expectedVersion":6}
                """).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPERATING"));
    }

    private org.springframework.test.web.servlet.ResultActions incidentCommand(String incidentId, String body) throws Exception {
        return incidentCommand(incidentId, body, TestAuth.operator());
    }

    private org.springframework.test.web.servlet.ResultActions incidentCommand(
            String incidentId,
            String body,
            org.springframework.test.web.servlet.request.RequestPostProcessor auth
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/operator/incidents/" + incidentId + "/commands")
                .with(auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private org.springframework.test.web.servlet.ResultActions attractionCommand(String attractionId, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/operator/attractions/" + attractionId + "/commands")
                .with(TestAuth.operator())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static org.springframework.test.web.servlet.ResultMatcher contentWithout(String leaked) {
        return jsonPath("$[0].message", not(containsString(leaked)));
    }
}
