package com.deanwagman.lumenmarsh.venueops.security;

import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentCommand;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentSeverity;
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
