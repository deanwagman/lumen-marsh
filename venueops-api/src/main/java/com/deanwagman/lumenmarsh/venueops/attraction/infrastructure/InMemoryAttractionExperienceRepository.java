package com.deanwagman.lumenmarsh.venueops.attraction.infrastructure;

import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionExperienceRepository;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionExperienceProfile;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.ExperienceMedia;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "venueops.attractions.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryAttractionExperienceRepository implements AttractionExperienceRepository {

    private final Map<AttractionId, AttractionExperienceProfile> profiles = new ConcurrentHashMap<>();

    @Override
    public Optional<AttractionExperienceProfile> findByAttractionId(AttractionId id) {
        return Optional.ofNullable(profiles.get(id)).map(InMemoryAttractionExperienceRepository::copyOf);
    }

    @Override
    public List<AttractionExperienceProfile> findAll() {
        return profiles.values().stream()
                .map(InMemoryAttractionExperienceRepository::copyOf)
                .sorted(Comparator.comparing(profile -> profile.attractionId().value()))
                .toList();
    }

    @Override
    public void save(AttractionExperienceProfile profile) {
        profiles.put(profile.attractionId(), copyOf(profile));
    }

    private static AttractionExperienceProfile copyOf(AttractionExperienceProfile profile) {
        ExperienceMedia media = profile.media();
        return AttractionExperienceProfile.create(
                profile.attractionId(),
                profile.shortDescription(),
                profile.durationMinutes(),
                profile.minimumHeightInches(),
                profile.intensity(),
                profile.environment(),
                profile.singleRiderAvailable(),
                profile.accessibilitySummary(),
                new ExperienceMedia(media.heroUrl(), media.thumbnailUrl(), media.altText())
        );
    }
}
