package com.deanwagman.lumenmarsh.venueops.attraction.application;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;

public class StaleAttractionVersionException extends RuntimeException {

    private final AttractionId attractionId;
    private final long expectedVersion;
    private final long actualVersion;

    public StaleAttractionVersionException(AttractionId attractionId, long expectedVersion, long actualVersion) {
        super("Stale version for " + attractionId + ": expected " + expectedVersion + " but was " + actualVersion);
        this.attractionId = attractionId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }

    public AttractionId attractionId() {
        return attractionId;
    }

    public long expectedVersion() {
        return expectedVersion;
    }

    public long actualVersion() {
        return actualVersion;
    }
}
