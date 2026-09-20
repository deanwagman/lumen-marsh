package com.deanwagman.lumenmarsh.venueops.flow.application;

import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendation;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationId;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationStatus;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationType;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationSeverity;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;

import java.util.List;
import java.util.Optional;

public interface FlowRecommendationRepository {

    Optional<FlowRecommendation> findById(FlowRecommendationId id);

    List<FlowRecommendation> findAll();

    List<FlowRecommendation> find(
            FlowRecommendationStatus status,
            FlowRecommendationSeverity severity,
            FlowRecommendationType type,
            AttractionId attractionId
    );

    void save(FlowRecommendation recommendation);
}
