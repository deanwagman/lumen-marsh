package com.deanwagman.lumenmarsh.venueops.attraction.infrastructure;

import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionRepository;
import com.deanwagman.lumenmarsh.venueops.attraction.application.StaleAttractionVersionException;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.Attraction;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "venueops.attractions.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryAttractionRepository implements AttractionRepository {

    private final Map<AttractionId, Attraction> attractions = new ConcurrentHashMap<>();

    @Override
    public Optional<Attraction> findById(AttractionId id) {
        return Optional.ofNullable(attractions.get(id)).map(InMemoryAttractionRepository::copyOf);
    }

    @Override
    public List<Attraction> findAll() {
        return attractions.values().stream()
                .map(InMemoryAttractionRepository::copyOf)
                .sorted(Comparator.comparing(attraction -> attraction.id().value()))
                .toList();
    }

    @Override
    public synchronized void save(Attraction attraction) {
        Attraction existing = attractions.get(attraction.id());
        if (existing != null) {
            long expectedVersion = attraction.version() - attraction.uncommittedActivity().size();
            if (existing.version() != expectedVersion) {
                throw new StaleAttractionVersionException(
                        attraction.id(),
                        expectedVersion,
                        existing.version()
                );
            }
        }
        attractions.put(attraction.id(), copyOf(attraction));
        attraction.markActivityCommitted();
    }

    private static Attraction copyOf(Attraction attraction) {
        return Attraction.rehydrate(
                attraction.id(),
                attraction.name(),
                attraction.area(),
                attraction.type(),
                attraction.status(),
                attraction.capacityMode(),
                attraction.waitMinutes(),
                attraction.updatedAt(),
                attraction.version(),
                attraction.activity()
        );
    }
}
