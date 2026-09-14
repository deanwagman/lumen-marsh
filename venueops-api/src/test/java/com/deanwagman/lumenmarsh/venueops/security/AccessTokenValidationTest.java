package com.deanwagman.lumenmarsh.venueops.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "venueops.attractions.seed=true")
@AutoConfigureMockMvc
class AccessTokenValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalJwtTokenFactory tokens;

    @Test
    void operatorAccessTokenIsAccepted() throws Exception {
        mockMvc.perform(get("/api/v1/operator/attractions/mangrove-run")
                        .header("Authorization", "Bearer " + tokens.operatorToken()))
                .andExpect(status().isOk());
    }

    @Test
    void unknownClientIdIsRejected() throws Exception {
        String token = tokens.token(
                "operator-sub-1",
                "Operator One",
                List.of("operators"),
                VenueOpsScopes.OPERATOR_READ + " " + VenueOpsScopes.ATTRACTIONS_COMMAND,
                "unknown-client",
                "access"
        );
        mockMvc.perform(command(token)).andExpect(status().isUnauthorized());
    }

    @Test
    void idTokenUseIsRejected() throws Exception {
        String token = tokens.token(
                "operator-sub-1",
                "Operator One",
                List.of("operators"),
                VenueOpsScopes.OPERATOR_READ + " " + VenueOpsScopes.ATTRACTIONS_COMMAND,
                TestAuth.CONSOLE_CLIENT_ID,
                "id"
        );
        mockMvc.perform(command(token)).andExpect(status().isUnauthorized());
    }

    @Test
    void missingTokenUseIsRejected() throws Exception {
        String token = tokens.token(
                "operator-sub-1",
                "Operator One",
                List.of("operators"),
                VenueOpsScopes.OPERATOR_READ + " " + VenueOpsScopes.ATTRACTIONS_COMMAND,
                TestAuth.CONSOLE_CLIENT_ID,
                null
        );
        mockMvc.perform(command(token)).andExpect(status().isUnauthorized());
    }

    @Test
    void missingClientIdIsRejected() throws Exception {
        String token = tokens.token(
                "operator-sub-1",
                "Operator One",
                List.of("operators"),
                VenueOpsScopes.OPERATOR_READ + " " + VenueOpsScopes.ATTRACTIONS_COMMAND,
                null,
                "access"
        );
        mockMvc.perform(command(token)).andExpect(status().isUnauthorized());
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder command(String token) {
        return post("/api/v1/operator/attractions/mangrove-run/commands")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"type":"UPDATE_WAIT_TIME","waitMinutes":5,"expectedVersion":0}
                        """);
    }
}
