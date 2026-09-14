package com.deanwagman.lumenmarsh.venueops.attraction.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Attraction {

    public static final int MAX_WAIT_MINUTES = 300;

    private final AttractionId id;
    private final String name;
    private final String area;
    private final AttractionType type;
    private AttractionStatus status;
    private CapacityMode capacityMode;
    private Integer waitMinutes;
    private Instant updatedAt;
    private long version;
    private final List<AttractionActivity> history;
    private int committedCount;

    private Attraction(
            AttractionId id,
            String name,
            String area,
            AttractionType type,
            AttractionStatus status,
            CapacityMode capacityMode,
            Integer waitMinutes,
            Instant updatedAt,
            long version,
            List<AttractionActivity> history,
            int committedCount
    ) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.name = requireText(name, "name");
        this.area = requireText(area, "area");
        this.type = Objects.requireNonNull(type, "type is required");
        this.status = Objects.requireNonNull(status, "status is required");
        this.capacityMode = Objects.requireNonNull(capacityMode, "capacityMode is required");
        this.waitMinutes = waitMinutes;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
        this.version = version;
        this.history = new ArrayList<>(history);
        this.committedCount = committedCount;
        assertInvariants();
    }

    public static Attraction create(AttractionId id, String name, String area, AttractionType type) {
        return create(id, name, area, type, Clock.systemUTC());
    }

    public static Attraction create(AttractionId id, String name, String area, AttractionType type, Clock clock) {
        return new Attraction(
                id,
                name,
                area,
                type,
                AttractionStatus.CLOSED,
                CapacityMode.NOT_APPLICABLE,
                null,
                Instant.now(clock),
                0L,
                List.of(),
                0
        );
    }

    public static Attraction rehydrate(
            AttractionId id,
            String name,
            String area,
            AttractionType type,
            AttractionStatus status,
            CapacityMode capacityMode,
            Integer waitMinutes,
            Instant updatedAt,
            long version,
            List<AttractionActivity> activity
    ) {
        List<AttractionActivity> history = activity == null ? List.of() : List.copyOf(activity);
        return new Attraction(
                id,
                name,
                area,
                type,
                status,
                capacityMode,
                waitMinutes,
                updatedAt,
                version,
                history,
                history.size()
        );
    }

    public void startTesting(String actor, String reason, Clock clock) {
        applyStatusCommand(AttractionCommand.START_TESTING, actor, reason, clock);
    }

    public void completeTesting(String actor, String reason, Clock clock) {
        applyStatusCommand(AttractionCommand.COMPLETE_TESTING, actor, reason, clock);
    }

    public void approveReturnToService(String actor, String reason, Clock clock) {
        applyStatusCommand(AttractionCommand.APPROVE_RETURN_TO_SERVICE, actor, reason, clock);
    }

    public void placeWeatherHold(String actor, String reason, Clock clock) {
        applyStatusCommand(AttractionCommand.PLACE_WEATHER_HOLD, actor, reason, clock);
    }

    public void clearWeatherHold(String actor, String reason, Clock clock) {
        applyStatusCommand(AttractionCommand.CLEAR_WEATHER_HOLD, actor, reason, clock);
    }

    public void reportTechnicalFault(String actor, String reason, Clock clock) {
        applyStatusCommand(AttractionCommand.REPORT_TECHNICAL_FAULT, actor, reason, clock);
    }

    public void completeRepair(String actor, String reason, Clock clock) {
        applyStatusCommand(AttractionCommand.COMPLETE_REPAIR, actor, reason, clock);
    }

    public void closeForDay(String actor, String reason, Clock clock) {
        applyStatusCommand(AttractionCommand.CLOSE_FOR_DAY, actor, reason, clock);
    }

    public void updateWaitTime(int minutes, String actor, String reason, Clock clock) {
        requireActor(actor);
        requireClock(clock);
        requireReason(AttractionCommand.UPDATE_WAIT_TIME, reason);
        if (!status.isOperating()) {
            throw new InvalidAttractionTransitionException(status, AttractionCommand.UPDATE_WAIT_TIME);
        }
        if (minutes < 0 || minutes > MAX_WAIT_MINUTES) {
            throw new IllegalArgumentException(
                    "Wait time must be between 0 and " + MAX_WAIT_MINUTES + " minutes"
            );
        }

        Integer previousMinutes = this.waitMinutes;
        Instant occurredAt = Instant.now(clock);
        long previousVersion = this.version;
        this.waitMinutes = minutes;
        this.version = previousVersion + 1;
        this.updatedAt = occurredAt;
        history.add(new AttractionWaitTimeChanged(
                newActivityId(),
                id,
                AttractionCommand.UPDATE_WAIT_TIME.eventType(),
                actor,
                normalizeReason(reason),
                occurredAt,
                previousVersion,
                this.version,
                previousMinutes,
                minutes
        ));
    }

    public void reduceCapacity(String actor, String reason, Clock clock) {
        requireActor(actor);
        requireClock(clock);
        requireReason(AttractionCommand.REDUCE_CAPACITY, reason);
        if (!status.isOperating() || capacityMode != CapacityMode.NORMAL) {
            throw new InvalidAttractionTransitionException(status, AttractionCommand.REDUCE_CAPACITY);
        }
        applyCapacityChange(CapacityMode.REDUCED, AttractionCommand.REDUCE_CAPACITY, actor, reason, clock);
    }

    public void restoreCapacity(String actor, String reason, Clock clock) {
        requireActor(actor);
        requireClock(clock);
        requireReason(AttractionCommand.RESTORE_CAPACITY, reason);
        if (!status.isOperating() || capacityMode != CapacityMode.REDUCED) {
            throw new InvalidAttractionTransitionException(status, AttractionCommand.RESTORE_CAPACITY);
        }
        applyCapacityChange(CapacityMode.NORMAL, AttractionCommand.RESTORE_CAPACITY, actor, reason, clock);
    }

    public AttractionId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String area() {
        return area;
    }

    public AttractionType type() {
        return type;
    }

    public AttractionStatus status() {
        return status;
    }

    public CapacityMode capacityMode() {
        return capacityMode;
    }

    public Integer waitMinutes() {
        return waitMinutes;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public long version() {
        return version;
    }

    public String guestStatusMessage() {
        return status.guestMessage();
    }

    public List<AttractionActivity> activity() {
        return List.copyOf(history);
    }

    public List<AttractionActivity> uncommittedActivity() {
        return List.copyOf(history.subList(committedCount, history.size()));
    }

    public void markActivityCommitted() {
        committedCount = history.size();
    }

    private void applyStatusCommand(AttractionCommand command, String actor, String reason, Clock clock) {
        requireActor(actor);
        requireClock(clock);
        requireReason(command, reason);

        AttractionStatus previousStatus = this.status;
        AttractionStatus nextStatus = AttractionStateMachine.nextStatus(previousStatus, command);
        Instant occurredAt = Instant.now(clock);
        long previousVersion = this.version;

        this.status = nextStatus;
        if (nextStatus.isOperating()) {
            this.capacityMode = CapacityMode.NORMAL;
            this.waitMinutes = 0;
        } else {
            this.capacityMode = CapacityMode.NOT_APPLICABLE;
            this.waitMinutes = null;
        }
        this.version = previousVersion + 1;
        this.updatedAt = occurredAt;
        history.add(new AttractionStatusChanged(
                newActivityId(),
                id,
                command.eventType(),
                actor,
                normalizeReason(reason),
                occurredAt,
                previousVersion,
                this.version,
                previousStatus,
                nextStatus
        ));
        assertInvariants();
    }

    private void applyCapacityChange(
            CapacityMode nextMode,
            AttractionCommand command,
            String actor,
            String reason,
            Clock clock
    ) {
        CapacityMode previousMode = this.capacityMode;
        Instant occurredAt = Instant.now(clock);
        long previousVersion = this.version;
        this.capacityMode = nextMode;
        this.version = previousVersion + 1;
        this.updatedAt = occurredAt;
        history.add(new AttractionCapacityChanged(
                newActivityId(),
                id,
                command.eventType(),
                actor,
                normalizeReason(reason),
                occurredAt,
                previousVersion,
                this.version,
                previousMode,
                nextMode
        ));
        assertInvariants();
    }

    private void assertInvariants() {
        if (status.isOperating()) {
            if (waitMinutes == null) {
                throw new IllegalStateException("Operating attractions must have a wait time");
            }
            if (waitMinutes < 0 || waitMinutes > MAX_WAIT_MINUTES) {
                throw new IllegalStateException("Wait time must be between 0 and " + MAX_WAIT_MINUTES);
            }
            if (capacityMode == CapacityMode.NOT_APPLICABLE) {
                throw new IllegalStateException("Operating attractions cannot use NOT_APPLICABLE capacity");
            }
        } else {
            if (waitMinutes != null) {
                throw new IllegalStateException("Non-operating attractions cannot publish a wait time");
            }
            if (capacityMode != CapacityMode.NOT_APPLICABLE) {
                throw new IllegalStateException("Non-operating attractions must use NOT_APPLICABLE capacity");
            }
        }
    }

    private static void requireActor(String actor) {
        requireText(actor, "actor");
    }

    private static void requireClock(Clock clock) {
        Objects.requireNonNull(clock, "clock is required");
    }

    private static void requireReason(AttractionCommand command, String reason) {
        if (command.requiresReason() && isBlank(reason)) {
            throw new IllegalArgumentException(command + " requires a meaningful reason");
        }
    }

    private static String normalizeReason(String reason) {
        return isBlank(reason) ? null : reason.trim();
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String newActivityId() {
        return UUID.randomUUID().toString();
    }
}
