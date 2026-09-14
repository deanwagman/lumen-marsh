package com.deanwagman.lumenmarsh.venueops.attraction.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttractionExperienceProfileTest {

    private static final ExperienceMedia MEDIA = new ExperienceMedia(
            "/media/attractions/mangrove-run/hero.webp",
            "/media/attractions/mangrove-run/thumbnail.webp",
            "An expedition boat moving through a glowing mangrove forest."
    );

    @Test
    void createAcceptsNullMinimumHeight() {
        AttractionExperienceProfile profile = AttractionExperienceProfile.create(
                new AttractionId("mangrove-run"),
                "Glide beneath a living canopy through the luminous wetlands.",
                8,
                null,
                Intensity.GENTLE,
                Environment.OUTDOOR,
                false,
                "Guests must transfer into the ride vehicle.",
                MEDIA
        );

        assertThat(profile.minimumHeightInches()).isNull();
        assertThat(profile.intensity()).isEqualTo(Intensity.GENTLE);
    }

    @Test
    void createRejectsBlankCopy() {
        assertThatThrownBy(() -> AttractionExperienceProfile.create(
                new AttractionId("mangrove-run"),
                " ",
                8,
                null,
                Intensity.GENTLE,
                Environment.OUTDOOR,
                false,
                "Guests must transfer into the ride vehicle.",
                MEDIA
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
