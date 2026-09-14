package com.deanwagman.lumenmarsh.venueops.attraction.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttractionActivityJpaRepository extends JpaRepository<AttractionActivityEntity, String> {

    List<AttractionActivityEntity> findByAttractionIdOrderByResultingVersionAsc(String attractionId);
}
