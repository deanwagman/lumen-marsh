package com.deanwagman.lumenmarsh.venueops.demo;

import com.deanwagman.lumenmarsh.venueops.security.VenueOpsScopes;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Authenticates only requests forwarded by the private demo gateway. The browser
 * receives no reusable bearer token; mutation authority remains the lease cookie.
 */
public final class DemoGatewayAuthenticationFilter extends OncePerRequestFilter {
    public static final String SECRET_HEADER = "X-Demo-Gateway-Secret";
    private final DemoSandboxProperties properties;

    DemoGatewayAuthenticationFilter(DemoSandboxProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/operator/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String supplied = request.getHeader(SECRET_HEADER);
        if (supplied != null && MessageDigest.isEqual(
                supplied.getBytes(StandardCharsets.UTF_8),
                properties.internalSecret().getBytes(StandardCharsets.UTF_8)
        )) {
            Instant now = Instant.now();
            Jwt jwt = Jwt.withTokenValue("demo-gateway")
                    .header("alg", "none")
                    .issuer("lumen-marsh-demo")
                    .subject("demo-visitor")
                    .issuedAt(now)
                    .expiresAt(now.plusSeconds(60))
                    .claim("token_use", "access")
                    .claim("client_id", "lumen-marsh-demo-gateway")
                    .claim("scope", "demo")
                    .claim("cognito:groups", List.of("supervisors"))
                    .claim("name", "Demo Visitor")
                    .build();
            List<SimpleGrantedAuthority> authorities = List.of(
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
            );
            SecurityContextHolder.getContext().setAuthentication(
                    new JwtAuthenticationToken(jwt, authorities, jwt.getSubject()));
            filterChain.doFilter(stripSensitiveHeaders(request), response);
            return;
        }
        filterChain.doFilter(stripGatewayHeader(request), response);
    }

    private static HttpServletRequest stripSensitiveHeaders(HttpServletRequest request) {
        return new HeaderStrippingRequest(request, true);
    }

    private static HttpServletRequest stripGatewayHeader(HttpServletRequest request) {
        return new HeaderStrippingRequest(request, false);
    }

    private static final class HeaderStrippingRequest extends HttpServletRequestWrapper {
        private final boolean stripAuthorization;

        private HeaderStrippingRequest(HttpServletRequest request, boolean stripAuthorization) {
            super(request);
            this.stripAuthorization = stripAuthorization;
        }

        @Override
        public String getHeader(String name) {
            if (SECRET_HEADER.equalsIgnoreCase(name)
                    || (stripAuthorization && HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name))) {
                return null;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (SECRET_HEADER.equalsIgnoreCase(name)
                    || (stripAuthorization && HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name))) {
                return Collections.emptyEnumeration();
            }
            return super.getHeaders(name);
        }
    }
}
