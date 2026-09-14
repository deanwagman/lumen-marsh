package com.deanwagman.lumenmarsh.venueops.attraction.application;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;

public class AttractionNotFoundException extends RuntimeException {

    private final AttractionId attractionId;

    public AttractionNotFoundException(AttractionId attractionId) {
        super("Attraction not found: " + attractionId);
        this.attractionId = attractionId;
    }

    public AttractionId attractionId() {
        return attractionId;
    }
}
