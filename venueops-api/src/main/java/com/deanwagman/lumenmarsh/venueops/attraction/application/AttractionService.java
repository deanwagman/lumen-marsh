package com.deanwagman.lumenmarsh.venueops.attraction.application;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.Attraction;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionActivity;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionCommand;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

public class AttractionService {

    private final AttractionRepository repository;
    private final Clock clock;
    private final AttractionUpdatePublisher updatePublisher;

    public AttractionService(
            AttractionRepository repository,
            Clock clock,
            AttractionUpdatePublisher updatePublisher
    ) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
        this.updatePublisher = Objects.requireNonNull(updatePublisher);
    }

    public List<Attraction> list() {
        return repository.findAll();
    }

    public Attraction get(AttractionId id) {
        return repository.findById(id).orElseThrow(() -> new AttractionNotFoundException(id));
    }

    public List<AttractionActivity> activity(AttractionId id) {
        return get(id).activity();
    }

    public Attraction execute(
            AttractionId id,
            AttractionCommand command,
            String actor,
            String reason,
            Integer waitMinutes,
            long expectedVersion
    ) {
        Attraction attraction = get(id);
        if (attraction.version() != expectedVersion) {
            throw new StaleAttractionVersionException(id, expectedVersion, attraction.version());
        }
        apply(attraction, command, actor, reason, waitMinutes);
        AttractionActivity activity = lastUncommitted(attraction);
        repository.save(attraction);
        updatePublisher.publish(AttractionOperationalUpdate.from(activity, attraction));
        return attraction;
    }

    private static AttractionActivity lastUncommitted(Attraction attraction) {
        List<AttractionActivity> uncommitted = attraction.uncommittedActivity();
        if (uncommitted.isEmpty()) {
            throw new IllegalStateException("Accepted command produced no activity");
        }
        return uncommitted.getLast();
    }

    private void apply(
            Attraction attraction,
            AttractionCommand command,
            String actor,
            String reason,
            Integer waitMinutes
    ) {
        switch (command) {
            case START_TESTING -> attraction.startTesting(actor, reason, clock);
            case COMPLETE_TESTING -> attraction.completeTesting(actor, reason, clock);
            case APPROVE_RETURN_TO_SERVICE -> attraction.approveReturnToService(actor, reason, clock);
            case PLACE_WEATHER_HOLD -> attraction.placeWeatherHold(actor, reason, clock);
            case CLEAR_WEATHER_HOLD -> attraction.clearWeatherHold(actor, reason, clock);
            case REPORT_TECHNICAL_FAULT -> attraction.reportTechnicalFault(actor, reason, clock);
            case COMPLETE_REPAIR -> attraction.completeRepair(actor, reason, clock);
            case CLOSE_FOR_DAY -> attraction.closeForDay(actor, reason, clock);
            case REDUCE_CAPACITY -> attraction.reduceCapacity(actor, reason, clock);
            case RESTORE_CAPACITY -> attraction.restoreCapacity(actor, reason, clock);
            case UPDATE_WAIT_TIME -> {
                if (waitMinutes == null) {
                    throw new IllegalArgumentException("UPDATE_WAIT_TIME requires waitMinutes");
                }
                attraction.updateWaitTime(waitMinutes, actor, reason, clock);
            }
        }
    }
}
