package com.deanwagman.lumenmarsh.venueops.weather.api;

import com.deanwagman.lumenmarsh.venueops.weather.application.WeatherRecommendationNotFoundException;
import com.deanwagman.lumenmarsh.venueops.weather.application.WeatherRecommendationService;
import com.deanwagman.lumenmarsh.venueops.weather.domain.InvalidWeatherRecommendationTransitionException;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendation;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationCommand;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationId;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationInbound;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationOperatorStatus;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ImportVenueOpsSecurity
@WebMvcTest(controllers = {
        OperatorWeatherRecommendationController.class,
        WeatherRecommendationExceptionHandler.class
})
class OperatorWeatherRecommendationControllerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-01T16:00:00Z"), ZoneOffset.UTC);
    private static final WeatherRecommendationId ID = new WeatherRecommendationId("rec-lightning-1");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WeatherRecommendationService weatherRecommendationService;

    @Test
    void listReturnsInbox() throws Exception {
        when(weatherRecommendationService.list()).thenReturn(List.of(received()));

        mockMvc.perform(get("/api/v1/operator/weather/recommendations").with(TestAuth.operator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("rec-lightning-1"))
                .andExpect(jsonPath("$[0].operatorStatus").value("PENDING"));
    }

    @Test
    void missingRecommendationReturns404() throws Exception {
        when(weatherRecommendationService.get(ID)).thenThrow(new WeatherRecommendationNotFoundException(ID));

        mockMvc.perform(get("/api/v1/operator/weather/recommendations/rec-lightning-1").with(TestAuth.operator()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WEATHER_RECOMMENDATION_NOT_FOUND"));
    }

    @Test
    void acknowledgeAdvancesOperatorStatus() throws Exception {
        WeatherRecommendation acknowledged = received();
        acknowledged.acknowledge("Operator One", null, CLOCK);
        when(weatherRecommendationService.execute(
                eq(ID),
                eq(WeatherRecommendationCommand.ACKNOWLEDGE),
                eq("Operator One"),
                isNull(),
                eq(1L),
                isNull()
        )).thenReturn(acknowledged);

        mockMvc.perform(post("/api/v1/operator/weather/recommendations/rec-lightning-1/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "ACKNOWLEDGE",
                                  "expectedVersion": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operatorStatus").value("ACKNOWLEDGED"));
    }

    @Test
    void invalidTransitionReturnsConflict() throws Exception {
        when(weatherRecommendationService.execute(
                eq(ID),
                eq(WeatherRecommendationCommand.LINK_INCIDENT),
                eq("Operator One"),
                isNull(),
                eq(1L),
                eq("inc-1")
        )).thenThrow(new InvalidWeatherRecommendationTransitionException(
                WeatherRecommendationOperatorStatus.DISMISSED,
                WeatherRecommendationCommand.LINK_INCIDENT
        ));

        mockMvc.perform(post("/api/v1/operator/weather/recommendations/rec-lightning-1/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "LINK_INCIDENT",
                                  "incidentId": "inc-1",
                                  "expectedVersion": 1
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
    }

    private static WeatherRecommendation received() {
        return WeatherRecommendation.receive(new WeatherRecommendationInbound(
                ID,
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
}
