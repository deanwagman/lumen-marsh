package com.deanwagman.lumenmarsh.venueops.attraction.domain;

import java.time.Instant;

public sealed interface AttractionActivity
        permits AttractionStatusChanged, AttractionCapacityChanged, AttractionWaitTimeChanged {

    String id();

    AttractionId attractionId();

    AttractionEventType type();

    String actor();

    String reason();

    Instant occurredAt();

    long previousVersion();

    long resultingVersion();
}
