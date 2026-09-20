package com.deanwagman.lumenmarsh.venueops.flow.application;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.flow.domain.QueueObservation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlowObservationRepository {

    Optional<QueueObservation> findById(UUID observationId);

    List<QueueObservation> findByAttraction(AttractionId attractionId, int limit);

    void save(QueueObservation observation);
}
