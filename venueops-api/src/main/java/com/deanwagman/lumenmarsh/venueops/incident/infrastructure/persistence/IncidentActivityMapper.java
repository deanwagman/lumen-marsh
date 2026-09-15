package com.deanwagman.lumenmarsh.venueops.incident.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentActivity;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentAssigned;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentAttractionLinked;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentAttractionUnlinked;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentGuestAdvisoryPublished;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentGuestAdvisoryWithdrawn;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentId;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentReported;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentSeverity;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentSeverityChanged;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentStatus;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentStatusChanged;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentType;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentWorkOrderLinked;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class IncidentActivityMapper {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private IncidentActivityMapper() {
    }

    static IncidentActivityEntity toEntity(IncidentActivity activity) {
        return new IncidentActivityEntity(
                activity.id(),
                activity.incidentId().value(),
                activity.type(),
                activity.actor(),
                activity.reason(),
                activity.occurredAt(),
                activity.previousVersion(),
                activity.resultingVersion(),
                JSON.writeValueAsString(payload(activity))
        );
    }

    static IncidentActivity toDomain(IncidentActivityEntity entity) {
        JsonNode payload = JSON.readTree(entity.getPayload());
        IncidentId incidentId = new IncidentId(entity.getIncidentId());
        return switch (entity.getType()) {
            case INCIDENT_REPORTED -> new IncidentReported(
                    entity.getId(),
                    incidentId,
                    entity.getType(),
                    entity.getActor(),
                    entity.getReason(),
                    entity.getOccurredAt(),
                    entity.getPreviousVersion(),
                    entity.getResultingVersion(),
                    payload.get("title").asString(),
                    IncidentType.valueOf(payload.get("incidentType").asString()),
                    IncidentSeverity.valueOf(payload.get("severity").asString()),
                    attractionIds(payload.get("attractionIds"))
            );
            case INCIDENT_ACKNOWLEDGED, MITIGATION_STARTED, INCIDENT_RESOLVED -> new IncidentStatusChanged(
                    entity.getId(),
                    incidentId,
                    entity.getType(),
                    entity.getActor(),
                    entity.getReason(),
                    entity.getOccurredAt(),
                    entity.getPreviousVersion(),
                    entity.getResultingVersion(),
                    IncidentStatus.valueOf(payload.get("previousStatus").asString()),
                    IncidentStatus.valueOf(payload.get("newStatus").asString())
            );
            case INCIDENT_ASSIGNED -> new IncidentAssigned(
                    entity.getId(),
                    incidentId,
                    entity.getType(),
                    entity.getActor(),
                    entity.getReason(),
                    entity.getOccurredAt(),
                    entity.getPreviousVersion(),
                    entity.getResultingVersion(),
                    payload.get("assignee").asString()
            );
            case SEVERITY_CHANGED -> new IncidentSeverityChanged(
                    entity.getId(),
                    incidentId,
                    entity.getType(),
                    entity.getActor(),
                    entity.getReason(),
                    entity.getOccurredAt(),
                    entity.getPreviousVersion(),
                    entity.getResultingVersion(),
                    IncidentSeverity.valueOf(payload.get("previousSeverity").asString()),
                    IncidentSeverity.valueOf(payload.get("newSeverity").asString())
            );
            case ATTRACTION_LINKED -> new IncidentAttractionLinked(
                    entity.getId(),
                    incidentId,
                    entity.getType(),
                    entity.getActor(),
                    entity.getReason(),
                    entity.getOccurredAt(),
                    entity.getPreviousVersion(),
                    entity.getResultingVersion(),
                    new AttractionId(payload.get("attractionId").asString())
            );
            case ATTRACTION_UNLINKED -> new IncidentAttractionUnlinked(
                    entity.getId(),
                    incidentId,
                    entity.getType(),
                    entity.getActor(),
                    entity.getReason(),
                    entity.getOccurredAt(),
                    entity.getPreviousVersion(),
                    entity.getResultingVersion(),
                    new AttractionId(payload.get("attractionId").asString())
            );
            case GUEST_ADVISORY_PUBLISHED -> new IncidentGuestAdvisoryPublished(
                    entity.getId(),
                    incidentId,
                    entity.getType(),
                    entity.getActor(),
                    entity.getReason(),
                    entity.getOccurredAt(),
                    entity.getPreviousVersion(),
                    entity.getResultingVersion(),
                    payload.get("guestTitle").asString(),
                    payload.get("guestMessage").asString()
            );
            case GUEST_ADVISORY_WITHDRAWN -> new IncidentGuestAdvisoryWithdrawn(
                    entity.getId(),
                    incidentId,
                    entity.getType(),
                    entity.getActor(),
                    entity.getReason(),
                    entity.getOccurredAt(),
                    entity.getPreviousVersion(),
                    entity.getResultingVersion()
            );
            case WORK_ORDER_LINKED -> new IncidentWorkOrderLinked(
                    entity.getId(),
                    incidentId,
                    entity.getType(),
                    entity.getActor(),
                    entity.getReason(),
                    entity.getOccurredAt(),
                    entity.getPreviousVersion(),
                    entity.getResultingVersion(),
                    payload.get("workOrderId").asString(),
                    payload.get("workOrderNumber").asString()
            );
        };
    }

    private static Map<String, Object> payload(IncidentActivity activity) {
        Map<String, Object> payload = new LinkedHashMap<>();
        switch (activity) {
            case IncidentReported reported -> {
                payload.put("title", reported.title());
                payload.put("incidentType", reported.incidentType().name());
                payload.put("severity", reported.severity().name());
                payload.put("attractionIds", reported.attractionIds().stream().map(AttractionId::value).toList());
            }
            case IncidentStatusChanged changed -> {
                payload.put("previousStatus", changed.previousStatus().name());
                payload.put("newStatus", changed.newStatus().name());
            }
            case IncidentAssigned assigned -> payload.put("assignee", assigned.assignee());
            case IncidentSeverityChanged changed -> {
                payload.put("previousSeverity", changed.previousSeverity().name());
                payload.put("newSeverity", changed.newSeverity().name());
            }
            case IncidentAttractionLinked linked -> payload.put("attractionId", linked.attractionId().value());
            case IncidentAttractionUnlinked unlinked -> payload.put("attractionId", unlinked.attractionId().value());
            case IncidentGuestAdvisoryPublished published -> {
                payload.put("guestTitle", published.guestTitle());
                payload.put("guestMessage", published.guestMessage());
            }
            case IncidentGuestAdvisoryWithdrawn ignored -> {
            }
            case IncidentWorkOrderLinked linked -> {
                payload.put("workOrderId", linked.workOrderId());
                payload.put("workOrderNumber", linked.workOrderNumber());
            }
        }
        return payload;
    }

    private static List<AttractionId> attractionIds(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<AttractionId> ids = new ArrayList<>();
        node.forEach(item -> ids.add(new AttractionId(item.asString())));
        return List.copyOf(ids);
    }
}
