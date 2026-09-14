package com.deanwagman.lumenmarsh.venueops.incident.domain;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class Incident {

    private final IncidentId id;
    private final String title;
    private final IncidentType type;
    private final String internalDescription;
    private final Instant createdAt;
    private IncidentSeverity severity;
    private IncidentStatus status;
    private String assignedTo;
    private String guestTitle;
    private String guestMessage;
    private boolean guestAdvisoryPublished;
    private Instant updatedAt;
    private long version;
    private final LinkedHashSet<AttractionId> attractionIds;
    private final List<IncidentActivity> history;
    private int committedCount;

    private Incident(
            IncidentId id,
            String title,
            IncidentType type,
            IncidentSeverity severity,
            IncidentStatus status,
            String internalDescription,
            String assignedTo,
            String guestTitle,
            String guestMessage,
            boolean guestAdvisoryPublished,
            Instant createdAt,
            Instant updatedAt,
            long version,
            Set<AttractionId> attractionIds,
            List<IncidentActivity> history,
            int committedCount
    ) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.title = requireText(title, "title");
        this.type = Objects.requireNonNull(type, "type is required");
        this.severity = Objects.requireNonNull(severity, "severity is required");
        this.status = Objects.requireNonNull(status, "status is required");
        this.internalDescription = normalizeOptional(internalDescription);
        this.assignedTo = normalizeOptional(assignedTo);
        this.guestTitle = normalizeOptional(guestTitle);
        this.guestMessage = normalizeOptional(guestMessage);
        this.guestAdvisoryPublished = guestAdvisoryPublished;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
        this.version = version;
        this.attractionIds = new LinkedHashSet<>(attractionIds);
        this.history = new ArrayList<>(history);
        this.committedCount = committedCount;
        assertInvariants();
    }

    public static Incident report(
            String title,
            IncidentType type,
            IncidentSeverity severity,
            String internalDescription,
            List<AttractionId> attractionIds,
            String actor,
            Clock clock
    ) {
        return report(
                new IncidentId(UUID.randomUUID().toString()),
                title,
                type,
                severity,
                internalDescription,
                attractionIds,
                actor,
                clock
        );
    }

    public static Incident report(
            IncidentId id,
            String title,
            IncidentType type,
            IncidentSeverity severity,
            String internalDescription,
            List<AttractionId> attractionIds,
            String actor,
            Clock clock
    ) {
        requireActor(actor);
        requireClock(clock);
        Instant occurredAt = Instant.now(clock);
        LinkedHashSet<AttractionId> links = uniqueAttractionIds(attractionIds);
        IncidentReported reported = new IncidentReported(
                newActivityId(),
                id,
                IncidentEventType.INCIDENT_REPORTED,
                actor,
                null,
                occurredAt,
                0L,
                1L,
                requireText(title, "title"),
                type,
                severity,
                List.copyOf(links)
        );
        return new Incident(
                id,
                title,
                type,
                severity,
                IncidentStatus.REPORTED,
                internalDescription,
                null,
                null,
                null,
                false,
                occurredAt,
                occurredAt,
                1L,
                links,
                List.of(reported),
                0
        );
    }

    public static Incident rehydrate(
            IncidentId id,
            String title,
            IncidentType type,
            IncidentSeverity severity,
            IncidentStatus status,
            String internalDescription,
            String assignedTo,
            String guestTitle,
            String guestMessage,
            boolean guestAdvisoryPublished,
            Instant createdAt,
            Instant updatedAt,
            long version,
            List<AttractionId> attractionIds,
            List<IncidentActivity> activity
    ) {
        List<IncidentActivity> history = activity == null ? List.of() : List.copyOf(activity);
        return new Incident(
                id,
                title,
                type,
                severity,
                status,
                internalDescription,
                assignedTo,
                guestTitle,
                guestMessage,
                guestAdvisoryPublished,
                createdAt,
                updatedAt,
                version,
                uniqueAttractionIds(attractionIds),
                history,
                history.size()
        );
    }

    public void acknowledge(String actor, String reason, Clock clock) {
        applyStatusCommand(IncidentCommand.ACKNOWLEDGE, actor, reason, clock);
    }

    public void startMitigation(String actor, String reason, Clock clock) {
        applyStatusCommand(IncidentCommand.START_MITIGATION, actor, reason, clock);
    }

    public void resolve(String actor, String reason, Clock clock) {
        applyStatusCommand(IncidentCommand.RESOLVE, actor, reason, clock);
        guestAdvisoryPublished = false;
        assertInvariants();
    }

    public void assign(String assignee, String actor, String reason, Clock clock) {
        requireOpen(IncidentCommand.ASSIGN);
        requireActor(actor);
        requireClock(clock);
        requireReason(IncidentCommand.ASSIGN, reason);
        String nextAssignee = requireText(assignee, "assignee");
        Instant occurredAt = Instant.now(clock);
        long previousVersion = this.version;
        this.assignedTo = nextAssignee;
        bump(occurredAt);
        history.add(new IncidentAssigned(
                newActivityId(),
                id,
                IncidentCommand.ASSIGN.eventType(),
                actor,
                normalizeReason(reason),
                occurredAt,
                previousVersion,
                this.version,
                nextAssignee
        ));
    }

    public void changeSeverity(IncidentSeverity nextSeverity, String actor, String reason, Clock clock) {
        requireOpen(IncidentCommand.CHANGE_SEVERITY);
        requireActor(actor);
        requireClock(clock);
        requireReason(IncidentCommand.CHANGE_SEVERITY, reason);
        Objects.requireNonNull(nextSeverity, "severity is required");
        if (nextSeverity == this.severity) {
            throw new IllegalArgumentException("Severity is already " + nextSeverity);
        }
        IncidentSeverity previousSeverity = this.severity;
        Instant occurredAt = Instant.now(clock);
        long previousVersion = this.version;
        this.severity = nextSeverity;
        bump(occurredAt);
        history.add(new IncidentSeverityChanged(
                newActivityId(),
                id,
                IncidentCommand.CHANGE_SEVERITY.eventType(),
                actor,
                normalizeReason(reason),
                occurredAt,
                previousVersion,
                this.version,
                previousSeverity,
                nextSeverity
        ));
    }

    public void linkAttraction(AttractionId attractionId, String actor, String reason, Clock clock) {
        requireOpen(IncidentCommand.LINK_ATTRACTION);
        requireActor(actor);
        requireClock(clock);
        requireReason(IncidentCommand.LINK_ATTRACTION, reason);
        Objects.requireNonNull(attractionId, "attractionId is required");
        if (attractionIds.contains(attractionId)) {
            throw new IllegalArgumentException("Attraction already linked: " + attractionId);
        }
        Instant occurredAt = Instant.now(clock);
        long previousVersion = this.version;
        attractionIds.add(attractionId);
        bump(occurredAt);
        history.add(new IncidentAttractionLinked(
                newActivityId(),
                id,
                IncidentCommand.LINK_ATTRACTION.eventType(),
                actor,
                normalizeReason(reason),
                occurredAt,
                previousVersion,
                this.version,
                attractionId
        ));
    }

    public void unlinkAttraction(AttractionId attractionId, String actor, String reason, Clock clock) {
        requireOpen(IncidentCommand.UNLINK_ATTRACTION);
        requireActor(actor);
        requireClock(clock);
        requireReason(IncidentCommand.UNLINK_ATTRACTION, reason);
        Objects.requireNonNull(attractionId, "attractionId is required");
        if (!attractionIds.contains(attractionId)) {
            throw new IllegalArgumentException("Attraction is not linked: " + attractionId);
        }
        Instant occurredAt = Instant.now(clock);
        long previousVersion = this.version;
        attractionIds.remove(attractionId);
        bump(occurredAt);
        history.add(new IncidentAttractionUnlinked(
                newActivityId(),
                id,
                IncidentCommand.UNLINK_ATTRACTION.eventType(),
                actor,
                normalizeReason(reason),
                occurredAt,
                previousVersion,
                this.version,
                attractionId
        ));
    }

    public void publishGuestAdvisory(String guestTitle, String guestMessage, String actor, String reason, Clock clock) {
        requireOpen(IncidentCommand.PUBLISH_GUEST_ADVISORY);
        requireActor(actor);
        requireClock(clock);
        requireReason(IncidentCommand.PUBLISH_GUEST_ADVISORY, reason);
        String nextTitle = requireText(guestTitle, "guestTitle");
        String nextMessage = requireText(guestMessage, "guestMessage");
        Instant occurredAt = Instant.now(clock);
        long previousVersion = this.version;
        this.guestTitle = nextTitle;
        this.guestMessage = nextMessage;
        this.guestAdvisoryPublished = true;
        bump(occurredAt);
        history.add(new IncidentGuestAdvisoryPublished(
                newActivityId(),
                id,
                IncidentCommand.PUBLISH_GUEST_ADVISORY.eventType(),
                actor,
                normalizeReason(reason),
                occurredAt,
                previousVersion,
                this.version,
                nextTitle,
                nextMessage
        ));
        assertInvariants();
    }

    public void withdrawGuestAdvisory(String actor, String reason, Clock clock) {
        requireOpen(IncidentCommand.WITHDRAW_GUEST_ADVISORY);
        requireActor(actor);
        requireClock(clock);
        requireReason(IncidentCommand.WITHDRAW_GUEST_ADVISORY, reason);
        if (!guestAdvisoryPublished) {
            throw new IllegalArgumentException("No guest advisory is currently published");
        }
        Instant occurredAt = Instant.now(clock);
        long previousVersion = this.version;
        this.guestAdvisoryPublished = false;
        bump(occurredAt);
        history.add(new IncidentGuestAdvisoryWithdrawn(
                newActivityId(),
                id,
                IncidentCommand.WITHDRAW_GUEST_ADVISORY.eventType(),
                actor,
                normalizeReason(reason),
                occurredAt,
                previousVersion,
                this.version
        ));
        assertInvariants();
    }

    public IncidentId id() {
        return id;
    }

    public String title() {
        return title;
    }

    public IncidentType type() {
        return type;
    }

    public IncidentSeverity severity() {
        return severity;
    }

    public IncidentStatus status() {
        return status;
    }

    public String internalDescription() {
        return internalDescription;
    }

    public String assignedTo() {
        return assignedTo;
    }

    public String guestTitle() {
        return guestTitle;
    }

    public String guestMessage() {
        return guestMessage;
    }

    public boolean guestAdvisoryPublished() {
        return guestAdvisoryPublished;
    }

    public boolean hasActiveGuestAdvisory() {
        return !status.isResolved()
                && guestAdvisoryPublished
                && guestTitle != null
                && guestMessage != null;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public long version() {
        return version;
    }

    public List<AttractionId> attractionIds() {
        return List.copyOf(attractionIds);
    }

    public List<IncidentActivity> activity() {
        return List.copyOf(history);
    }

    public List<IncidentActivity> uncommittedActivity() {
        return List.copyOf(history.subList(committedCount, history.size()));
    }

    public void markActivityCommitted() {
        committedCount = history.size();
    }

    private void applyStatusCommand(IncidentCommand command, String actor, String reason, Clock clock) {
        requireActor(actor);
        requireClock(clock);
        requireReason(command, reason);
        IncidentStatus previousStatus = this.status;
        IncidentStatus nextStatus = IncidentStateMachine.nextStatus(previousStatus, command);
        Instant occurredAt = Instant.now(clock);
        long previousVersion = this.version;
        this.status = nextStatus;
        bump(occurredAt);
        history.add(new IncidentStatusChanged(
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

    private void requireOpen(IncidentCommand command) {
        if (status.isResolved()) {
            throw new InvalidIncidentTransitionException(status, command);
        }
    }

    private void bump(Instant occurredAt) {
        this.version = this.version + 1;
        this.updatedAt = occurredAt;
    }

    private void assertInvariants() {
        if (guestAdvisoryPublished) {
            if (guestTitle == null || guestMessage == null) {
                throw new IllegalStateException("Published guest advisories require a public title and message");
            }
        }
        if (version < 1) {
            throw new IllegalStateException("Incidents start at version 1");
        }
        if (history.isEmpty()) {
            throw new IllegalStateException("Incidents must have an audited report event");
        }
    }

    private static LinkedHashSet<AttractionId> uniqueAttractionIds(List<AttractionId> attractionIds) {
        LinkedHashSet<AttractionId> unique = new LinkedHashSet<>();
        if (attractionIds == null) {
            return unique;
        }
        for (AttractionId attractionId : attractionIds) {
            Objects.requireNonNull(attractionId, "attractionId is required");
            unique.add(attractionId);
        }
        return unique;
    }

    private static void requireActor(String actor) {
        requireText(actor, "actor");
    }

    private static void requireClock(Clock clock) {
        Objects.requireNonNull(clock, "clock is required");
    }

    private static void requireReason(IncidentCommand command, String reason) {
        if (command.requiresReason() && isBlank(reason)) {
            throw new IllegalArgumentException(command + " requires a meaningful reason");
        }
    }

    private static String normalizeReason(String reason) {
        return isBlank(reason) ? null : reason.trim();
    }

    private static String normalizeOptional(String value) {
        return isBlank(value) ? null : value.trim();
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
