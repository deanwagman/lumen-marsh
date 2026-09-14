package com.deanwagman.lumenmarsh.venueops.attraction.application;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionActivity;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionCapacityChanged;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatusChanged;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionWaitTimeChanged;

public enum AttractionUpdateEventType {
    STATUS_CHANGED,
    CAPACITY_CHANGED,
    WAIT_TIME_CHANGED;

    public static AttractionUpdateEventType from(AttractionActivity activity) {
        return switch (activity) {
            case AttractionStatusChanged ignored -> STATUS_CHANGED;
            case AttractionCapacityChanged ignored -> CAPACITY_CHANGED;
            case AttractionWaitTimeChanged ignored -> WAIT_TIME_CHANGED;
        };
    }
}
