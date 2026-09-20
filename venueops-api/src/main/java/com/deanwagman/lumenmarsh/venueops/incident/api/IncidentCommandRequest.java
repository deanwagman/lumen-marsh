package com.deanwagman.lumenmarsh.venueops.incident.api;

import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentCommand;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentSeverity;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record IncidentCommandRequest(
        @NotNull UUID commandId,
        @NotNull IncidentCommand type,
        @NotNull Long expectedVersion,
        String reason,
        Map<String, Object> data
) {
    public String assignee() {
        return text("assignee");
    }

    public IncidentSeverity severity() {
        String value = text("severity");
        return value == null ? null : IncidentSeverity.valueOf(value);
    }

    public String attractionId() {
        return text("attractionId");
    }

    public String guestTitle() {
        return text("guestTitle");
    }

    public String guestMessage() {
        return text("guestMessage");
    }

    public boolean confirmActiveWorkOrders() {
        Object value = data == null ? null : data.get("confirmActiveWorkOrders");
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null && Boolean.parseBoolean(value.toString());
    }

    private String text(String key) {
        Object value = data == null ? null : data.get(key);
        if (value == null) {
            return null;
        }
        String text = value.toString();
        return text.isBlank() ? null : text;
    }
}
