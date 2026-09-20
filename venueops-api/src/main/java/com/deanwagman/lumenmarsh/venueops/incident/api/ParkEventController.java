package com.deanwagman.lumenmarsh.venueops.incident.api;

import com.deanwagman.lumenmarsh.venueops.attraction.api.AttractionResponse;
import com.deanwagman.lumenmarsh.venueops.attraction.application.GuestAttractionReadService;
import com.deanwagman.lumenmarsh.venueops.attraction.infrastructure.streaming.SseAttractionUpdateBroadcaster;
import com.deanwagman.lumenmarsh.venueops.flow.application.FlowQueryService;
import com.deanwagman.lumenmarsh.venueops.flow.application.GuestFlowOperationalUpdate;
import com.deanwagman.lumenmarsh.venueops.incident.application.GuestAdvisoryOperationalSnapshot;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Park live stream")
public class ParkEventController {

    private final SseAttractionUpdateBroadcaster broadcaster;
    private final GuestAttractionReadService guestAttractionReadService;
    private final IncidentService incidentService;
    private final FlowQueryService flowQueries;

    public ParkEventController(
            SseAttractionUpdateBroadcaster broadcaster,
            GuestAttractionReadService guestAttractionReadService,
            IncidentService incidentService,
            FlowQueryService flowQueries
    ) {
        this.broadcaster = broadcaster;
        this.guestAttractionReadService = guestAttractionReadService;
        this.incidentService = incidentService;
        this.flowQueries = flowQueries;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Stream park-wide operational updates",
            description = "Server-sent events for attractions, guest advisories, and guest-safe flow updates. The first events are attractions.snapshot, advisories.snapshot, and guest.flow.updated. Reconnection receives fresh snapshots rather than replayed history."
    )
    public SseEmitter events(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store");
        response.setHeader("X-Accel-Buffering", "no");
        SseEmitter emitter = broadcaster.subscribe();
        try {
            List<AttractionResponse> attractions = guestAttractionReadService.listForGuest().stream()
                    .map(detail -> AttractionResponse.from(detail.operational(), detail.experience()))
                    .toList();
            broadcaster.sendSnapshot(emitter, attractions);
            List<GuestAdvisoryOperationalSnapshot> advisories = incidentService.listActiveGuestAdvisories().stream()
                    .map(GuestAdvisoryOperationalSnapshot::from)
                    .toList();
            broadcaster.sendAdvisoriesSnapshot(emitter, advisories);
            broadcaster.sendNamed(emitter, GuestFlowOperationalUpdate.UPDATED_EVENT, null, flowQueries.guestOverview());
        } catch (RuntimeException ex) {
            broadcaster.unsubscribe(emitter);
            throw ex;
        }
        return emitter;
    }
}
