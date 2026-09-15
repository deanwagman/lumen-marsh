package com.deanwagman.lumenmarsh.venueops.maintenance.application;

import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceRecommendation;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceRecommendationId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MaintenanceRecommendationRepository {

    Optional<MaintenanceRecommendation> findById(MaintenanceRecommendationId id);

    Optional<MaintenanceRecommendation> findByObservationId(String observationId);

    Optional<MaintenanceRecommendation> findByCommandId(UUID commandId);

    List<MaintenanceRecommendation> findAll();

    void save(MaintenanceRecommendation recommendation);
}
