package com.deanwagman.lumenmarsh.venueops.attraction.domain;

import java.util.Objects;

public record ExperienceMedia(String heroUrl, String thumbnailUrl, String altText) {

    public ExperienceMedia {
        heroUrl = requireText(heroUrl, "heroUrl");
        thumbnailUrl = requireText(thumbnailUrl, "thumbnailUrl");
        altText = requireText(altText, "altText");
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
