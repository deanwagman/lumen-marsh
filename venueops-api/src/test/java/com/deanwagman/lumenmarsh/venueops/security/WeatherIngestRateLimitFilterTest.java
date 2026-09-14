package com.deanwagman.lumenmarsh.venueops.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "venueops.attractions.seed=true",
        "venueops.security.weather-ingest-rate-limit-per-minute=2"
})
@AutoConfigureMockMvc
class WeatherIngestRateLimitFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void thirdIngestInTheWindowIsRejected() throws Exception {
        mockMvc.perform(ingest("rate-limit-rec-1")).andExpect(status().isCreated());
        mockMvc.perform(ingest("rate-limit-rec-2")).andExpect(status().isCreated());
        mockMvc.perform(ingest("rate-limit-rec-3"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.title").value("Too Many Requests"));
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder ingest(String id) {
        return post("/api/v1/integrations/weather/recommendations")
                .with(TestAuth.weatherService())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "recommendationId":"%s",
                          "ruleId":"r",
                          "status":"ACTIVE",
                          "severity":"INFO",
                          "summary":"s",
                          "evidence":"e",
                          "recommendedAction":"a",
                          "affectedAttractionIds":[],
                          "observedAt":"2026-09-01T16:00:00Z",
                          "version":1
                        }
                        """.formatted(id));
    }
}
