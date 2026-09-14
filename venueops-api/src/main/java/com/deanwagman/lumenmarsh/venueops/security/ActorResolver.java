package com.deanwagman.lumenmarsh.venueops.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class ActorResolver {

    public ActorIdentity requireActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth) || !jwtAuth.isAuthenticated()) {
            throw new AccessDeniedException("Authenticated JWT principal is required");
        }
        return fromJwt(jwtAuth.getToken(), jwtAuth.getAuthorities());
    }

    static ActorIdentity fromJwt(Jwt jwt, Collection<? extends GrantedAuthority> authorities) {
        boolean service = hasAuthority(authorities, VenueOpsScopes.SCOPE_WEATHER_WRITE)
                && !hasAuthority(authorities, VenueOpsScopes.ROLE_OPERATOR)
                && !hasAuthority(authorities, VenueOpsScopes.ROLE_SUPERVISOR);
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new AccessDeniedException("Token subject is required");
        }
        String issuer = jwt.getIssuer() != null ? jwt.getIssuer().toString() : "unknown";
        if (service) {
            String clientId = firstNonBlank(
                    jwt.getClaimAsString("client_id"),
                    jwt.getClaimAsString("cid"),
                    subject
            );
            return new ActorIdentity(clientId, ActorIdentity.WEATHER_SERVICE_DISPLAY, ActorType.SERVICE, issuer);
        }
        String display = firstNonBlank(
                jwt.getClaimAsString("name"),
                jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("email"),
                subject
        );
        return new ActorIdentity(subject, display, ActorType.HUMAN, issuer);
    }

    private static boolean hasAuthority(Collection<? extends GrantedAuthority> authorities, String value) {
        return authorities.stream().anyMatch(a -> value.equals(a.getAuthority()));
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "unknown";
    }
}
