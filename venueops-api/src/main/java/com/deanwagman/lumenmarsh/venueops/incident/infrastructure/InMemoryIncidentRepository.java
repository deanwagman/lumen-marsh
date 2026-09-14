package com.deanwagman.lumenmarsh.venueops.incident.infrastructure;

import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentRepository;
import com.deanwagman.lumenmarsh.venueops.incident.application.StaleIncidentVersionException;
import com.deanwagman.lumenmarsh.venueops.incident.domain.Incident;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "venueops.attractions.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryIncidentRepository implements IncidentRepository {

    private final Map<IncidentId, Incident> incidents = new ConcurrentHashMap<>();

    @Override
    public Optional<Incident> findById(IncidentId id) {
        return Optional.ofNullable(incidents.get(id)).map(InMemoryIncidentRepository::copyOf);
    }

    @Override
    public List<Incident> findAll() {
        return incidents.values().stream()
                .map(InMemoryIncidentRepository::copyOf)
                .sorted(Comparator.comparing(Incident::updatedAt).reversed())
                .toList();
    }

    @Override
    public synchronized void save(Incident incident) {
        Incident existing = incidents.get(incident.id());
        if (existing != null) {
            long expectedVersion = incident.version() - incident.uncommittedActivity().size();
            if (existing.version() != expectedVersion) {
                throw new StaleIncidentVersionException(
                        incident.id(),
                        expectedVersion,
                        existing.version()
                );
            }
        }
        incidents.put(incident.id(), copyOf(incident));
        incident.markActivityCommitted();
    }

    private static Incident copyOf(Incident incident) {
        return Incident.rehydrate(
                incident.id(),
                incident.title(),
                incident.type(),
                incident.severity(),
                incident.status(),
                incident.internalDescription(),
                incident.assignedTo(),
                incident.guestTitle(),
                incident.guestMessage(),
                incident.guestAdvisoryPublished(),
                incident.createdAt(),
                incident.updatedAt(),
                incident.version(),
                incident.attractionIds(),
                incident.activity()
        );
    }
}
