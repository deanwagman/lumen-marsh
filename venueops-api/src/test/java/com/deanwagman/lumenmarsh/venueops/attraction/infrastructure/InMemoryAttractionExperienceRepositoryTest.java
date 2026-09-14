package com.deanwagman.lumenmarsh.venueops.attraction.infrastructure;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionExperienceProfile;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.Environment;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.ExperienceMedia;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.Intensity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryAttractionExperienceRepositoryTest {

    private InMemoryAttractionExperienceRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAttractionExperienceRepository();
    }

    @Test
    void saveAndFindByAttractionIdReturnsDefensiveCopy() {
        AttractionId id = new AttractionId("mangrove-run");
        repository.save(AttractionExperienceProfile.create(
                id,
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

        AttractionExperienceProfile stored = repository.findByAttractionId(id).orElseThrow();
        assertThat(stored.shortDescription())
                .isEqualTo("Glide beneath a living canopy through the luminous wetlands.");
        assertThat(repository.findAll()).hasSize(1);
    }
}
