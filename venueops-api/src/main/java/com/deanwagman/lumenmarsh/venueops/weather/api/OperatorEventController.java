package com.deanwagman.lumenmarsh.venueops.weather.api;

import com.deanwagman.lumenmarsh.venueops.attraction.api.AttractionResponse;
import com.deanwagman.lumenmarsh.venueops.attraction.application.GuestAttractionReadService;
import com.deanwagman.lumenmarsh.venueops.attraction.infrastructure.streaming.SseAttractionUpdateBroadcaster;
import com.deanwagman.lumenmarsh.venueops.flow.application.FlowOperationalUpdate;
import com.deanwagman.lumenmarsh.venueops.flow.application.FlowQueryService;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentOperationalSnapshot;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentOperationalUpdate;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentService;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceOperationalUpdate;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceWorkOrderFilter;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceWorkOrderQueryService;
import com.deanwagman.lumenmarsh.venueops.security.VenueOpsScopes;
import com.deanwagman.lumenmarsh.venueops.weather.application.WeatherRecommendationOperationalSnapshot;
import com.deanwagman.lumenmarsh.venueops.weather.application.WeatherRecommendationOperationalUpdate;
import com.deanwagman.lumenmarsh.venueops.weather.application.WeatherRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/v1/operator/events")
@Tag(name = "Operator live stream")
public class OperatorEventController {

    private final SseAttractionUpdateBroadcaster broadcaster;
    private final GuestAttractionReadService guestAttractionReadService;
    private final WeatherRecommendationService weatherRecommendationService;
    private final IncidentService incidentService;
    private final MaintenanceWorkOrderQueryService maintenanceWorkOrders;
    private final FlowQueryService flowQueries;

    public OperatorEventController(
            SseAttractionUpdateBroadcaster broadcaster,
            GuestAttractionReadService guestAttractionReadService,
            WeatherRecommendationService weatherRecommendationService,
            IncidentService incidentService,
            MaintenanceWorkOrderQueryService maintenanceWorkOrders,
            FlowQueryService flowQueries
    ) {
        this.broadcaster = broadcaster;
        this.guestAttractionReadService = guestAttractionReadService;
        this.weatherRecommendationService = weatherRecommendationService;
        this.incidentService = incidentService;
        this.maintenanceWorkOrders = maintenanceWorkOrders;
        this.flowQueries = flowQueries;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_OPERATOR_READ + "')")
    @Operation(
            summary = "Stream operator console updates",
            description = "Server-sent events for Control Tower. Connecting requires venueops/operator.read. The first events are attractions.snapshot, weather.recommendations.snapshot, and incidents.snapshot. Subscribers that also hold venueops/maintenance.read receive maintenance snapshots. Subscribers that hold venueops/flow.read receive flow.snapshot and later flow.* events."
    )
    public SseEmitter events(HttpServletResponse response, Authentication authentication) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store");
        response.setHeader("X-Accel-Buffering", "no");
        boolean maintenanceRead = hasAuthority(authentication, VenueOpsScopes.SCOPE_MAINTENANCE_READ);
        boolean flowRead = hasAuthority(authentication, VenueOpsScopes.SCOPE_FLOW_READ);
        SseEmitter emitter = broadcaster.subscribeOperator(maintenanceRead, flowRead);
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
            if (maintenanceRead) {
                var workOrders = maintenanceWorkOrders.list(new MaintenanceWorkOrderFilter(
                        null, null, null, null, null, null, null, null, null, null, 0, 100
                )).items().stream()
                        .map(MaintenanceOperationalUpdate.MaintenanceWorkOrderSnapshot::from)
                        .toList();
                broadcaster.sendNamed(emitter, MaintenanceOperationalUpdate.SNAPSHOT_EVENT, null, workOrders);
            }
            if (flowRead) {
                broadcaster.sendNamed(emitter, FlowOperationalUpdate.SNAPSHOT_EVENT, null, flowQueries.overview());
            }
        } catch (RuntimeException ex) {
            broadcaster.unsubscribe(emitter);
            throw ex;
        }
        return emitter;
    }

    private static boolean hasAuthority(Authentication authentication, String authority) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }
}
