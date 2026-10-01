package com.deanwagman.lumenmarsh.venueops.incident.application;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.incident.domain.Incident;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentEventType;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentId;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentReported;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentSeverity;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentStatus;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class IncidentCommandSnapshot {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private IncidentCommandSnapshot() {
    }

    static String write(Incident incident) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", incident.id().value());
        body.put("title", incident.title());
        body.put("type", incident.type().name());
        body.put("severity", incident.severity().name());
        body.put("status", incident.status().name());
        body.put("internalDescription", incident.internalDescription());
        body.put("assignedTo", incident.assignedTo());
        body.put("guestTitle", incident.guestTitle());
        body.put("guestMessage", incident.guestMessage());
        body.put("guestAdvisoryPublished", incident.guestAdvisoryPublished());
        body.put("createdAt", incident.createdAt().toString());
        body.put("updatedAt", incident.updatedAt().toString());
        body.put("version", incident.version());
        body.put("attractionIds", incident.attractionIds().stream().map(AttractionId::value).toList());
        return JSON.writeValueAsString(body);
    }

    static Incident read(String json) {
        JsonNode root = JSON.readTree(json);
        IncidentId id = new IncidentId(text(root, "id"));
        Instant createdAt = Instant.parse(text(root, "createdAt"));
        List<AttractionId> attractionIds = strings(root.get("attractionIds")).stream()
                .map(AttractionId::new)
                .toList();
        return Incident.rehydrate(
                id,
                text(root, "title"),
                IncidentType.valueOf(text(root, "type")),
                IncidentSeverity.valueOf(text(root, "severity")),
                IncidentStatus.valueOf(text(root, "status")),
                text(root, "internalDescription"),
                text(root, "assignedTo"),
                text(root, "guestTitle"),
                text(root, "guestMessage"),
                root.get("guestAdvisoryPublished").asBoolean(),
                createdAt,
                Instant.parse(text(root, "updatedAt")),
                root.get("version").asLong(),
                attractionIds,
                List.of(new IncidentReported(
                        id.value() + "-replay",
                        id,
                        IncidentEventType.INCIDENT_REPORTED,
                        "replay",
                        null,
                        createdAt,
                        0L,
                        1L,
                        text(root, "title"),
                        IncidentType.valueOf(text(root, "type")),
                        IncidentSeverity.valueOf(text(root, "severity")),
                        attractionIds
                ))
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
