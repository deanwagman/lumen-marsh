package com.deanwagman.lumenmarsh.venueops.flow.application;

import java.time.Instant;

public record GuestFlowOperationalUpdate(
        String eventId,
        GuestFlowUpdateEventType eventType,
        Instant occurredAt,
        Object payload
) {
    public static final String UPDATED_EVENT = "guest.flow.updated";
    public static final String RECOMMENDATION_PUBLISHED_EVENT = "guest.flow.recommendation.published";
    public static final String RECOMMENDATION_WITHDRAWN_EVENT = "guest.flow.recommendation.withdrawn";

    public String sseEventName() {
        return switch (eventType) {
            case UPDATED -> UPDATED_EVENT;
            case RECOMMENDATION_PUBLISHED -> RECOMMENDATION_PUBLISHED_EVENT;
            case RECOMMENDATION_WITHDRAWN -> RECOMMENDATION_WITHDRAWN_EVENT;
        };
    }
}
