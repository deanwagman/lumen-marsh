package com.deanwagman.lumenmarsh.venueops.attraction.application;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.Attraction;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionExperienceProfile;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatus;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionType;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.CapacityMode;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.Environment;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.ExperienceMedia;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.Intensity;
import com.deanwagman.lumenmarsh.venueops.attraction.infrastructure.InMemoryAttractionExperienceRepository;
import com.deanwagman.lumenmarsh.venueops.attraction.infrastructure.InMemoryAttractionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuestAttractionReadServiceTest {

    private static final AttractionId MANGROVE_RUN = new AttractionId("mangrove-run");

    private InMemoryAttractionRepository attractions;
    private InMemoryAttractionExperienceRepository experiences;
    private GuestAttractionReadService service;

    @BeforeEach
    void setUp() {
        attractions = new InMemoryAttractionRepository();
        experiences = new InMemoryAttractionExperienceRepository();
        service = new GuestAttractionReadService(attractions, experiences);

        attractions.save(Attraction.rehydrate(
                MANGROVE_RUN,
                "Mangrove Run",
                "Luminous Wetlands",
                AttractionType.BOAT_EXPEDITION,
                AttractionStatus.OPERATING,
                CapacityMode.NORMAL,
                25,
                Instant.parse("2026-08-27T14:30:00Z"),
                0L,
                List.of()
        ));
        experiences.save(AttractionExperienceProfile.create(
                MANGROVE_RUN,
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
        ));
    }

    @Test
    void listForGuestComposesOperationalAndExperienceProfiles() {
        var summaries = service.listForGuest();

        assertThat(summaries).hasSize(1);
        assertThat(summaries.getFirst().operational().waitMinutes()).isEqualTo(25);
        assertThat(summaries.getFirst().experience().media().thumbnailUrl())
                .isEqualTo("/media/attractions/mangrove-run/thumbnail.webp");
    }

    @Test
    void getDetailComposesOperationalAndExperienceProfiles() {
        GuestAttractionReadService.AttractionDetail detail = service.getDetail(MANGROVE_RUN);

        assertThat(detail.operational().waitMinutes()).isEqualTo(25);
        assertThat(detail.experience().shortDescription())
                .isEqualTo("Glide beneath a living canopy through the luminous wetlands.");
        assertThat(detail.experience().minimumHeightInches()).isNull();
    }

    @Test
    void getDetailFailsWhenExperienceProfileIsMissing() {
        assertThatThrownBy(() -> service.getDetail(new AttractionId("missing-ride")))
                .isInstanceOf(AttractionNotFoundException.class);
    }
}
