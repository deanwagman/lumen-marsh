package com.deanwagman.lumenmarsh.venueops.attraction.api;

import com.deanwagman.lumenmarsh.venueops.WebCorsConfiguration;
import com.deanwagman.lumenmarsh.venueops.security.ImportVenueOpsSecurity;
import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionNotFoundException;
import com.deanwagman.lumenmarsh.venueops.attraction.application.GuestAttractionReadService;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.Attraction;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionExperienceProfile;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatus;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionType;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.CapacityMode;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.Environment;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.ExperienceMedia;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.Intensity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ImportVenueOpsSecurity
@WebMvcTest(controllers = {AttractionController.class, AttractionExceptionHandler.class})
@Import(WebCorsConfiguration.class)
class AttractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GuestAttractionReadService guestAttractionReadService;

    @Test
    void listReturnsGuestSafeRepresentations() throws Exception {
        when(guestAttractionReadService.listForGuest()).thenReturn(List.of(
                new GuestAttractionReadService.AttractionDetail(
                        weatherHoldMangroveRun(),
                        mangroveRunExperience()
                )
        ));

        mockMvc.perform(get("/api/v1/attractions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("mangrove-run"))
                .andExpect(jsonPath("$[0].type").value("BOAT_EXPEDITION"))
                .andExpect(jsonPath("$[0].status").value("WEATHER_HOLD"))
                .andExpect(jsonPath("$[0].capacityMode").value("NOT_APPLICABLE"))
                .andExpect(jsonPath("$[0].waitMinutes").isEmpty())
                .andExpect(jsonPath("$[0].statusMessage").value("Temporarily unavailable due to nearby weather."))
                .andExpect(jsonPath("$[0].version").value(8))
                .andExpect(jsonPath("$[0].thumbnailUrl")
                        .value("/media/attractions/mangrove-run/thumbnail.webp"))
                .andExpect(jsonPath("$[0].thumbnailAltText")
                        .value("An expedition boat moving through a glowing mangrove forest."))
                .andExpect(jsonPath("$[0].experience").doesNotExist());
    }

    @Test
    void listAllowsFlutterWebDevelopmentOrigin() throws Exception {
        when(guestAttractionReadService.listForGuest()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/attractions").header("Origin", "http://localhost:53521"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:53521"));
    }

    @Test
    void detailReturnsOperationalAndExperienceFields() throws Exception {
        when(guestAttractionReadService.getDetail(new AttractionId("mangrove-run")))
                .thenReturn(new GuestAttractionReadService.AttractionDetail(
                        weatherHoldMangroveRun(),
                        mangroveRunExperience()
                ));

        mockMvc.perform(get("/api/v1/attractions/mangrove-run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WEATHER_HOLD"))
                .andExpect(jsonPath("$.experience.shortDescription")
                        .value("Glide beneath a living canopy through the luminous wetlands."))
                .andExpect(jsonPath("$.experience.minimumHeightInches").isEmpty())
                .andExpect(jsonPath("$.experience.media.altText")
                        .value("An expedition boat moving through a glowing mangrove forest."));
    }

    @Test
    void unknownAttractionReturnsProblemDetail() throws Exception {
        AttractionId id = new AttractionId("missing-ride");
        when(guestAttractionReadService.getDetail(id)).thenThrow(new AttractionNotFoundException(id));

        mockMvc.perform(get("/api/v1/attractions/missing-ride"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ATTRACTION_NOT_FOUND"));
    }

    private static Attraction weatherHoldMangroveRun() {
        return Attraction.rehydrate(
                new AttractionId("mangrove-run"),
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

    private static AttractionExperienceProfile mangroveRunExperience() {
        return AttractionExperienceProfile.create(
                new AttractionId("mangrove-run"),
                "Glide beneath a living canopy through the luminous wetlands.",
                8,
                null,
                Intensity.GENTLE,
                Environment.OUTDOOR,
                false,
                "Guests must transfer into the ride vehicle.",
                new ExperienceMedia(
                        "/media/attractions/mangrove-run/hero.webp",
                        "/media/attractions/mangrove-run/thumbnail.webp",
                        "An expedition boat moving through a glowing mangrove forest."
                )
        );
    }
}
