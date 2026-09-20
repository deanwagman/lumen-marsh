package com.deanwagman.lumenmarsh.venueops.flow.application;

import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowActivity;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendation;

public record FlowCommandResult(
        FlowRecommendation recommendation,
        FlowActivity activity,
        boolean replay
) {
}
