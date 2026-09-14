package com.deanwagman.lumenmarsh.venueops.attraction.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AttractionExperienceProfileJpaRepository
        extends JpaRepository<AttractionExperienceProfileEntity, String> {
}
