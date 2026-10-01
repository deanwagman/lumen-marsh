package com.deanwagman.lumenmarsh.venueops.attraction.api;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionCommand;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record AttractionCommandRequest(
        @NotNull UUID commandId,
        @NotNull AttractionCommand type,
        @NotNull Long expectedVersion,
        String reason,
        Map<String, Object> data
) {
    public Integer waitMinutes() {
        Object value = data == null ? null : data.get("waitMinutes");
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(value.toString());
    }
}
