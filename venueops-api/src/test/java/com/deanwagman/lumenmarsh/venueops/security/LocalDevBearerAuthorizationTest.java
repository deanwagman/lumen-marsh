package com.deanwagman.lumenmarsh.venueops.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.deanwagman.lumenmarsh.venueops.testsupport.CommandJson;
import tools.jackson.databind.json.JsonMapper;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "venueops.attractions.seed=true")
@AutoConfigureMockMvc
@DirtiesContext
class LocalDevBearerAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void limitedOperatorTokenCannotPublishButSupervisorTokenCan() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/operator/incidents")
                        .header(HttpHeaders.AUTHORIZATION, bearer(LocalDevBearerAuthenticationFilter.OPERATOR_LIMITED_TOKEN))
                        .header("X-Actor", "spoofed-attacker")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Opaque token weather incident",
                                  "type": "WEATHER",
                                  "severity": "MAJOR",
                                  "internalDescription": "INTERNAL ONLY — must never reach guests",
                                  "attractionIds": ["mangrove-run"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String incidentId = jsonMapper.readTree(created.getResponse().getContentAsByteArray()).get("id").asString();

        mockMvc.perform(get("/api/v1/operator/incidents/" + incidentId + "/activity")
                        .header(HttpHeaders.AUTHORIZATION, bearer(LocalDevBearerAuthenticationFilter.OPERATOR_LIMITED_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actor").value("Local Limited Operator"));

        mockMvc.perform(post("/api/v1/operator/incidents/" + incidentId + "/commands")
                        .header(HttpHeaders.AUTHORIZATION, bearer(LocalDevBearerAuthenticationFilter.OPERATOR_LIMITED_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CommandJson.envelope("""
                                {
                                  "type":"PUBLISH_GUEST_ADVISORY",
                                  "guestTitle":"Should not publish",
                                  "guestMessage":"Limited operators cannot publish.",
                                  "expectedVersion":1
                                }
                                """)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/operator/incidents/" + incidentId + "/commands")
                        .header(HttpHeaders.AUTHORIZATION, bearer(LocalDevBearerAuthenticationFilter.CONSOLE_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CommandJson.envelope("""
                                {
                                  "type":"PUBLISH_GUEST_ADVISORY",
                                  "guestTitle":"Outdoor weather pause",
                                  "guestMessage":"Some outdoor attractions are temporarily paused.",
                                  "expectedVersion":1
                                }
                                """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guestAdvisoryPublished").value(true));

        mockMvc.perform(get("/api/v1/advisories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + incidentId + "')].title", hasItem("Outdoor weather pause")))
                .andExpect(jsonPath("$[*].internalDescription").doesNotExist())
                .andExpect(jsonPath("$[*].assignedTo").doesNotExist())
                .andExpect(content().string(not(containsString("INTERNAL ONLY — must never reach guests"))))
                .andExpect(content().string(not(containsString("\"internalDescription\""))));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
