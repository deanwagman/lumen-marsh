package com.deanwagman.lumenmarsh.venueops.incident.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentRepository;
import com.deanwagman.lumenmarsh.venueops.incident.application.StaleIncidentVersionException;
import com.deanwagman.lumenmarsh.venueops.incident.domain.Incident;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentActivity;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "venueops.attractions.persistence", havingValue = "jpa")
public class JpaIncidentRepository implements IncidentRepository {

    private final IncidentJpaRepository incidents;
    private final IncidentAttractionJpaRepository links;
    private final IncidentActivityJpaRepository activities;

    public JpaIncidentRepository(
            IncidentJpaRepository incidents,
            IncidentAttractionJpaRepository links,
            IncidentActivityJpaRepository activities
    ) {
        this.incidents = incidents;
        this.links = links;
        this.activities = activities;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Incident> findById(IncidentId id) {
        return incidents.findById(id.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Incident> findAll() {
        return incidents.findAll().stream()
                .sorted(Comparator.comparing(IncidentEntity::getUpdatedAt).reversed())
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void save(Incident incident) {
        String id = incident.id().value();
        Optional<IncidentEntity> existing = incidents.findById(id);
        if (existing.isEmpty()) {
            incidents.save(toEntity(incident));
            replaceLinks(incident);
            persistUncommitted(incident);
            return;
        }

        long expectedVersion = incident.version() - incident.uncommittedActivity().size();
        int updated = incidents.updateOperationalState(
                id,
                incident.severity(),
                incident.status(),
                incident.assignedTo(),
                incident.guestTitle(),
                incident.guestMessage(),
                incident.guestAdvisoryPublished(),
                incident.updatedAt(),
                incident.version(),
                expectedVersion
        );
        if (updated == 0) {
            throw new StaleIncidentVersionException(incident.id(), expectedVersion, existing.get().getVersion());
        }
        replaceLinks(incident);
        persistUncommitted(incident);
    }

    private void replaceLinks(Incident incident) {
        String id = incident.id().value();
        links.deleteByIncidentId(id);
        for (AttractionId attractionId : incident.attractionIds()) {
            links.save(new IncidentAttractionEntity(id, attractionId.value()));
        }
    }

    private void persistUncommitted(Incident incident) {
        for (IncidentActivity activity : incident.uncommittedActivity()) {
            activities.save(IncidentActivityMapper.toEntity(activity));
        }
        incident.markActivityCommitted();
    }

    private Incident toDomain(IncidentEntity entity) {
        List<AttractionId> attractionIds = links.findByIncidentIdOrderByAttractionIdAsc(entity.getId()).stream()
                .map(link -> new AttractionId(link.getAttractionId()))
                .toList();
        List<IncidentActivity> history = activities
                .findByIncidentIdOrderByResultingVersionAsc(entity.getId())
                .stream()
                .map(IncidentActivityMapper::toDomain)
                .toList();
        return Incident.rehydrate(
                new IncidentId(entity.getId()),
                entity.getTitle(),
                entity.getType(),
                entity.getSeverity(),
                entity.getStatus(),
                entity.getInternalDescription(),
                entity.getAssignedTo(),
                entity.getGuestTitle(),
                entity.getGuestMessage(),
                entity.isGuestAdvisoryPublished(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion(),
                attractionIds,
                history
        );
    }

    private static IncidentEntity toEntity(Incident incident) {
        return new IncidentEntity(
                incident.id().value(),
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
                incident.version()
        );
    }
}
