package com.deanwagman.lumenmarsh.venueops.attraction.api;

import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionService;
import com.deanwagman.lumenmarsh.venueops.attraction.application.StaleAttractionVersionException;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.Attraction;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionCommand;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatus;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionType;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.CapacityMode;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.InvalidAttractionTransitionException;
import com.deanwagman.lumenmarsh.venueops.security.ImportVenueOpsSecurity;
import com.deanwagman.lumenmarsh.venueops.security.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ImportVenueOpsSecurity
@WebMvcTest(controllers = {OperatorAttractionController.class, AttractionExceptionHandler.class})
class OperatorAttractionControllerTest {

    private static final AttractionId MANGROVE_RUN = new AttractionId("mangrove-run");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AttractionService attractionService;

    @Test
    void commandReturnsUpdatedOperatorView() throws Exception {
        when(attractionService.execute(
                eq(MANGROVE_RUN),
                eq(AttractionCommand.PLACE_WEATHER_HOLD),
                eq("Operator One"),
                eq("Lightning detected within operating radius"),
                isNull(),
                eq(7L)
        )).thenReturn(weatherHold());

        mockMvc.perform(post("/api/v1/operator/attractions/mangrove-run/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "PLACE_WEATHER_HOLD",
                                  "reason": "Lightning detected within operating radius",
                                  "expectedVersion": 7
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("mangrove-run"))
                .andExpect(jsonPath("$.status").value("WEATHER_HOLD"))
                .andExpect(jsonPath("$.waitMinutes").isEmpty())
                .andExpect(jsonPath("$.statusMessage").doesNotExist())
                .andExpect(jsonPath("$.version").value(8));
    }

    @Test
    void invalidTransitionReturnsConflict() throws Exception {
        when(attractionService.execute(
                eq(MANGROVE_RUN),
                eq(AttractionCommand.APPROVE_RETURN_TO_SERVICE),
                eq("Operator One"),
                eq("Skip testing"),
                isNull(),
                eq(8L)
        )).thenThrow(new InvalidAttractionTransitionException(
                AttractionStatus.WEATHER_HOLD,
                AttractionCommand.APPROVE_RETURN_TO_SERVICE
        ));

        mockMvc.perform(post("/api/v1/operator/attractions/mangrove-run/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "APPROVE_RETURN_TO_SERVICE",
                                  "reason": "Skip testing",
                                  "expectedVersion": 8
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
    }

    @Test
    void staleVersionReturnsConflict() throws Exception {
        when(attractionService.execute(
                eq(MANGROVE_RUN),
                eq(AttractionCommand.PLACE_WEATHER_HOLD),
                eq("Operator One"),
                eq("Lightning nearby"),
                isNull(),
                eq(7L)
        )).thenThrow(new StaleAttractionVersionException(MANGROVE_RUN, 7L, 8L));

        mockMvc.perform(post("/api/v1/operator/attractions/mangrove-run/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "PLACE_WEATHER_HOLD",
                                  "reason": "Lightning nearby",
                                  "expectedVersion": 7
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STALE_VERSION"));
    }

    @Test
    void missingTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/operator/attractions/mangrove-run/commands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "PLACE_WEATHER_HOLD",
                                  "reason": "Lightning nearby",
                                  "expectedVersion": 7
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    private static Attraction weatherHold() {
        return Attraction.rehydrate(
                MANGROVE_RUN,
                "Mangrove Run",
                "Luminous Wetlands",
                AttractionType.BOAT_EXPEDITION,
                AttractionStatus.WEATHER_HOLD,
                CapacityMode.NOT_APPLICABLE,
                null,
                Instant.parse("2026-08-26T18:42:00Z"),
                8L,
                List.of()
        );
    }
}
