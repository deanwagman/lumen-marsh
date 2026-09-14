package com.deanwagman.lumenmarsh.venueops.incident.application;

import com.deanwagman.lumenmarsh.venueops.incident.domain.Incident;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentId;

import java.util.List;
import java.util.Optional;

public interface IncidentRepository {

    Optional<Incident> findById(IncidentId id);

    List<Incident> findAll();

    void save(Incident incident);
}
