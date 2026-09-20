package com.deanwagman.lumenmarsh.venueops.testsupport;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.UUID;

/**
 * Wraps legacy attraction/incident command JSON into the shared command envelope
 * ({@code commandId}, {@code type}, {@code expectedVersion}, {@code reason}, {@code data}).
 */
public final class CommandJson {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String[] DATA_KEYS = {
            "waitMinutes",
            "assignee",
            "severity",
            "attractionId",
            "guestTitle",
            "guestMessage",
            "confirmActiveWorkOrders"
    };

    private CommandJson() {
    }

    public static String envelope(String json) {
        JsonNode parsed = JSON.readTree(json);
        if (!(parsed instanceof ObjectNode obj)) {
            return json;
        }
        if (!obj.has("commandId") || obj.get("commandId").isNull()) {
            obj.put("commandId", UUID.randomUUID().toString());
        }
        ObjectNode data = obj.get("data") instanceof ObjectNode existing
                ? existing.deepCopy()
                : JSON.createObjectNode();
        for (String key : DATA_KEYS) {
            if (obj.has(key)) {
                data.set(key, obj.remove(key));
            }
        }
        if (!data.isEmpty()) {
            obj.set("data", data);
        }
        return JSON.writeValueAsString(obj);
    }
}
