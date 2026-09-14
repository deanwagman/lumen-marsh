package com.deanwagman.lumenmarsh.venueops.weather.domain;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentId;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class WeatherRecommendation {

    public static final String SYSTEM_ACTOR = "environmental-monitor";

    private final WeatherRecommendationId id;
    private String ruleId;
    private WeatherRecommendationStatus status;
    private WeatherRecommendationOperatorStatus operatorStatus;
    private WeatherRecommendationSeverity severity;
    private String summary;
    private String evidence;
    private String recommendedAction;
    private final LinkedHashSet<AttractionId> affectedAttractionIds;
    private Instant observedAt;
    private final Instant receivedAt;
    private Instant updatedAt;
    private long sourceVersion;
    private long version;
    private IncidentId linkedIncidentId;
    private final List<WeatherRecommendationActivity> history;
    private int committedCount;

    private WeatherRecommendation(
            WeatherRecommendationId id,
            String ruleId,
            WeatherRecommendationStatus status,
            WeatherRecommendationOperatorStatus operatorStatus,
            WeatherRecommendationSeverity severity,
            String summary,
            String evidence,
            String recommendedAction,
            Set<AttractionId> affectedAttractionIds,
            Instant observedAt,
            Instant receivedAt,
            Instant updatedAt,
            long sourceVersion,
            long version,
            IncidentId linkedIncidentId,
            List<WeatherRecommendationActivity> history,
            int committedCount
    ) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.ruleId = requireText(ruleId, "ruleId");
        this.status = Objects.requireNonNull(status, "status is required");
        this.operatorStatus = Objects.requireNonNull(operatorStatus, "operatorStatus is required");
        this.severity = Objects.requireNonNull(severity, "severity is required");
        this.summary = requireText(summary, "summary");
        this.evidence = requireText(evidence, "evidence");
        this.recommendedAction = requireText(recommendedAction, "recommendedAction");
        this.affectedAttractionIds = new LinkedHashSet<>(affectedAttractionIds);
        this.observedAt = Objects.requireNonNull(observedAt, "observedAt is required");
        this.receivedAt = Objects.requireNonNull(receivedAt, "receivedAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
        this.sourceVersion = sourceVersion;
        this.version = version;
        this.linkedIncidentId = linkedIncidentId;
        this.history = new ArrayList<>(history);
        this.committedCount = committedCount;
        assertInvariants();
    }

    public static WeatherRecommendation receive(WeatherRecommendationInbound inbound, Clock clock) {
        requireClock(clock);
        Instant occurredAt = Instant.now(clock);
        WeatherRecommendationId id = inbound.id();
        WeatherRecommendationActivity received = new WeatherRecommendationActivity(
                newActivityId(),
                id,
                WeatherRecommendationEventType.RECEIVED,
                SYSTEM_ACTOR,
                null,
                occurredAt,
                0L,
                1L,
                inbound.sourceVersion(),
                null
        );
        return new WeatherRecommendation(
                id,
                inbound.ruleId(),
                inbound.status(),
                WeatherRecommendationOperatorStatus.PENDING,
                inbound.severity(),
                inbound.summary(),
                inbound.evidence(),
                inbound.recommendedAction(),
                uniqueAttractionIds(inbound.affectedAttractionIds()),
                inbound.observedAt(),
                occurredAt,
                occurredAt,
                inbound.sourceVersion(),
                1L,
                null,
                List.of(received),
                0
        );
    }

    public static WeatherRecommendation rehydrate(
            WeatherRecommendationId id,
            String ruleId,
            WeatherRecommendationStatus status,
            WeatherRecommendationOperatorStatus operatorStatus,
            WeatherRecommendationSeverity severity,
            String summary,
            String evidence,
            String recommendedAction,
            List<AttractionId> affectedAttractionIds,
            Instant observedAt,
            Instant receivedAt,
            Instant updatedAt,
            long sourceVersion,
            long version,
            IncidentId linkedIncidentId,
            List<WeatherRecommendationActivity> activity
    ) {
        List<WeatherRecommendationActivity> history = activity == null ? List.of() : List.copyOf(activity);
        return new WeatherRecommendation(
                id,
                ruleId,
                status,
                operatorStatus,
                severity,
                summary,
                evidence,
                recommendedAction,
                uniqueAttractionIds(affectedAttractionIds),
                observedAt,
                receivedAt,
                updatedAt,
                sourceVersion,
                version,
                linkedIncidentId,
                history,
                history.size()
        );
    }

    public InboundApplyResult applyInbound(WeatherRecommendationInbound inbound, Clock clock) {
        requireClock(clock);
        Objects.requireNonNull(inbound, "inbound is required");
        if (!id.equals(inbound.id())) {
            throw new IllegalArgumentException("Inbound recommendation id does not match " + id);
        }
        if (inbound.sourceVersion() < sourceVersion) {
            throw new StaleWeatherRecommendationSourceException(id, inbound.sourceVersion(), sourceVersion);
        }
        if (inbound.sourceVersion() == sourceVersion) {
            return InboundApplyResult.DUPLICATE;
        }

        Instant occurredAt = Instant.now(clock);
        long previousVersion = this.version;
        copySource(inbound);
        bump(occurredAt);
        history.add(new WeatherRecommendationActivity(
                newActivityId(),
                id,
                WeatherRecommendationEventType.SOURCE_UPDATED,
                SYSTEM_ACTOR,
                null,
                occurredAt,
                previousVersion,
                this.version,
                this.sourceVersion,
                this.linkedIncidentId
        ));
        return InboundApplyResult.UPDATED;
    }

    public void acknowledge(String actor, String reason, Clock clock) {
        applyOperatorCommand(
                WeatherRecommendationCommand.ACKNOWLEDGE,
                WeatherRecommendationEventType.ACKNOWLEDGED,
                actor,
                reason,
                clock,
                () -> operatorStatus = WeatherRecommendationOperatorStatus.ACKNOWLEDGED
        );
    }

    public void dismiss(String actor, String reason, Clock clock) {
        applyOperatorCommand(
                WeatherRecommendationCommand.DISMISS,
                WeatherRecommendationEventType.DISMISSED,
                actor,
                reason,
                clock,
                () -> operatorStatus = WeatherRecommendationOperatorStatus.DISMISSED
        );
    }

    public void linkIncident(IncidentId incidentId, String actor, String reason, Clock clock) {
        Objects.requireNonNull(incidentId, "incidentId is required");
        applyOperatorCommand(
                WeatherRecommendationCommand.LINK_INCIDENT,
                WeatherRecommendationEventType.INCIDENT_LINKED,
                actor,
                reason,
                clock,
                () -> {
                    this.linkedIncidentId = incidentId;
                    this.operatorStatus = WeatherRecommendationOperatorStatus.LINKED;
                }
        );
    }

    public WeatherRecommendationId id() {
        return id;
    }

    public String ruleId() {
        return ruleId;
    }

    public WeatherRecommendationStatus status() {
        return status;
    }

    public WeatherRecommendationOperatorStatus operatorStatus() {
        return operatorStatus;
    }

    public WeatherRecommendationSeverity severity() {
        return severity;
    }

    public String summary() {
        return summary;
    }

    public String evidence() {
        return evidence;
    }

    public String recommendedAction() {
        return recommendedAction;
    }

    public List<AttractionId> affectedAttractionIds() {
        return List.copyOf(affectedAttractionIds);
    }

    public Instant observedAt() {
        return observedAt;
    }

    public Instant receivedAt() {
        return receivedAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public long sourceVersion() {
        return sourceVersion;
    }

    public long version() {
        return version;
    }

    public IncidentId linkedIncidentId() {
        return linkedIncidentId;
    }

    public boolean simulated() {
        return ruleId.toLowerCase(Locale.ROOT).startsWith("simulated-")
                || evidence.toLowerCase(Locale.ROOT).contains("simulated");
    }

    public List<WeatherRecommendationActivity> activity() {
        return List.copyOf(history);
    }

    public List<WeatherRecommendationActivity> uncommittedActivity() {
        return List.copyOf(history.subList(committedCount, history.size()));
    }

    public void markActivityCommitted() {
        committedCount = history.size();
    }

    private void applyOperatorCommand(
            WeatherRecommendationCommand command,
            WeatherRecommendationEventType eventType,
            String actor,
            String reason,
            Clock clock,
            Runnable mutation
    ) {
        requireActor(actor);
        requireClock(clock);
        if (!allowed(command)) {
            throw new InvalidWeatherRecommendationTransitionException(operatorStatus, command);
        }
        Instant occurredAt = Instant.now(clock);
        long previousVersion = this.version;
        mutation.run();
        bump(occurredAt);
        history.add(new WeatherRecommendationActivity(
                newActivityId(),
                id,
                eventType,
                actor.trim(),
                normalizeReason(reason),
                occurredAt,
                previousVersion,
                this.version,
                this.sourceVersion,
                this.linkedIncidentId
        ));
    }

    private boolean allowed(WeatherRecommendationCommand command) {
        return switch (command) {
            case ACKNOWLEDGE -> operatorStatus.allowsAcknowledge();
            case DISMISS -> operatorStatus.allowsDismiss();
            case LINK_INCIDENT -> operatorStatus.allowsLinkIncident();
        };
    }

    private void copySource(WeatherRecommendationInbound inbound) {
        this.ruleId = inbound.ruleId();
        this.status = inbound.status();
        this.severity = inbound.severity();
        this.summary = inbound.summary();
        this.evidence = inbound.evidence();
        this.recommendedAction = inbound.recommendedAction();
        this.affectedAttractionIds.clear();
        this.affectedAttractionIds.addAll(uniqueAttractionIds(inbound.affectedAttractionIds()));
        this.observedAt = inbound.observedAt();
        this.sourceVersion = inbound.sourceVersion();
    }

    private void bump(Instant occurredAt) {
        this.version = this.version + 1;
        this.updatedAt = occurredAt;
    }

    private void assertInvariants() {
        if (sourceVersion < 1) {
            throw new IllegalStateException("Source version starts at 1");
        }
        if (version < 1) {
            throw new IllegalStateException("Recommendations start at version 1");
        }
        if (history.isEmpty()) {
            throw new IllegalStateException("Recommendations must have an audited receive event");
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
