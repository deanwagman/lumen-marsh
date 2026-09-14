package com.deanwagman.lumenmarsh.venueops.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Mints signed local JWTs for HttpClient / SSE integration tests.
 */
@Component
public class LocalJwtTokenFactory {

    private final JwtEncoder encoder;

    @Autowired
    public LocalJwtTokenFactory(JwtEncoder encoder) {
        this.encoder = encoder;
    }

    public String operatorToken() {
        return token(
                "operator-sub-1",
                "Operator One",
                List.of("operators"),
                VenueOpsScopes.OPERATOR_READ + " " + VenueOpsScopes.ATTRACTIONS_COMMAND + " "
                        + VenueOpsScopes.INCIDENTS_COMMAND + " " + VenueOpsScopes.WEATHER_REVIEW
        );
    }

    public String supervisorToken() {
        return token(
                "supervisor-sub-1",
                "Supervisor One",
                List.of("supervisors"),
                VenueOpsScopes.OPERATOR_READ + " " + VenueOpsScopes.ATTRACTIONS_COMMAND + " "
                        + VenueOpsScopes.INCIDENTS_COMMAND + " " + VenueOpsScopes.ADVISORIES_PUBLISH + " "
                        + VenueOpsScopes.WEATHER_REVIEW
        );
    }

    public String weatherServiceToken() {
        return token(
                "environmental-monitor-client",
                ActorIdentity.WEATHER_SERVICE_DISPLAY,
                List.of(),
                VenueOpsScopes.WEATHER_WRITE
        );
    }

    public String token(String subject, String name, List<String> groups, String scope) {
        boolean weatherOnly = VenueOpsScopes.WEATHER_WRITE.equals(scope);
        return token(
                subject,
                name,
                groups,
                scope,
                weatherOnly ? TestAuth.MONITOR_CLIENT_ID : TestAuth.CONSOLE_CLIENT_ID,
                "access"
        );
    }

    public String token(
            String subject,
            String name,
            List<String> groups,
            String scope,
            String clientId,
            String tokenUse
    ) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(TestAuth.ISSUER)
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("scope", scope)
                .claim("cognito:groups", groups)
                .claim("name", name);
        if (tokenUse != null) {
            claims.claim("token_use", tokenUse);
        }
        if (clientId != null) {
            claims.claim("client_id", clientId);
        }
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims.build()))
                .getTokenValue();
    }
}
