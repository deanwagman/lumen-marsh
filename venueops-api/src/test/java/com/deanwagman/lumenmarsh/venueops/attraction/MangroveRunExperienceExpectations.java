package com.deanwagman.lumenmarsh.venueops.attraction;

import org.springframework.test.web.servlet.ResultMatcher;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

final class MangroveRunExperienceExpectations {

    private MangroveRunExperienceExpectations() {
    }

    static ResultMatcher[] detailExperienceFields() {
        return new ResultMatcher[] {
                jsonPath("$.experience.shortDescription")
                        .value("Glide beneath a living canopy through the luminous wetlands."),
                jsonPath("$.experience.durationMinutes").value(8),
                jsonPath("$.experience.minimumHeightInches").isEmpty(),
                jsonPath("$.experience.intensity").value("GENTLE"),
                jsonPath("$.experience.environment").value("OUTDOOR"),
                jsonPath("$.experience.singleRiderAvailable").value(false),
                jsonPath("$.experience.accessibilitySummary")
                        .value("Guests must transfer into the ride vehicle."),
                jsonPath("$.experience.media.heroUrl")
                        .value("/media/attractions/mangrove-run/hero.webp"),
                jsonPath("$.experience.media.thumbnailUrl")
                        .value("/media/attractions/mangrove-run/thumbnail.webp"),
                jsonPath("$.experience.media.altText")
                        .value("An expedition boat moving through a glowing mangrove forest.")
        };
    }
}
