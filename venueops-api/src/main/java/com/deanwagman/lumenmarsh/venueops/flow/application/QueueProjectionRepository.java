package com.deanwagman.lumenmarsh.venueops.flow.application;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.flow.domain.QueueProjection;

import java.util.List;
import java.util.Optional;

public interface QueueProjectionRepository {

    Optional<QueueProjection> findByAttractionId(AttractionId attractionId);

    List<QueueProjection> findAll();

    void save(QueueProjection projection);
}
