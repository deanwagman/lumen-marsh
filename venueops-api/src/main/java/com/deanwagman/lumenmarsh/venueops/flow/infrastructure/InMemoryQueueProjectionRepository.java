package com.deanwagman.lumenmarsh.venueops.flow.infrastructure;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.flow.application.QueueProjectionRepository;
import com.deanwagman.lumenmarsh.venueops.flow.domain.QueueProjection;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "venueops.attractions.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryQueueProjectionRepository implements QueueProjectionRepository {

    private final Map<String, QueueProjection> projections = new ConcurrentHashMap<>();

    @Override
    public Optional<QueueProjection> findByAttractionId(AttractionId attractionId) {
        return Optional.ofNullable(projections.get(attractionId.value()));
    }

    @Override
    public List<QueueProjection> findAll() {
        return projections.values().stream()
                .sorted(Comparator.comparing(projection -> projection.attractionId().value()))
                .toList();
    }

    @Override
    public synchronized void save(QueueProjection projection) {
        projections.put(projection.attractionId().value(), projection);
    }
}
