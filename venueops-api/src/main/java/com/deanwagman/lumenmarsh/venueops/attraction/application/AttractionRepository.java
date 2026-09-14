package com.deanwagman.lumenmarsh.venueops.attraction.application;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.Attraction;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;

import java.util.List;
import java.util.Optional;

public interface AttractionRepository {

    Optional<Attraction> findById(AttractionId id);

    List<Attraction> findAll();

    void save(Attraction attraction);
}
