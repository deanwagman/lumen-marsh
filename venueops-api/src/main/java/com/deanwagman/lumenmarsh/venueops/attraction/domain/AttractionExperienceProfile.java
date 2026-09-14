package com.deanwagman.lumenmarsh.venueops.attraction.domain;

import java.util.Objects;

public final class AttractionExperienceProfile {

    private final AttractionId attractionId;
    private final String shortDescription;
    private final int durationMinutes;
    private final Integer minimumHeightInches;
    private final Intensity intensity;
    private final Environment environment;
    private final boolean singleRiderAvailable;
    private final String accessibilitySummary;
    private final ExperienceMedia media;

    private AttractionExperienceProfile(
            AttractionId attractionId,
            String shortDescription,
            int durationMinutes,
            Integer minimumHeightInches,
            Intensity intensity,
            Environment environment,
            boolean singleRiderAvailable,
            String accessibilitySummary,
            ExperienceMedia media
    ) {
        this.attractionId = Objects.requireNonNull(attractionId, "attractionId is required");
        this.shortDescription = requireText(shortDescription, "shortDescription");
        this.durationMinutes = requirePositiveDuration(durationMinutes);
        this.minimumHeightInches = minimumHeightInches;
        if (minimumHeightInches != null && minimumHeightInches <= 0) {
            throw new IllegalArgumentException("minimumHeightInches must be positive when present");
        }
        this.intensity = Objects.requireNonNull(intensity, "intensity is required");
        this.environment = Objects.requireNonNull(environment, "environment is required");
        this.singleRiderAvailable = singleRiderAvailable;
        this.accessibilitySummary = requireText(accessibilitySummary, "accessibilitySummary");
        this.media = Objects.requireNonNull(media, "media is required");
    }

    public static AttractionExperienceProfile create(
            AttractionId attractionId,
            String shortDescription,
            int durationMinutes,
            Integer minimumHeightInches,
            Intensity intensity,
            Environment environment,
            boolean singleRiderAvailable,
            String accessibilitySummary,
            ExperienceMedia media
    ) {
        return new AttractionExperienceProfile(
                attractionId,
                shortDescription,
                durationMinutes,
                minimumHeightInches,
                intensity,
                environment,
                singleRiderAvailable,
                accessibilitySummary,
                media
        );
    }

    public AttractionId attractionId() {
        return attractionId;
    }

    public String shortDescription() {
        return shortDescription;
    }

    public int durationMinutes() {
        return durationMinutes;
    }

    public Integer minimumHeightInches() {
        return minimumHeightInches;
    }

    public Intensity intensity() {
        return intensity;
    }

    public Environment environment() {
        return environment;
    }

    public boolean singleRiderAvailable() {
        return singleRiderAvailable;
    }

    public String accessibilitySummary() {
        return accessibilitySummary;
    }

    public ExperienceMedia media() {
        return media;
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static int requirePositiveDuration(int durationMinutes) {
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("durationMinutes must be positive");
        }
        return durationMinutes;
    }
}
