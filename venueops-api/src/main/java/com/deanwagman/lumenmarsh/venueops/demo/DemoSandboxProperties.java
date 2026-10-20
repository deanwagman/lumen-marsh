package com.deanwagman.lumenmarsh.venueops.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("venueops.demo-sandbox")
public record DemoSandboxProperties(
        boolean enabled,
        String internalSecret,
        String environmentalUrl,
        String flowUrl,
        String reliabilityUrl,
        String guestUrl,
        String towerUrl,
        Duration leaseDuration,
        Duration inactivityTimeout,
        Duration hardMaximum,
        int mutationsPerMinute,
        boolean secureCookie
) {
    public DemoSandboxProperties {
        internalSecret = normalize(internalSecret);
        environmentalUrl = url(environmentalUrl, "http://environmental-monitor:8080");
        flowUrl = url(flowUrl, "http://park-flow-intelligence:8080");
        reliabilityUrl = url(reliabilityUrl, "http://reliability-intelligence:8080");
        guestUrl = url(guestUrl, "http://lumen-marsh-app:8080");
        towerUrl = url(towerUrl, "http://venueops-console:8080");
        leaseDuration = positive(leaseDuration, Duration.ofMinutes(10));
        inactivityTimeout = positive(inactivityTimeout, Duration.ofMinutes(3));
        hardMaximum = positive(hardMaximum, Duration.ofMinutes(15));
        mutationsPerMinute = mutationsPerMinute > 0 ? mutationsPerMinute : 30;
        if (enabled && internalSecret.length() < 32) {
            throw new IllegalStateException(
                    "venueops.demo-sandbox.internal-secret must contain at least 32 characters when enabled"
            );
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String url(String value, String fallback) {
        String normalized = normalize(value);
        return (normalized.isEmpty() ? fallback : normalized).replaceAll("/+$", "");
    }

    private static Duration positive(Duration value, Duration fallback) {
        return value == null || value.isZero() || value.isNegative() ? fallback : value;
    }
}
