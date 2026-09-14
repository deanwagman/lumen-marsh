package com.deanwagman.lumenmarsh.venueops.attraction.api;

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
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ImportVenueOpsSecurity
@WebMvcTest(controllers = AttractionEventController.class)
@Import({SseAttractionUpdateBroadcaster.class, AttractionEventControllerTest.MetricsConfig.class})
class AttractionEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SseAttractionUpdateBroadcaster broadcaster;

    @MockitoBean
    private GuestAttractionReadService guestAttractionReadService;

    @Test
    void newConnectionReceivesGuestSafeSnapshot() throws Exception {
        when(guestAttractionReadService.listForGuest()).thenReturn(List.of(
                new GuestAttractionReadService.AttractionDetail(operatingMangroveRun(), mangroveRunExperience())
        ));

        MvcResult result = mockMvc.perform(get("/api/v1/attractions/events").accept(MediaType.TEXT_EVENT_STREAM))
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
                .andExpect(content().string(containsString("\"id\":\"mangrove-run\"")))
                .andExpect(content().string(containsString("\"thumbnailUrl\"")))
                .andExpect(content().string(not(containsString("\"actor\""))))
                .andExpect(content().string(not(containsString("Lightning"))));
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
                Instant.parse("2026-08-27T19:20:00Z"),
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
