package com.deanwagman.lumenmarsh.venueops.attraction.api;

import com.deanwagman.lumenmarsh.venueops.attraction.application.GuestAttractionReadService;
import com.deanwagman.lumenmarsh.venueops.attraction.infrastructure.streaming.SseAttractionUpdateBroadcaster;
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
@RequestMapping("/api/v1/attractions/events")
@Tag(name = "Guest attraction stream")
public class AttractionEventController {

    private final SseAttractionUpdateBroadcaster broadcaster;
    private final GuestAttractionReadService guestAttractionReadService;

    public AttractionEventController(
            SseAttractionUpdateBroadcaster broadcaster,
            GuestAttractionReadService guestAttractionReadService
    ) {
        this.broadcaster = broadcaster;
        this.guestAttractionReadService = guestAttractionReadService;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Stream attraction operational updates",
            description = "Server-sent events. The first event is attractions.snapshot; later events are attraction.updated. Reconnection receives a new snapshot rather than replayed history."
    )
    public SseEmitter events(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store");
        response.setHeader("X-Accel-Buffering", "no");
        SseEmitter emitter = broadcaster.subscribe();
        try {
            List<AttractionResponse> snapshot = guestAttractionReadService.listForGuest().stream()
                    .map(detail -> AttractionResponse.from(detail.operational(), detail.experience()))
                    .toList();
            broadcaster.sendSnapshot(emitter, snapshot);
        } catch (RuntimeException ex) {
            broadcaster.unsubscribe(emitter);
            throw ex;
        }
        return emitter;
    }
}
