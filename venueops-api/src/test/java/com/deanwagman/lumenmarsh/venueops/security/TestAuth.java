package com.deanwagman.lumenmarsh.venueops.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.security.core.GrantedAuthority;

/**
 * Offline JWT fixtures for MockMvc — no live Cognito dependency.
 */
public final class TestAuth {

    public static final String ISSUER = "http://venueops.local/test";
    public static final String CONSOLE_CLIENT_ID = VenueOpsSecurityProperties.LOCAL_CONSOLE_CLIENT_ID;
    public static final String MONITOR_CLIENT_ID = VenueOpsSecurityProperties.LOCAL_MONITOR_CLIENT_ID;

    private TestAuth() {
    }

    public static RequestPostProcessor operator() {
        return jwt(
                "operator-sub-1",
                "Operator One",
                List.of("operators"),
                VenueOpsScopes.OPERATOR_READ,
                VenueOpsScopes.ATTRACTIONS_COMMAND,
                VenueOpsScopes.INCIDENTS_COMMAND,
                VenueOpsScopes.WEATHER_REVIEW
        );
    }

    public static RequestPostProcessor supervisor() {
        return jwt(
                "supervisor-sub-1",
                "Supervisor One",
                List.of("supervisors"),
                VenueOpsScopes.OPERATOR_READ,
                VenueOpsScopes.ATTRACTIONS_COMMAND,
                VenueOpsScopes.INCIDENTS_COMMAND,
                VenueOpsScopes.ADVISORIES_PUBLISH,
                VenueOpsScopes.WEATHER_REVIEW
        );
    }

    public static RequestPostProcessor weatherService() {
        return jwt(
                "environmental-monitor-client",
                ActorIdentity.WEATHER_SERVICE_DISPLAY,
                List.of(),
                VenueOpsScopes.WEATHER_WRITE
        );
    }

    public static RequestPostProcessor jwt(String subject, String name, List<String> groups, String... scopes) {
        String scope = String.join(" ", scopes);
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .issuer(ISSUER)
                .subject(subject)
                .issuedAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("token_use", "access")
                .claim("client_id", clientIdFor(groups, scopes))
                .claim("scope", scope)
                .claim("cognito:groups", groups)
                .claim("name", name)
                .build();
        List<GrantedAuthority> authorities = Stream.concat(
                        Arrays.stream(scopes).map(s -> new SimpleGrantedAuthority("SCOPE_" + s)),
                        groups.stream().flatMap(group -> {
                            if ("supervisors".equals(group)) {
                                return Stream.of(
                                        new SimpleGrantedAuthority(VenueOpsScopes.ROLE_SUPERVISOR),
                                        new SimpleGrantedAuthority(VenueOpsScopes.ROLE_OPERATOR)
                                );
                            }
                            if ("operators".equals(group)) {
                                return Stream.of(new SimpleGrantedAuthority(VenueOpsScopes.ROLE_OPERATOR));
                            }
                            return Stream.empty();
                        })
                )
                .map(GrantedAuthority.class::cast)
                .toList();
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt)
                .authorities(authorities.toArray(GrantedAuthority[]::new));
    }

    private static String clientIdFor(List<String> groups, String... scopes) {
        boolean weatherOnly = Arrays.asList(scopes).contains(VenueOpsScopes.WEATHER_WRITE)
                && groups.isEmpty();
        return weatherOnly ? MONITOR_CLIENT_ID : CONSOLE_CLIENT_ID;
    }
}
