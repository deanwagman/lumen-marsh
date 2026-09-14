package com.deanwagman.lumenmarsh.venueops.attraction;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "venueops.attractions.seed=false")
@AutoConfigureMockMvc
class AttractionSeedDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void doesNotSeedAttractionsWhenDisabled() throws Exception {
        mockMvc.perform(get("/api/v1/attractions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/v1/attractions/mangrove-run"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ATTRACTION_NOT_FOUND"));
    }
}
