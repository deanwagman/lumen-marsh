package com.deanwagman.lumenmarsh.venueops.attraction.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionExperienceRepository;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionExperienceProfile;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.ExperienceMedia;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "venueops.attractions.persistence", havingValue = "jpa")
public class JpaAttractionExperienceRepository implements AttractionExperienceRepository {

    private final AttractionExperienceProfileJpaRepository profiles;

    public JpaAttractionExperienceRepository(AttractionExperienceProfileJpaRepository profiles) {
        this.profiles = profiles;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AttractionExperienceProfile> findByAttractionId(AttractionId id) {
        return profiles.findById(id.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttractionExperienceProfile> findAll() {
        return profiles.findAll().stream()
                .sorted((left, right) -> left.getAttractionId().compareTo(right.getAttractionId()))
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void save(AttractionExperienceProfile profile) {
        profiles.save(toEntity(profile));
    }

    private AttractionExperienceProfile toDomain(AttractionExperienceProfileEntity entity) {
        return AttractionExperienceProfile.create(
                new AttractionId(entity.getAttractionId()),
                entity.getShortDescription(),
                entity.getDurationMinutes(),
                entity.getMinimumHeightInches(),
                entity.getIntensity(),
                entity.getEnvironment(),
                entity.isSingleRiderAvailable(),
                entity.getAccessibilitySummary(),
                new ExperienceMedia(
                        entity.getHeroUrl(),
                        entity.getThumbnailUrl(),
                        entity.getAltText()
                )
        );
    }

    private static AttractionExperienceProfileEntity toEntity(AttractionExperienceProfile profile) {
        ExperienceMedia media = profile.media();
        return new AttractionExperienceProfileEntity(
                profile.attractionId().value(),
                profile.shortDescription(),
                profile.durationMinutes(),
                profile.minimumHeightInches(),
                profile.intensity(),
                profile.environment(),
                profile.singleRiderAvailable(),
                profile.accessibilitySummary(),
                media.heroUrl(),
                media.thumbnailUrl(),
                media.altText()
        );
    }
}
