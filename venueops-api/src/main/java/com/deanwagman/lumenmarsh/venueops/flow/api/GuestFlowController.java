package com.deanwagman.lumenmarsh.venueops.flow.api;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.flow.application.FlowQueryService;
import com.deanwagman.lumenmarsh.venueops.flow.application.FlowSnapshots;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Guest park flow")
public class GuestFlowController {

    private final FlowQueryService queries;

    public GuestFlowController(FlowQueryService queries) {
        this.queries = queries;
    }

    @GetMapping("/flow/overview")
    @Operation(summary = "Guest-safe park flow overview")
    public FlowQueryService.GuestOverview overview() {
        return queries.guestOverview();
    }

    @GetMapping("/flow/recommendations")
    @Operation(summary = "Published guest flow guidance")
    public List<FlowSnapshots.GuestGuidanceSnapshot> recommendations() {
        return queries.guestRecommendations();
    }

    @GetMapping("/attractions/{attractionId}/wait-forecast")
    @Operation(summary = "Guest-safe wait outlook for one attraction")
    public FlowSnapshots.GuestWaitSnapshot waitForecast(@PathVariable String attractionId) {
        return queries.guestWaitForecast(new AttractionId(attractionId));
    }
}
