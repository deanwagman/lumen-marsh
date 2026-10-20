package com.deanwagman.lumenmarsh.venueops.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "venueops.demo-sandbox.enabled=false")
@AutoConfigureMockMvc
class DemoSandboxDisabledTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void demoAndResetEndpointsDoNotExistByDefault() throws Exception {
        mockMvc.perform(get("/api/v1/demo/lease")).andExpect(status().isNotFound());
        mockMvc.perform(post("/internal/demo/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resetId\":\"8ad6ff18-dbbc-4c55-a190-a8f4fdd24cde\"}"))
                .andExpect(status().isNotFound());
    }
}
