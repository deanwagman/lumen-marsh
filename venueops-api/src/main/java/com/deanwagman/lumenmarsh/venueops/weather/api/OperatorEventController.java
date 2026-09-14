package com.deanwagman.lumenmarsh.venueops.weather.api;

import com.deanwagman.lumenmarsh.venueops.attraction.api.AttractionResponse;
import com.deanwagman.lumenmarsh.venueops.attraction.application.GuestAttractionReadService;
import com.deanwagman.lumenmarsh.venueops.attraction.infrastructure.streaming.SseAttractionUpdateBroadcaster;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentOperationalSnapshot;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentOperationalUpdate;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentService;
import com.deanwagman.lumenmarsh.venueops.weather.application.WeatherRecommendationOperationalSnapshot;
import com.deanwagman.lumenmarsh.venueops.weather.application.WeatherRecommendationOperationalUpdate;
import com.deanwagman.lumenmarsh.venueops.weather.application.WeatherRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.deanwagman.lumenmarsh.venueops.security.VenueOpsScopes;

import java.util.List;

@RestController
@RequestMapping("/api/v1/operator/events")
@Tag(name = "Operator live stream")
public class OperatorEventController {

    private final SseAttractionUpdateBroadcaster broadcaster;
    private final GuestAttractionReadService guestAttractionReadService;
    private final WeatherRecommendationService weatherRecommendationService;
    private final IncidentService incidentService;

    public OperatorEventController(
            SseAttractionUpdateBroadcaster broadcaster,
            GuestAttractionReadService guestAttractionReadService,
            WeatherRecommendationService weatherRecommendationService,
            IncidentService incidentService
    ) {
        this.broadcaster = broadcaster;
        this.guestAttractionReadService = guestAttractionReadService;
        this.weatherRecommendationService = weatherRecommendationService;
        this.incidentService = incidentService;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_OPERATOR_READ + "')")
    @Operation(
            summary = "Stream operator console updates",
            description = "Server-sent events for Control Tower. The first events are attractions.snapshot, weather.recommendations.snapshot, and incidents.snapshot. Later events are attraction.updated, weather.recommendation.updated, weather.recommendation.cleared, incident.reported, incident.updated, and incident.resolved. Incident events are operator-only. Guest Flutter clients must use GET /api/v1/attractions/events or GET /api/v1/events."
    )
    public SseEmitter events(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store");
        response.setHeader("X-Accel-Buffering", "no");
        SseEmitter emitter = broadcaster.subscribeOperator();
        try {
            List<AttractionResponse> attractions = guestAttractionReadService.listForGuest().stream()
                    .map(detail -> AttractionResponse.from(detail.operational(), detail.experience()))
                    .toList();
            broadcaster.sendSnapshot(emitter, attractions);
            List<WeatherRecommendationOperationalSnapshot> recommendations = weatherRecommendationService.list().stream()
                    .map(WeatherRecommendationOperationalSnapshot::from)
                    .toList();
            broadcaster.sendNamed(emitter, WeatherRecommendationOperationalUpdate.SNAPSHOT_EVENT, null, recommendations);
            List<IncidentOperationalSnapshot> incidents = incidentService.list().stream()
                    .map(IncidentOperationalSnapshot::from)
                    .toList();
            broadcaster.sendNamed(emitter, IncidentOperationalUpdate.SNAPSHOT_EVENT, null, incidents);
        } catch (RuntimeException ex) {
            broadcaster.unsubscribe(emitter);
            throw ex;
        }
        return emitter;
    }
}
