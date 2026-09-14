package com.deanwagman.lumenmarsh.venueops.incident.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IncidentAttractionJpaRepository extends JpaRepository<IncidentAttractionEntity, IncidentAttractionId> {

    List<IncidentAttractionEntity> findByIncidentIdOrderByAttractionIdAsc(String incidentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from IncidentAttractionEntity l where l.incidentId = :incidentId")
    void deleteByIncidentId(@Param("incidentId") String incidentId);
}
