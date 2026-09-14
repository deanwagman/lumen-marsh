package com.deanwagman.lumenmarsh.venueops.incident.api;

import com.deanwagman.lumenmarsh.venueops.attraction.api.AttractionResponse;
import com.deanwagman.lumenmarsh.venueops.attraction.application.GuestAttractionReadService;
import com.deanwagman.lumenmarsh.venueops.attraction.infrastructure.streaming.SseAttractionUpdateBroadcaster;
import com.deanwagman.lumenmarsh.venueops.incident.application.GuestAdvisoryOperationalSnapshot;
import com.deanwagman.lumenmarsh.venueops.incident.application.GuestAdvisoryOperationalUpdate;
import com.deanwagman.lumenmarsh.venueops.incident.application.GuestAdvisoryUpdatePublisher;
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

    public ParkEventController(
            SseAttractionUpdateBroadcaster broadcaster,
            GuestAttractionReadService guestAttractionReadService,
            IncidentService incidentService
    ) {
        this.broadcaster = broadcaster;
        this.guestAttractionReadService = guestAttractionReadService;
        this.incidentService = incidentService;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Stream park-wide operational updates",
            description = "Server-sent events for attractions and guest advisories. The first events are attractions.snapshot and advisories.snapshot. Later events are attraction.updated, advisory.published, advisory.updated, and advisory.withdrawn. GET /api/v1/attractions/events remains available for attraction-only clients. Reconnection receives fresh snapshots rather than replayed history."
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
        } catch (RuntimeException ex) {
            broadcaster.unsubscribe(emitter);
            throw ex;
        }
        return emitter;
    }
}
