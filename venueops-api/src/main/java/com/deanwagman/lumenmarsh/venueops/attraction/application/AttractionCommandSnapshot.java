package com.deanwagman.lumenmarsh.venueops.attraction.application;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.Attraction;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatus;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionType;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.CapacityMode;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AttractionCommandSnapshot {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private AttractionCommandSnapshot() {
    }

    static String write(Attraction attraction) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", attraction.id().value());
        body.put("name", attraction.name());
        body.put("area", attraction.area());
        body.put("type", attraction.type().name());
        body.put("status", attraction.status().name());
        body.put("capacityMode", attraction.capacityMode().name());
        body.put("waitMinutes", attraction.waitMinutes());
        body.put("updatedAt", attraction.updatedAt().toString());
        body.put("version", attraction.version());
        return JSON.writeValueAsString(body);
    }

    static Attraction read(String json) {
        JsonNode root = JSON.readTree(json);
        JsonNode wait = root.get("waitMinutes");
        return Attraction.rehydrate(
                new AttractionId(text(root, "id")),
                text(root, "name"),
                text(root, "area"),
                AttractionType.valueOf(text(root, "type")),
                AttractionStatus.valueOf(text(root, "status")),
                CapacityMode.valueOf(text(root, "capacityMode")),
                wait == null || wait.isNull() ? null : wait.asInt(),
                Instant.parse(text(root, "updatedAt")),
                root.get("version").asLong(),
                List.of()
        );
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asString();
    }
}
