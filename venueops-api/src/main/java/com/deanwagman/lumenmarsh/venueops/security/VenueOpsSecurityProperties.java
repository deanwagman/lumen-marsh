package com.deanwagman.lumenmarsh.venueops.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@ConfigurationProperties(prefix = "venueops.security")
public record VenueOpsSecurityProperties(
        Mode mode,
        String issuerUri,
        String allowedClientIds,
        boolean exposeApiDocs,
        String allowedOrigins,
        Integer weatherIngestRateLimitPerMinute
) {
    public static final String LOCAL_CONSOLE_CLIENT_ID = "venueops-console-local";
    public static final String LOCAL_MONITOR_CLIENT_ID = "environmental-monitor-local";

    public enum Mode {
        /**
         * Validate Cognito (or other OIDC) access tokens via issuer metadata.
         */
        OIDC,
        /**
         * Validate locally signed JWTs for offline tests and local Compose without AWS.
         */
        LOCAL_JWT
    }

    public VenueOpsSecurityProperties {
        if (mode == null) {
            throw new IllegalStateException("venueops.security.mode is required (OIDC or LOCAL_JWT)");
        }
        if (issuerUri == null || issuerUri.isBlank()) {
            throw new IllegalStateException("venueops.security.issuer-uri is required");
        }
        if (allowedClientIds == null || allowedClientIds.isBlank()) {
            allowedClientIds = LOCAL_CONSOLE_CLIENT_ID + "," + LOCAL_MONITOR_CLIENT_ID;
        }
        if (parseCsv(allowedClientIds).isEmpty()) {
            throw new IllegalStateException("venueops.security.allowed-client-ids must list at least one client_id");
        }
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            allowedOrigins = "http://localhost:*,http://127.0.0.1:*";
        }
        if (weatherIngestRateLimitPerMinute == null) {
            weatherIngestRateLimitPerMinute = 120;
        }
        if (weatherIngestRateLimitPerMinute < 0) {
            throw new IllegalStateException("venueops.security.weather-ingest-rate-limit-per-minute must be >= 0");
        }
    }

    public Set<String> allowedClientIdSet() {
        return parseCsv(allowedClientIds);
    }

    public String[] allowedOriginPatterns() {
        return parseCsv(allowedOrigins).toArray(String[]::new);
    }

    private static Set<String> parseCsv(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
