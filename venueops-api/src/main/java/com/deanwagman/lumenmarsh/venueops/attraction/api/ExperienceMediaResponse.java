package com.deanwagman.lumenmarsh.venueops.attraction.api;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.ExperienceMedia;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ExperienceMedia")
public record ExperienceMediaResponse(
        @Schema(example = "/media/attractions/mangrove-run/hero.webp") String heroUrl,
        @Schema(example = "/media/attractions/mangrove-run/thumbnail.webp") String thumbnailUrl,
        @Schema(example = "An expedition boat moving through a glowing mangrove forest.") String altText
) {
    public static ExperienceMediaResponse from(ExperienceMedia media) {
        return new ExperienceMediaResponse(media.heroUrl(), media.thumbnailUrl(), media.altText());
    }
}
