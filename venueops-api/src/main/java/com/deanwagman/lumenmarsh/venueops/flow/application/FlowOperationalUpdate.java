package com.deanwagman.lumenmarsh.venueops.flow.application;

import java.time.Instant;

public record FlowOperationalUpdate(
        String eventId,
        FlowUpdateEventType eventType,
        Instant occurredAt,
        Object payload
) {
    public static final String SNAPSHOT_EVENT = "flow.snapshot";
    public static final String QUEUE_UPDATED_EVENT = "flow.queue.updated";
    public static final String FORECAST_UPDATED_EVENT = "flow.forecast.updated";
    public static final String RECOMMENDATION_CREATED_EVENT = "flow.recommendation.created";
    public static final String RECOMMENDATION_UPDATED_EVENT = "flow.recommendation.updated";

    public String sseEventName() {
        return switch (eventType) {
            case SNAPSHOT -> SNAPSHOT_EVENT;
            case QUEUE_UPDATED -> QUEUE_UPDATED_EVENT;
            case FORECAST_UPDATED -> FORECAST_UPDATED_EVENT;
            case RECOMMENDATION_CREATED -> RECOMMENDATION_CREATED_EVENT;
            case RECOMMENDATION_UPDATED -> RECOMMENDATION_UPDATED_EVENT;
        };
    }
}
