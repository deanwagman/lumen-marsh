package com.deanwagman.lumenmarsh.venueops.incident.domain;

import java.util.Objects;

public record IncidentId(String value) {

    public IncidentId {
        Objects.requireNonNull(value, "Incident id is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Incident id must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
