package com.deanwagman.lumenmarsh.venueops.security;

import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentCommand;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentSeverity;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenancePriority;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrderCommand;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CommandAuthorization {

    public void requireIncidentCommand(IncidentCommand command, IncidentSeverity currentSeverity) {
        Authentication authentication = requireAuthentication();
        switch (command) {
            case PUBLISH_GUEST_ADVISORY, WITHDRAW_GUEST_ADVISORY -> {
                require(authentication, VenueOpsScopes.SCOPE_ADVISORIES_PUBLISH);
                require(authentication, VenueOpsScopes.ROLE_SUPERVISOR);
            }
            case RESOLVE -> {
                require(authentication, VenueOpsScopes.SCOPE_INCIDENTS_COMMAND);
                if (currentSeverity == IncidentSeverity.MAJOR || currentSeverity == IncidentSeverity.CRITICAL) {
                    require(authentication, VenueOpsScopes.ROLE_SUPERVISOR);
                }
            }
            default -> require(authentication, VenueOpsScopes.SCOPE_INCIDENTS_COMMAND);
        }
    }

    public void requireAttractionCommand() {
        require(requireAuthentication(), VenueOpsScopes.SCOPE_ATTRACTIONS_COMMAND);
    }

    public void requireWeatherReviewCommand() {
        require(requireAuthentication(), VenueOpsScopes.SCOPE_WEATHER_REVIEW);
    }

    public void requireFlowCommand() {
        require(requireAuthentication(), VenueOpsScopes.SCOPE_FLOW_COMMAND);
    }

    public void requireFlowPublish() {
        Authentication authentication = requireAuthentication();
        require(authentication, VenueOpsScopes.SCOPE_FLOW_PUBLISH);
        require(authentication, VenueOpsScopes.ROLE_SUPERVISOR);
    }

    public void requireMaintenanceCommand(MaintenanceWorkOrderCommand command, MaintenancePriority priority) {
        Authentication authentication = requireAuthentication();
        if (command.requiresSupervisor()) {
            require(authentication, VenueOpsScopes.SCOPE_MAINTENANCE_INSPECT);
            require(authentication, VenueOpsScopes.ROLE_SUPERVISOR);
            return;
        }
        require(authentication, VenueOpsScopes.SCOPE_MAINTENANCE_COMMAND);
        if (command == MaintenanceWorkOrderCommand.CANCEL && priority.requiresSupervisorToCancel()) {
            require(authentication, VenueOpsScopes.ROLE_SUPERVISOR);
        }
    }

    public boolean isSupervisor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(VenueOpsScopes.ROLE_SUPERVISOR::equals);
    }

    private static Authentication requireAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required");
        }
        return authentication;
    }

    private static void require(Authentication authentication, String authority) {
        boolean allowed = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
        if (!allowed) {
            throw new AccessDeniedException("Missing required authority: " + authority);
        }
    }
}
