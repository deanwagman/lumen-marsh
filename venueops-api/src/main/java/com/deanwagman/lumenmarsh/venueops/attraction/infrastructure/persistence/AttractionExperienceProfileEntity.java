package com.deanwagman.lumenmarsh.venueops.attraction.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.Environment;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.Intensity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "attraction_experience")
public class AttractionExperienceProfileEntity {

    @Id
    @Column(name = "attraction_id", length = 64)
    private String attractionId;

    @Column(name = "short_description", nullable = false)
    private String shortDescription;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "minimum_height_inches")
    private Integer minimumHeightInches;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private Intensity intensity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private Environment environment;

    @Column(name = "single_rider_available", nullable = false)
    private boolean singleRiderAvailable;

    @Column(name = "accessibility_summary", nullable = false)
    private String accessibilitySummary;

    @Column(name = "hero_url", nullable = false, length = 512)
    private String heroUrl;

    @Column(name = "thumbnail_url", nullable = false, length = 512)
    private String thumbnailUrl;

    @Column(name = "alt_text", nullable = false)
    private String altText;

    protected AttractionExperienceProfileEntity() {
    }

    public AttractionExperienceProfileEntity(
            String attractionId,
            String shortDescription,
            int durationMinutes,
            Integer minimumHeightInches,
            Intensity intensity,
            Environment environment,
            boolean singleRiderAvailable,
            String accessibilitySummary,
            String heroUrl,
            String thumbnailUrl,
            String altText
    ) {
        this.attractionId = attractionId;
        this.shortDescription = shortDescription;
        this.durationMinutes = durationMinutes;
        this.minimumHeightInches = minimumHeightInches;
        this.intensity = intensity;
        this.environment = environment;
        this.singleRiderAvailable = singleRiderAvailable;
        this.accessibilitySummary = accessibilitySummary;
        this.heroUrl = heroUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.altText = altText;
    }

    public String getAttractionId() {
        return attractionId;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public Integer getMinimumHeightInches() {
        return minimumHeightInches;
    }

    public Intensity getIntensity() {
        return intensity;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public boolean isSingleRiderAvailable() {
        return singleRiderAvailable;
    }

    public String getAccessibilitySummary() {
        return accessibilitySummary;
    }

    public String getHeroUrl() {
        return heroUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getAltText() {
        return altText;
    }
}
