package com.deanwagman.lumenmarsh.venueops.maintenance.application;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentId;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.activity.MaintenanceActivity;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.asset.MaintenanceAssetId;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.checklist.ChecklistResult;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.checklist.MaintenanceChecklistItem;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.checklist.MaintenanceChecklistItemId;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.event.MaintenanceEventType;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceClassification;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceEvidence;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenancePriority;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceSourceType;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrder;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrderId;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrderStatus;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class MaintenanceCommandResultSnapshot {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private MaintenanceCommandResultSnapshot() {
    }

    static String write(MaintenanceWorkOrder workOrder, MaintenanceActivity activity) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("workOrder", workOrderMap(workOrder));
        body.put("activity", activityMap(activity));
        return JSON.writeValueAsString(body);
    }

    static MaintenanceCommandResult read(String json) {
        JsonNode root = JSON.readTree(json);
        MaintenanceActivity activity = activity(root.get("activity"));
        MaintenanceWorkOrder workOrder = workOrder(root.get("workOrder"), activity);
        return new MaintenanceCommandResult(workOrder, activity, true);
    }

    private static Map<String, Object> workOrderMap(MaintenanceWorkOrder workOrder) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", workOrder.id().toString());
        body.put("workOrderNumber", workOrder.workOrderNumber());
        body.put("assetId", workOrder.assetId().toString());
        body.put("attractionId", workOrder.attractionId().value());
        body.put("incidentId", workOrder.incidentId() == null ? null : workOrder.incidentId().value());
        body.put("sourceType", workOrder.sourceType().name());
        body.put("sourceReferenceId", workOrder.sourceReferenceId());
        body.put("classification", workOrder.classification().name());
        body.put("priority", workOrder.priority().name());
        body.put("status", workOrder.status().name());
        body.put("summary", workOrder.summary());
        body.put("description", workOrder.description());
        body.put("assignedTeam", workOrder.assignedTeam());
        body.put("assignedActorSubject", workOrder.assignedActorSubject());
        body.put("estimatedRestoreAt", iso(workOrder.estimatedRestoreAt()));
        body.put("openedAt", iso(workOrder.openedAt()));
        body.put("workStartedAt", iso(workOrder.workStartedAt()));
        body.put("readyForTestingAt", iso(workOrder.readyForTestingAt()));
        body.put("completedAt", iso(workOrder.completedAt()));
        body.put("canceledAt", iso(workOrder.canceledAt()));
        body.put("version", workOrder.version());
        body.put("createdAt", iso(workOrder.createdAt()));
        body.put("updatedAt", iso(workOrder.updatedAt()));
        body.put("checklist", workOrder.checklist().stream().map(MaintenanceCommandResultSnapshot::checklistMap).toList());
        body.put("evidence", workOrder.evidence().stream().map(MaintenanceCommandResultSnapshot::evidenceMap).toList());
        return body;
    }

    private static Map<String, Object> checklistMap(MaintenanceChecklistItem item) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", item.id().toString());
        body.put("sequence", item.sequence());
        body.put("label", item.label());
        body.put("instructions", item.instructions());
        body.put("required", item.required());
        body.put("result", item.result().name());
        body.put("notes", item.notes());
        body.put("completedBySubject", item.completedBySubject());
        body.put("completedByDisplayName", item.completedByDisplayName());
        body.put("completedAt", iso(item.completedAt()));
        body.put("version", item.version());
        return body;
    }

    private static Map<String, Object> evidenceMap(MaintenanceEvidence item) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", item.id().toString());
        body.put("label", item.label());
        body.put("contentType", item.contentType());
        body.put("uri", item.uri());
        body.put("addedBySubject", item.addedBySubject());
        body.put("addedAt", iso(item.addedAt()));
        return body;
    }

    private static Map<String, Object> activityMap(MaintenanceActivity activity) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", activity.id().toString());
        body.put("workOrderId", activity.workOrderId().toString());
        body.put("sequence", activity.sequence());
        body.put("eventType", activity.eventType().name());
        body.put("fromStatus", activity.fromStatus() == null ? null : activity.fromStatus().name());
        body.put("toStatus", activity.toStatus() == null ? null : activity.toStatus().name());
        body.put("actorSubject", activity.actorSubject());
        body.put("actorDisplayName", activity.actorDisplayName());
        body.put("reason", activity.reason());
        body.put("details", activity.details());
        body.put("commandId", activity.commandId().toString());
        body.put("correlationId", activity.correlationId());
        body.put("occurredAt", iso(activity.occurredAt()));
        body.put("resultingVersion", activity.resultingVersion());
        return body;
    }

    private static MaintenanceWorkOrder workOrder(JsonNode node, MaintenanceActivity activity) {
        List<MaintenanceChecklistItem> checklist = new ArrayList<>();
        JsonNode checklistNode = node.get("checklist");
        if (checklistNode != null && checklistNode.isArray()) {
            checklistNode.forEach(item -> checklist.add(new MaintenanceChecklistItem(
                    MaintenanceChecklistItemId.of(text(item, "id")),
                    (int) item.get("sequence").asLong(),
                    text(item, "label"),
                    text(item, "instructions"),
                    item.get("required").asBoolean(),
                    ChecklistResult.valueOf(text(item, "result")),
                    text(item, "notes"),
                    text(item, "completedBySubject"),
                    text(item, "completedByDisplayName"),
                    instant(item, "completedAt"),
                    item.get("version").asLong()
            )));
        }
        List<MaintenanceEvidence> evidence = new ArrayList<>();
        JsonNode evidenceNode = node.get("evidence");
        if (evidenceNode != null && evidenceNode.isArray()) {
            evidenceNode.forEach(item -> evidence.add(new MaintenanceEvidence(
                    UUID.fromString(text(item, "id")),
                    text(item, "label"),
                    text(item, "contentType"),
                    text(item, "uri"),
                    text(item, "addedBySubject"),
                    instant(item, "addedAt")
            )));
        }
        String incidentId = text(node, "incidentId");
        return MaintenanceWorkOrder.rehydrate(
                MaintenanceWorkOrderId.of(text(node, "id")),
                text(node, "workOrderNumber"),
                MaintenanceAssetId.of(text(node, "assetId")),
                new AttractionId(text(node, "attractionId")),
                incidentId == null ? null : new IncidentId(incidentId),
                MaintenanceSourceType.valueOf(text(node, "sourceType")),
                text(node, "sourceReferenceId"),
                MaintenanceClassification.valueOf(text(node, "classification")),
                MaintenancePriority.valueOf(text(node, "priority")),
                MaintenanceWorkOrderStatus.valueOf(text(node, "status")),
                text(node, "summary"),
                text(node, "description"),
                text(node, "assignedTeam"),
                text(node, "assignedActorSubject"),
                instant(node, "estimatedRestoreAt"),
                instant(node, "openedAt"),
                instant(node, "workStartedAt"),
                instant(node, "readyForTestingAt"),
                instant(node, "completedAt"),
                instant(node, "canceledAt"),
                node.get("version").asLong(),
                instant(node, "createdAt"),
                instant(node, "updatedAt"),
                checklist,
                evidence,
                List.of(activity)
        );
    }

    private static MaintenanceActivity activity(JsonNode node) {
        String fromStatus = text(node, "fromStatus");
        String toStatus = text(node, "toStatus");
        return new MaintenanceActivity(
                UUID.fromString(text(node, "id")),
                MaintenanceWorkOrderId.of(text(node, "workOrderId")),
                node.get("sequence").asLong(),
                MaintenanceEventType.valueOf(text(node, "eventType")),
                fromStatus == null ? null : MaintenanceWorkOrderStatus.valueOf(fromStatus),
                toStatus == null ? null : MaintenanceWorkOrderStatus.valueOf(toStatus),
                text(node, "actorSubject"),
                text(node, "actorDisplayName"),
                text(node, "reason"),
                details(node.get("details")),
                UUID.fromString(text(node, "commandId")),
                text(node, "correlationId"),
                instant(node, "occurredAt"),
                node.get("resultingVersion").asLong()
        );
    }

    private static Map<String, Object> details(JsonNode node) {
        if (node == null || node.isNull() || !node.isObject()) {
            return Map.of();
        }
        Map<String, Object> details = new LinkedHashMap<>();
        node.properties().forEach(entry -> details.put(entry.getKey(), scalar(entry.getValue())));
        return details;
    }

    private static Object scalar(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isNumber()) {
            return node.asLong();
        }
        if (node.isArray()) {
            List<Object> values = new ArrayList<>();
            node.forEach(item -> values.add(scalar(item)));
            return values;
        }
        return node.asString();
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asString();
        return text == null || text.isBlank() ? null : text;
    }

    private static Instant instant(JsonNode node, String field) {
        String value = text(node, field);
        return value == null ? null : Instant.parse(value);
    }

    private static String iso(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
