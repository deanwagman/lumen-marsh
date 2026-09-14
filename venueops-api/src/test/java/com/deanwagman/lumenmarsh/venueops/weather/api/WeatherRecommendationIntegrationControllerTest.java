package com.deanwagman.lumenmarsh.venueops.weather.api;

import com.deanwagman.lumenmarsh.venueops.weather.application.WeatherRecommendationIngestResult;
import com.deanwagman.lumenmarsh.venueops.weather.application.WeatherRecommendationService;
import com.deanwagman.lumenmarsh.venueops.weather.domain.StaleWeatherRecommendationSourceException;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendation;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationId;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationInbound;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationSeverity;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationStatus;
import com.deanwagman.lumenmarsh.venueops.security.ImportVenueOpsSecurity;
import com.deanwagman.lumenmarsh.venueops.security.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ImportVenueOpsSecurity
@WebMvcTest(controllers = {
        WeatherRecommendationIntegrationController.class,
        WeatherRecommendationExceptionHandler.class
})
class WeatherRecommendationIntegrationControllerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-01T16:00:00Z"), ZoneOffset.UTC);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WeatherRecommendationService weatherRecommendationService;

    @Test
    void ingestCreatesWithServiceToken() throws Exception {
        when(weatherRecommendationService.ingest(any())).thenReturn(
                WeatherRecommendationIngestResult.created(received())
        );

        mockMvc.perform(post("/api/v1/integrations/weather/recommendations")
                        .with(TestAuth.weatherService())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ACTIVE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("rec-lightning-1"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.sourceVersion").value(1))
                .andExpect(jsonPath("$.simulated").value(true));
    }

    @Test
    void duplicateIngestReturnsCurrentRecord() throws Exception {
        when(weatherRecommendationService.ingest(any())).thenReturn(
                WeatherRecommendationIngestResult.duplicate(received())
        );

        mockMvc.perform(post("/api/v1/integrations/weather/recommendations")
                        .with(TestAuth.weatherService())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ACTIVE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void staleSourceVersionReturnsConflict() throws Exception {
        when(weatherRecommendationService.ingest(any())).thenThrow(
                new StaleWeatherRecommendationSourceException(
                        new WeatherRecommendationId("rec-lightning-1"),
                        1L,
                        2L
                )
        );

        mockMvc.perform(post("/api/v1/integrations/weather/recommendations")
                        .with(TestAuth.weatherService())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ACTIVE_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STALE_VERSION"));
    }

    private static WeatherRecommendation received() {
        return WeatherRecommendation.receive(new WeatherRecommendationInbound(
                new WeatherRecommendationId("rec-lightning-1"),
                "simulated-lightning-hold",
                WeatherRecommendationStatus.ACTIVE,
                WeatherRecommendationSeverity.WARNING,
                "Place Mangrove Run and Cypress Coil on weather hold",
                "Simulated lightning strike 1.2 miles from the western basin.",
                "Place Mangrove Run and Cypress Coil on weather hold",
                List.of(),
                Instant.parse("2026-09-01T16:00:00Z"),
                1L
        ), CLOCK);
    }

    private static final String ACTIVE_BODY = """
            {
              "recommendationId": "rec-lightning-1",
              "ruleId": "simulated-lightning-hold",
              "status": "ACTIVE",
              "severity": "WARNING",
              "summary": "Place Mangrove Run and Cypress Coil on weather hold",
              "evidence": "Simulated lightning strike 1.2 miles from the western basin.",
              "recommendedAction": "Place Mangrove Run and Cypress Coil on weather hold",
              "affectedAttractionIds": ["mangrove-run", "cypress-coil"],
              "observedAt": "2026-09-01T16:00:00Z",
              "version": 1
            }
            """;
}
