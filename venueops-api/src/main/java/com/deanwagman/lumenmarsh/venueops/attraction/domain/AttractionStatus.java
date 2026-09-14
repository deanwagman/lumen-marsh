package com.deanwagman.lumenmarsh.venueops.attraction.domain;

public enum AttractionStatus {
    CLOSED("Currently closed."),
    TESTING("Preparing to welcome explorers."),
    RETURNING_TO_SERVICE("Expected to reopen soon."),
    OPERATING(null),
    WEATHER_HOLD("Temporarily unavailable due to nearby weather."),
    TECHNICAL_DELAY("Temporarily unavailable.");

    private final String guestMessage;

    AttractionStatus(String guestMessage) {
        this.guestMessage = guestMessage;
    }

    public String guestMessage() {
        return guestMessage;
    }

    public boolean isOperating() {
        return this == OPERATING;
    }
}
