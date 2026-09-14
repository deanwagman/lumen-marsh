package com.deanwagman.lumenmarsh.venueops.dashboard.api;

import com.deanwagman.lumenmarsh.venueops.dashboard.application.OperatorDashboardService;
import com.deanwagman.lumenmarsh.venueops.security.VenueOpsScopes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/operator/dashboard")
@Tag(name = "Operator dashboard")
public class OperatorDashboardController {

    private final OperatorDashboardService dashboardService;

    public OperatorDashboardController(OperatorDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_OPERATOR_READ + "')")
    @Operation(
            summary = "Get the operations dashboard snapshot",
            description = "Read-only park-wide snapshot. Does not issue commands or write activity."
    )
    public OperatorDashboardResponse get() {
        return dashboardService.snapshot();
    }
}
