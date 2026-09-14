package com.deanwagman.lumenmarsh.venueops.incident.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentActivityJpaRepository extends JpaRepository<IncidentActivityEntity, String> {

    List<IncidentActivityEntity> findByIncidentIdOrderByResultingVersionAsc(String incidentId);
}
