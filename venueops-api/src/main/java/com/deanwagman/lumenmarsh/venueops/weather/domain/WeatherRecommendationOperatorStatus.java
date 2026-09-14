package com.deanwagman.lumenmarsh.venueops.weather.domain;

public enum WeatherRecommendationOperatorStatus {
    PENDING,
    ACKNOWLEDGED,
    DISMISSED,
    LINKED;

    public boolean allowsAcknowledge() {
        return this == PENDING;
    }

    public boolean allowsDismiss() {
        return this != DISMISSED;
    }

    public boolean allowsLinkIncident() {
        return this == PENDING || this == ACKNOWLEDGED;
    }
}
