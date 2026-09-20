package com.deanwagman.lumenmarsh.venueops.flow.application;

import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowActivity;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowEventType;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendation;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationCommand;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationId;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationStatus;
import com.deanwagman.lumenmarsh.venueops.security.ActorIdentity;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class FlowRecommendationService {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final FlowRecommendationRepository recommendations;
    private final FlowProcessedCommandRepository processedCommands;
    private final FlowUpdatePublisher publisher;
    private final FlowMetrics metrics;
    private final Clock clock;

    public FlowRecommendationService(
            FlowRecommendationRepository recommendations,
            FlowProcessedCommandRepository processedCommands,
            FlowUpdatePublisher publisher,
            FlowMetrics metrics,
            Clock clock
    ) {
        this.recommendations = Objects.requireNonNull(recommendations);
        this.processedCommands = Objects.requireNonNull(processedCommands);
        this.publisher = Objects.requireNonNull(publisher);
        this.metrics = Objects.requireNonNull(metrics);
        this.clock = Objects.requireNonNull(clock);
    }

    public FlowRecommendation get(FlowRecommendationId id) {
        expireDue();
        return recommendations.findById(id).orElseThrow(() -> new FlowRecommendationNotFoundException(id));
    }

    public List<FlowRecommendation> list() {
        expireDue();
        return recommendations.findAll();
    }

    @Transactional
    public FlowCommandResult execute(
            FlowRecommendationId id,
            UUID commandId,
            String correlationId,
            FlowRecommendationCommand command,
            long expectedVersion,
            String reason,
            String guestMessage,
            ActorIdentity actor
    ) {
        return processedCommands.findByCommandId(commandId)
                .map(processed -> replay(processed, id))
                .orElseGet(() -> executeNew(
                        id,
                        commandId,
                        correlationId,
                        command,
                        expectedVersion,
                        reason,
                        guestMessage,
                        actor
                ));
    }

    @Transactional
    public void expireDue() {
        for (FlowRecommendation recommendation : recommendations.findAll()) {
            FlowActivity activity = recommendation.expireIfDue(clock);
            if (activity != null) {
                recommendations.save(recommendation);
                publish(recommendation, activity);
            }
        }
    }

    private FlowCommandResult executeNew(
            FlowRecommendationId id,
            UUID commandId,
            String correlationId,
            FlowRecommendationCommand command,
            long expectedVersion,
            String reason,
            String guestMessage,
            ActorIdentity actor
    ) {
        FlowRecommendation recommendation = get(id);
        if (recommendation.version() != expectedVersion) {
            throw new StaleFlowRecommendationVersionException(id, expectedVersion, recommendation.version());
        }
        FlowActivity activity = recommendation.apply(command, actor, commandId, correlationId, reason, guestMessage, clock);
        recommendations.save(recommendation);
        processedCommands.save(new FlowProcessedCommandRepository.ProcessedCommand(
                commandId,
                recommendation.id(),
                activity.eventType(),
                activity.resultingVersion(),
                activity.occurredAt(),
                snapshot(recommendation, activity)
        ));
        metrics.command();
        AfterCommit.run(() -> publish(recommendation, activity));
        return new FlowCommandResult(recommendation, activity, false);
    }

    private FlowCommandResult replay(
            FlowProcessedCommandRepository.ProcessedCommand processed,
            FlowRecommendationId requestedId
    ) {
        if (!processed.recommendationId().value().equals(requestedId.value())) {
            throw new ConflictingFlowCommandException(
                    processed.commandId(),
                    processed.recommendationId().value(),
                    requestedId.value()
            );
        }
        return readSnapshot(processed.resultJson());
    }

    private void publish(FlowRecommendation recommendation, FlowActivity activity) {
        publisher.publish(new FlowOperationalUpdate(
                activity.id().toString(),
                activity.eventType() == FlowEventType.RECOMMENDATION_CREATED
                        ? FlowUpdateEventType.RECOMMENDATION_CREATED
                        : FlowUpdateEventType.RECOMMENDATION_UPDATED,
                activity.occurredAt(),
                FlowSnapshots.RecommendationSnapshot.from(recommendation)
        ));
        if (activity.eventType() == FlowEventType.RECOMMENDATION_PUBLISHED) {
            publisher.publish(new GuestFlowOperationalUpdate(
                    activity.id().toString(),
                    GuestFlowUpdateEventType.RECOMMENDATION_PUBLISHED,
                    activity.occurredAt(),
                    FlowSnapshots.GuestGuidanceSnapshot.from(recommendation)
            ));
        }
        if (activity.eventType() == FlowEventType.RECOMMENDATION_WITHDRAWN
                || (activity.eventType() == FlowEventType.RECOMMENDATION_EXPIRED
                && activity.fromStatus() == FlowRecommendationStatus.PUBLISHED)) {
            publisher.publish(new GuestFlowOperationalUpdate(
                    activity.id().toString(),
                    GuestFlowUpdateEventType.RECOMMENDATION_WITHDRAWN,
                    activity.occurredAt(),
                    FlowSnapshots.GuestGuidanceSnapshot.from(recommendation)
            ));
        }
    }

    private static String snapshot(FlowRecommendation recommendation, FlowActivity activity) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("recommendationId", recommendation.id().value());
        body.put("type", recommendation.type().name());
        body.put("status", recommendation.status().name());
        body.put("severity", recommendation.severity().name());
        body.put("sourceAttractionId", recommendation.sourceAttractionId() == null ? null : recommendation.sourceAttractionId().value());
        body.put("affectedAttractionIds", recommendation.affectedAttractionIds().stream().map(id -> id.value()).toList());
        body.put("recommendedDestinationIds", recommendation.recommendedDestinationIds().stream().map(id -> id.value()).toList());
        body.put("summary", recommendation.summary());
        body.put("explanation", recommendation.explanation());
        body.put("guestMessage", recommendation.guestMessage());
        body.put("expiresAt", recommendation.expiresAt().toString());
        body.put("relatedIncidentId", recommendation.relatedIncidentId());
        body.put("relatedWorkOrderId", recommendation.relatedWorkOrderId());
        body.put("version", recommendation.version());
        body.put("createdAt", recommendation.createdAt().toString());
        body.put("updatedAt", recommendation.updatedAt().toString());
        body.put("simulated", recommendation.simulated());
        Map<String, Object> activityMap = new LinkedHashMap<>();
        activityMap.put("id", activity.id().toString());
        activityMap.put("sequence", activity.sequence());
        activityMap.put("eventType", activity.eventType().name());
        activityMap.put("fromStatus", activity.fromStatus() == null ? null : activity.fromStatus().name());
        activityMap.put("toStatus", activity.toStatus().name());
        activityMap.put("actorSubject", activity.actorSubject());
        activityMap.put("actorDisplayName", activity.actorDisplayName());
        activityMap.put("actorType", activity.actorType().name());
        activityMap.put("reason", activity.reason());
        activityMap.put("details", activity.details());
        activityMap.put("commandId", activity.commandId().toString());
        activityMap.put("correlationId", activity.correlationId());
        activityMap.put("occurredAt", activity.occurredAt().toString());
        activityMap.put("resultingVersion", activity.resultingVersion());
        body.put("activity", activityMap);
        return JSON.writeValueAsString(body);
    }

    private static FlowCommandResult readSnapshot(String json) {
        JsonNode root = JSON.readTree(json);
        FlowRecommendation recommendation = FlowRecommendation.rehydrate(
                new FlowRecommendationId(text(root, "recommendationId")),
                com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationType.valueOf(text(root, "type")),
                FlowRecommendationStatus.valueOf(text(root, "status")),
                com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationSeverity.valueOf(text(root, "severity")),
                text(root, "sourceAttractionId") == null
                        ? null
                        : new com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId(text(root, "sourceAttractionId")),
                strings(root.get("affectedAttractionIds")).stream()
                        .map(com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId::new)
                        .toList(),
                strings(root.get("recommendedDestinationIds")).stream()
                        .map(com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId::new)
                        .toList(),
                text(root, "summary"),
                text(root, "explanation"),
                text(root, "guestMessage"),
                Instant.parse(text(root, "expiresAt")),
                text(root, "relatedIncidentId"),
                text(root, "relatedWorkOrderId"),
                root.get("version").asLong(),
                Instant.parse(text(root, "createdAt")),
                Instant.parse(text(root, "updatedAt")),
                root.get("simulated").asBoolean(),
                List.of(activity(root.get("activity"), text(root, "recommendationId")))
        );
        return new FlowCommandResult(recommendation, recommendation.activity().getFirst(), true);
    }

    private static FlowActivity activity(JsonNode node, String recommendationId) {
        String from = text(node, "fromStatus");
        return new FlowActivity(
                UUID.fromString(text(node, "id")),
                new FlowRecommendationId(recommendationId),
                node.get("sequence").asLong(),
                FlowEventType.valueOf(text(node, "eventType")),
                from == null ? null : FlowRecommendationStatus.valueOf(from),
                FlowRecommendationStatus.valueOf(text(node, "toStatus")),
                text(node, "actorSubject"),
                text(node, "actorDisplayName"),
                com.deanwagman.lumenmarsh.venueops.security.ActorType.valueOf(text(node, "actorType")),
                text(node, "reason"),
                Map.of(),
                UUID.fromString(text(node, "commandId")),
                text(node, "correlationId"),
                Instant.parse(text(node, "occurredAt")),
                node.get("resultingVersion").asLong()
        );
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asString();
        return text == null || text.isBlank() ? null : text;
    }

    private static List<String> strings(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        node.forEach(item -> values.add(item.asString()));
        return values;
    }
}
