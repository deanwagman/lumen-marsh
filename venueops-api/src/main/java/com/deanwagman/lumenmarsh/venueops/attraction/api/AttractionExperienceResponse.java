package com.deanwagman.lumenmarsh.venueops.attraction.api;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionExperienceProfile;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.Environment;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.Intensity;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AttractionExperience")
public record AttractionExperienceResponse(
        @Schema(example = "Glide beneath a living canopy through the luminous wetlands.")
        String shortDescription,
        @Schema(example = "8") int durationMinutes,
        @Schema(nullable = true, example = "null") Integer minimumHeightInches,
        Intensity intensity,
        Environment environment,
        boolean singleRiderAvailable,
        @Schema(example = "Guests must transfer into the ride vehicle.") String accessibilitySummary,
        ExperienceMediaResponse media
) {
    public static AttractionExperienceResponse from(AttractionExperienceProfile profile) {
        return new AttractionExperienceResponse(
                profile.shortDescription(),
                profile.durationMinutes(),
                profile.minimumHeightInches(),
                profile.intensity(),
                profile.environment(),
                profile.singleRiderAvailable(),
                profile.accessibilitySummary(),
                ExperienceMediaResponse.from(profile.media())
        );
    }
}
