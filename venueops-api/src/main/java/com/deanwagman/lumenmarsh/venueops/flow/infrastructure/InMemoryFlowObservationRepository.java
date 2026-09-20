package com.deanwagman.lumenmarsh.venueops.flow.infrastructure;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.flow.application.FlowObservationRepository;
import com.deanwagman.lumenmarsh.venueops.flow.domain.QueueObservation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "venueops.attractions.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryFlowObservationRepository implements FlowObservationRepository {

    private final Map<UUID, QueueObservation> observations = new ConcurrentHashMap<>();

    @Override
    public Optional<QueueObservation> findById(UUID observationId) {
        return Optional.ofNullable(observations.get(observationId));
    }

    @Override
    public List<QueueObservation> findByAttraction(AttractionId attractionId, int limit) {
        return observations.values().stream()
                .filter(observation -> observation.attractionId().equals(attractionId))
                .sorted(Comparator.comparing(QueueObservation::observedAt)
                        .thenComparing(QueueObservation::receivedAt)
                        .reversed())
                .limit(Math.max(limit, 0))
                .toList();
    }

    @Override
    public void save(QueueObservation observation) {
        observations.put(observation.observationId(), observation);
    }
}
