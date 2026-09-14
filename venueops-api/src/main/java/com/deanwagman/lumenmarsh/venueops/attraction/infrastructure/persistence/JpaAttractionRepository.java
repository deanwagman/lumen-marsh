package com.deanwagman.lumenmarsh.venueops.attraction.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionRepository;
import com.deanwagman.lumenmarsh.venueops.attraction.application.StaleAttractionVersionException;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.Attraction;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionActivity;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "venueops.attractions.persistence", havingValue = "jpa")
public class JpaAttractionRepository implements AttractionRepository {

    private final AttractionJpaRepository attractions;
    private final AttractionActivityJpaRepository activities;

    public JpaAttractionRepository(
            AttractionJpaRepository attractions,
            AttractionActivityJpaRepository activities
    ) {
        this.attractions = attractions;
        this.activities = activities;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Attraction> findById(AttractionId id) {
        return attractions.findById(id.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attraction> findAll() {
        return attractions.findAll().stream()
                .sorted((left, right) -> left.getId().compareTo(right.getId()))
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void save(Attraction attraction) {
        String id = attraction.id().value();
        Optional<AttractionEntity> existing = attractions.findById(id);
        if (existing.isEmpty()) {
            attractions.save(toEntity(attraction));
            persistUncommitted(attraction);
            return;
        }

        long expectedVersion = attraction.version() - attraction.uncommittedActivity().size();
        int updated = attractions.updateOperationalState(
                id,
                attraction.status(),
                attraction.capacityMode(),
                attraction.waitMinutes(),
                attraction.updatedAt(),
                attraction.version(),
                expectedVersion
        );
        if (updated == 0) {
            throw new StaleAttractionVersionException(attraction.id(), expectedVersion, existing.get().getVersion());
        }
        persistUncommitted(attraction);
    }

    private void persistUncommitted(Attraction attraction) {
        for (AttractionActivity activity : attraction.uncommittedActivity()) {
            activities.save(AttractionActivityMapper.toEntity(activity));
        }
        attraction.markActivityCommitted();
    }

    private Attraction toDomain(AttractionEntity entity) {
        List<AttractionActivity> history = activities
                .findByAttractionIdOrderByResultingVersionAsc(entity.getId())
                .stream()
                .map(AttractionActivityMapper::toDomain)
                .toList();
        return Attraction.rehydrate(
                new AttractionId(entity.getId()),
                entity.getName(),
                entity.getArea(),
                entity.getType(),
                entity.getStatus(),
                entity.getCapacityMode(),
                entity.getWaitMinutes(),
                entity.getUpdatedAt(),
                entity.getVersion(),
                history
        );
    }

    private static AttractionEntity toEntity(Attraction attraction) {
        return new AttractionEntity(
                attraction.id().value(),
                attraction.name(),
                attraction.area(),
                attraction.type(),
                attraction.status(),
                attraction.capacityMode(),
                attraction.waitMinutes(),
                attraction.updatedAt(),
                attraction.version()
        );
    }
}
