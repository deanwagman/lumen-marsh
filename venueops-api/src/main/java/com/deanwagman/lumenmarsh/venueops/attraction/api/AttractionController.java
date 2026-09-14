package com.deanwagman.lumenmarsh.venueops.attraction.api;

import com.deanwagman.lumenmarsh.venueops.attraction.application.GuestAttractionReadService;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attractions")
@Tag(name = "Guest attractions")
public class AttractionController {

    private final GuestAttractionReadService guestAttractionReadService;

    public AttractionController(GuestAttractionReadService guestAttractionReadService) {
        this.guestAttractionReadService = guestAttractionReadService;
    }

    @GetMapping
    @Operation(
            summary = "List attractions",
            description = "Lightweight operational catalog with thumbnail media for guest list views."
    )
    public List<AttractionResponse> list() {
        return guestAttractionReadService.listForGuest().stream()
                .map(detail -> AttractionResponse.from(detail.operational(), detail.experience()))
                .toList();
    }

    @GetMapping("/{attractionId}")
    @Operation(
            summary = "Get attraction detail",
            description = "Operational state composed with the stable guest experience profile."
    )
    public AttractionDetailResponse get(@PathVariable String attractionId) {
        return AttractionDetailResponse.from(
                guestAttractionReadService.getDetail(new AttractionId(attractionId))
        );
    }
}
