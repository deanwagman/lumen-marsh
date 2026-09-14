package com.deanwagman.lumenmarsh.venueops.attraction.domain;

import java.util.Objects;

public record AttractionId(String value) {

    public AttractionId {
        Objects.requireNonNull(value, "Attraction id is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Attraction id must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
