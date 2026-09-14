package com.deanwagman.lumenmarsh.venueops.security;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;

import java.util.Set;

/**
 * Cognito access tokens identify the caller with {@code client_id} and {@code token_use}.
 * They do not reliably carry a resource-server {@code aud} claim unless resource binding is used.
 */
final class VenueOpsJwtValidators {

    private VenueOpsJwtValidators() {
    }

    static OAuth2TokenValidator<Jwt> accessToken(String issuerUri, Set<String> allowedClientIds) {
        JwtClaimValidator<String> tokenUse = new JwtClaimValidator<>(
                "token_use",
                "access"::equals
        );
        JwtClaimValidator<String> clientId = new JwtClaimValidator<>(
                "client_id",
                value -> value != null && allowedClientIds.contains(value)
        );
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuerUri),
                tokenUse,
                clientId
        );
    }
}
