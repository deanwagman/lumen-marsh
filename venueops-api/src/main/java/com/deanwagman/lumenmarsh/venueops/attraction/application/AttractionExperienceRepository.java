package com.deanwagman.lumenmarsh.venueops.attraction.application;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionExperienceProfile;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;

import java.util.List;
import java.util.Optional;

public interface AttractionExperienceRepository {

    Optional<AttractionExperienceProfile> findByAttractionId(AttractionId id);

    List<AttractionExperienceProfile> findAll();

    void save(AttractionExperienceProfile profile);
}
