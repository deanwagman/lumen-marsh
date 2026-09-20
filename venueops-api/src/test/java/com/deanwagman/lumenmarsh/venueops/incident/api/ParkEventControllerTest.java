package com.deanwagman.lumenmarsh.venueops.incident.api;

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
import com.deanwagman.lumenmarsh.venueops.attraction.infrastructure.streaming.SseAttractionUpdateBroadcaster;
import com.deanwagman.lumenmarsh.venueops.flow.application.FlowQueryService;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentService;
import com.deanwagman.lumenmarsh.venueops.security.ImportVenueOpsSecurity;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ImportVenueOpsSecurity
@WebMvcTest(controllers = ParkEventController.class)
@Import({SseAttractionUpdateBroadcaster.class, ParkEventControllerTest.MetricsConfig.class})
class ParkEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SseAttractionUpdateBroadcaster broadcaster;

    @MockitoBean
    private GuestAttractionReadService guestAttractionReadService;

    @MockitoBean
    private IncidentService incidentService;

    @MockitoBean
    private FlowQueryService flowQueries;

    @Test
    void parkStreamSendsAttractionAndAdvisorySnapshots() throws Exception {
        when(guestAttractionReadService.listForGuest()).thenReturn(List.of(
                new GuestAttractionReadService.AttractionDetail(operatingMangroveRun(), mangroveRunExperience())
        ));
        when(incidentService.listActiveGuestAdvisories()).thenReturn(List.of());
        when(flowQueries.guestOverview()).thenReturn(new FlowQueryService.GuestOverview(
                List.of(),
                List.of(),
                Instant.parse("2026-09-15T18:00:00Z"),
                true
        ));

        MvcResult result = mockMvc.perform(get("/api/v1/events").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-cache, no-store"))
                .andExpect(header().string("X-Accel-Buffering", "no"))
                .andReturn();

        broadcaster.completeAll();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("event:attractions.snapshot")))
                .andExpect(content().string(containsString("event:advisories.snapshot")))
                .andExpect(content().string(containsString("event:guest.flow.updated")))
                .andExpect(content().string(containsString("\"id\":\"mangrove-run\"")));
    }

    private static Attraction operatingMangroveRun() {
        return Attraction.rehydrate(
                new AttractionId("mangrove-run"),
                "Mangrove Run",
                "Luminous Wetlands",
                AttractionType.BOAT_EXPEDITION,
                AttractionStatus.OPERATING,
                CapacityMode.NORMAL,
                25,
                Instant.parse("2026-09-01T15:30:00Z"),
                0L,
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

    @TestConfiguration
    static class MetricsConfig {
        @Bean
        SimpleMeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}
