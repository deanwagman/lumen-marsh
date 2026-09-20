package com.deanwagman.lumenmarsh.venueops.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * LOCAL_JWT-only opaque bearer bridge for Compose/console local mode.
 * Recognized tokens are converted before the JWT bearer filter runs.
 */
public class LocalDevBearerAuthenticationFilter extends OncePerRequestFilter {

    /** LOCAL_JWT console bridge: operators + supervisors (matches Control Tower local mode). */
    public static final String CONSOLE_TOKEN = "local-development-token";
    /** LOCAL_JWT operator-only token: no {@code venueops/advisories.publish}. */
    public static final String OPERATOR_LIMITED_TOKEN = "local-operator-token";
    public static final String WEATHER_TOKEN = "local-weather-token";
    public static final String RELIABILITY_TOKEN = "local-reliability-token";
    public static final String FLOW_TOKEN = "local-flow-token";

    private final VenueOpsSecurityProperties properties;

    public LocalDevBearerAuthenticationFilter(VenueOpsSecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring("Bearer ".length()).trim();
            JwtAuthenticationToken authentication = authenticateOpaque(token);
            if (authentication != null) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
                filterChain.doFilter(stripAuthorization(request), response);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private static HttpServletRequest stripAuthorization(HttpServletRequest request) {
        return new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
                    return null;
                }
                return super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
                    return Collections.emptyEnumeration();
                }
                return super.getHeaders(name);
            }
        };
    }

    private JwtAuthenticationToken authenticateOpaque(String token) {
        if (CONSOLE_TOKEN.equals(token)) {
            Jwt jwt = baseJwt(
                    "local-operator",
                    "Local Operator",
                    List.of("supervisors"),
                    VenueOpsScopes.OPERATOR_READ + " " + VenueOpsScopes.ATTRACTIONS_COMMAND + " "
                            + VenueOpsScopes.INCIDENTS_COMMAND + " " + VenueOpsScopes.ADVISORIES_PUBLISH + " "
                            + VenueOpsScopes.WEATHER_REVIEW + " " + VenueOpsScopes.MAINTENANCE_READ + " "
                            + VenueOpsScopes.MAINTENANCE_COMMAND + " " + VenueOpsScopes.MAINTENANCE_INSPECT + " "
                            + VenueOpsScopes.FLOW_READ + " " + VenueOpsScopes.FLOW_COMMAND + " "
                            + VenueOpsScopes.FLOW_PUBLISH,
                    VenueOpsSecurityProperties.LOCAL_CONSOLE_CLIENT_ID
            );
            return new JwtAuthenticationToken(jwt, List.of(
                    new SimpleGrantedAuthority(VenueOpsScopes.SCOPE_OPERATOR_READ),
                    new SimpleGrantedAuthority(VenueOpsScopes.SCOPE_ATTRACTIONS_COMMAND),
                    new SimpleGrantedAuthority(VenueOpsScopes.SCOPE_INCIDENTS_COMMAND),
                    new SimpleGrantedAuthority(VenueOpsScopes.SCOPE_ADVISORIES_PUBLISH),
                    new SimpleGrantedAuthority(VenueOpsScopes.SCOPE_WEATHER_REVIEW),
                    new SimpleGrantedAuthority(VenueOpsScopes.SCOPE_MAINTENANCE_READ),
                    new SimpleGrantedAuthority(VenueOpsScopes.SCOPE_MAINTENANCE_COMMAND),
                    new SimpleGrantedAuthority(VenueOpsScopes.SCOPE_MAINTENANCE_INSPECT),
                    new SimpleGrantedAuthority(VenueOpsScopes.SCOPE_FLOW_READ),
                    new SimpleGrantedAuthority(VenueOpsScopes.SCOPE_FLOW_COMMAND),
                    new SimpleGrantedAuthority(VenueOpsScopes.SCOPE_FLOW_PUBLISH),
                    new SimpleGrantedAuthority(VenueOpsScopes.ROLE_OPERATOR),
                    new SimpleGrantedAuthority(VenueOpsScopes.ROLE_SUPERVISOR)
            ), jwt.getSubject());
        }
        if (OPERATOR_LIMITED_TOKEN.equals(token)) {
            Jwt jwt = baseJwt(
                    "local-limited-operator",
                    "Local Limited Operator",
                    List.of("operators"),
                    VenueOpsScopes.OPERATOR_READ + " " + VenueOpsScopes.ATTRACTIONS_COMMAND + " "
                            + VenueOpsScopes.INCIDENTS_COMMAND + " " + VenueOpsScopes.WEATHER_REVIEW + " "
                            + VenueOpsScopes.MAINTENANCE_READ + " " + VenueOpsScopes.MAINTENANCE_COMMAND + " "
                            + VenueOpsScopes.FLOW_READ + " " + VenueOpsScopes.FLOW_COMMAND,
                    VenueOpsSecurityProperties.LOCAL_CONSOLE_CLIENT_ID
            );
            return new JwtAuthenticationToken(jwt, List.of(
                    new SimpleGrantedAuthority(VenueOpsScopes.SCOPE_OPERATOR_READ),
                    new SimpleGrantedAuthority(VenueOpsScopes.SCOPE_ATTRACTIONS_COMMAND),
                    new SimpleGrantedAuthority(VenueOpsScopes.SCOPE_INCIDENTS_COMMAND),
                    new SimpleGrantedAuthority(VenueOpsScopes.SCOPE_WEATHER_REVIEW),
                    new SimpleGrantedAuthority(VenueOpsScopes.SCOPE_MAINTENANCE_READ),
                    new SimpleGrantedAuthority(VenueOpsScopes.SCOPE_MAINTENANCE_COMMAND),
                    new SimpleGrantedAuthority(VenueOpsScopes.SCOPE_FLOW_READ),
                    new SimpleGrantedAuthority(VenueOpsScopes.SCOPE_FLOW_COMMAND),
                    new SimpleGrantedAuthority(VenueOpsScopes.ROLE_OPERATOR)
            ), jwt.getSubject());
        }
        if (WEATHER_TOKEN.equals(token)) {
            Jwt jwt = baseJwt(
                    "environmental-monitor-local",
                    ActorIdentity.WEATHER_SERVICE_DISPLAY,
                    List.of(),
                    VenueOpsScopes.WEATHER_WRITE,
                    VenueOpsSecurityProperties.LOCAL_MONITOR_CLIENT_ID
            );
            return new JwtAuthenticationToken(jwt, List.of(
                    new SimpleGrantedAuthority(VenueOpsScopes.SCOPE_WEATHER_WRITE)
            ), jwt.getSubject());
        }
        if (RELIABILITY_TOKEN.equals(token)) {
            Jwt jwt = baseJwt(
                    "reliability-integration-local",
                    ActorIdentity.RELIABILITY_SERVICE_DISPLAY,
                    List.of(),
                    VenueOpsScopes.RELIABILITY_WRITE,
                    VenueOpsSecurityProperties.LOCAL_RELIABILITY_CLIENT_ID
            );
            return new JwtAuthenticationToken(jwt, List.of(
                    new SimpleGrantedAuthority(VenueOpsScopes.SCOPE_RELIABILITY_WRITE)
            ), jwt.getSubject());
        }
        if (FLOW_TOKEN.equals(token)) {
            Jwt jwt = baseJwt(
                    "park-flow-intelligence-local",
                    ActorIdentity.FLOW_SERVICE_DISPLAY,
                    List.of(),
                    VenueOpsScopes.FLOW_INGEST_WRITE,
                    VenueOpsSecurityProperties.LOCAL_FLOW_CLIENT_ID
            );
            return new JwtAuthenticationToken(jwt, List.of(
                    new SimpleGrantedAuthority(VenueOpsScopes.SCOPE_FLOW_INGEST_WRITE)
            ), jwt.getSubject());
        }
        return null;
    }

    private Jwt baseJwt(String subject, String name, List<String> groups, String scope, String clientId) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("local-opaque")
                .header("alg", "none")
                .issuer(properties.issuerUri())
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("token_use", "access")
                .claim("client_id", clientId)
                .claim("scope", scope)
                .claim("cognito:groups", groups)
                .claim("name", name)
                .build();
    }
}
