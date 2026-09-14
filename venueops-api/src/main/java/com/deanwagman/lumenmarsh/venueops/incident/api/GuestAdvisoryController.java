package com.deanwagman.lumenmarsh.venueops.incident.api;

import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/advisories")
@Tag(name = "Guest advisories")
public class GuestAdvisoryController {

    private final IncidentService incidentService;

    public GuestAdvisoryController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping
    @Operation(
            summary = "List published guest advisories",
            description = "Returns unresolved incidents that currently have a published public title and message. Internal descriptions, operators, and activity are never included."
    )
    public List<GuestAdvisoryResponse> list() {
        return incidentService.listActiveGuestAdvisories().stream()
                .map(GuestAdvisoryResponse::from)
                .toList();
    }
}
